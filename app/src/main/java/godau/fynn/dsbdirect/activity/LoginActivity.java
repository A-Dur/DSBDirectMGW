/*
 * DSBDirect
 * Copyright (C) 2019 Fynn Godau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with heinekingmedia GmbH, the
 * developer of the DSB platform.
 */

package godau.fynn.dsbdirect.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import godau.fynn.dsbdirect.model.Login;
import godau.fynn.dsbdirect.download.NewsQuery;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.download.DownloadManager;
import godau.fynn.dsbdirect.persistence.FileManager;
import godau.fynn.dsbdirect.persistence.LoginManager;
import godau.fynn.dsbdirect.download.exception.LoginFailureException;
import godau.fynn.dsbdirect.download.exception.NoContentException;
import godau.fynn.dsbdirect.model.Table;
import godau.fynn.dsbdirect.view.FilterConfigDialog;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final TextInputEditText idEditText = findViewById(R.id.id);
        final TextInputEditText passEditText = findViewById(R.id.pass);
        final Button login = findViewById(R.id.login);

        final Utility u = new Utility(LoginActivity.this);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = idEditText.getText().toString();
                String pass = passEditText.getText().toString();
                final Login login = new Login(id, pass);

                // Indicate that something is happening
                indicateProgress(true);

                final DownloadManager downloadManager = DownloadManager.getDownloadManager(LoginActivity.this);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            Table[] tables = downloadManager.downloadTables(login);

                            // If no exception occurred, the credentials are correct
                            LoginManager loginManager = new LoginManager(LoginActivity.this);

                            // Only prompt user for their preferences when they log in with their first login
                            boolean promptPreferences = loginManager.getLoginCount() <= 0;

                            loginManager.addLogin(login);

                            setResult(RESULT_OK);

                            if (promptPreferences) {
                                potentiallyPromptForFilter(downloadManager, u, login.getId(), tables);
                            } else {
                                // Skip questions
                                finish();
                            }

                        } catch (LoginFailureException e) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Credentials invalid
                                    new AlertDialog.Builder(LoginActivity.this, R.style.LoginTheme_AlertDialog)
                                            .setTitle(R.string.credentials_popup_title)
                                            .setMessage(R.string.credentials_popup_message)
                                            .setPositiveButton(R.string.ok, null)
                                            .setNeutralButton(R.string.news_fix, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    new Thread(new NewsQuery(LoginActivity.this, downloadManager))
                                                            .start();
                                                }
                                            })
                                            .setNegativeButton(R.string.credentials_popup_open_preferences, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    startActivity(new Intent(LoginActivity.this, SettingsActivity.class));
                                                }
                                            })
                                            .show();

                                    indicateProgress(false);
                                }
                            });
                            e.printStackTrace();
                        } catch (IOException e) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Error
                                    new AlertDialog.Builder(LoginActivity.this, R.style.LoginTheme_AlertDialog)
                                            .setTitle(R.string.network_generic_error)
                                            .setMessage(R.string.network_generic_error_credentials)
                                            .setPositiveButton(R.string.ok, null)
                                            .show();
                                    indicateProgress(false);
                                }
                            });

                            e.printStackTrace();
                        }
                    }
                }).start();


            }
        });


        // Handel enter presses
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) passEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    login.callOnClick();

                    return true;
                }

                return false;
            }
        });
    }

    private void potentiallyPromptForFilter(final DownloadManager downloadManager, final Utility u, final String id, Table[] tables) {

        try {

            if (tables[0].isHtml()) {
                FileManager fileManager = new FileManager(LoginActivity.this);

                // Download the first table
                String html = fileManager.getHtmlTable(tables[0], downloadManager);

                // If a reader can be gotten, parsing is probably possible
                if (u.getReader(html, id) != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            indicateProgress(false);
                            new FilterConfigDialog(LoginActivity.this, R.style.LoginTheme_AlertDialog,
                                    new Handler(new Handler.Callback() {
                                        @Override
                                        public boolean handleMessage(Message msg) {
                                            // Ensure parse and filter are on
                                            u.getSharedPreferences().edit()
                                                    .putBoolean("parse", true)
                                                    .putBoolean("filter", true)
                                                    .apply();

                                            promptForNotificationsOnUiThreadAndFinish();

                                            return false;
                                        }
                                    })).show();
                        }
                    });
                } else {
                    // Parsing seems to be impossible
                    u.getSharedPreferences().edit()
                            .putBoolean("parse", false)
                            .apply();
                    promptForNotificationsOnUiThreadAndFinish();
                }

            } else {
                promptForNotificationsOnUiThreadAndFinish();
            }


        } catch (NoContentException e) {

            promptForNotificationsOnUiThreadAndFinish();

            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void promptForNotificationsOnUiThreadAndFinish() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                indicateProgress(false);
                new AlertDialog.Builder(LoginActivity.this, R.style.LoginTheme_AlertDialog)
                        .setTitle(R.string.notifications_popup_title)
                        .setMessage(R.string.notifications_popup_message)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Utility(LoginActivity.this).getSharedPreferences()
                                        .edit()
                                        .putBoolean("poll", true)
                                        .apply();
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Utility(LoginActivity.this).getSharedPreferences()
                                        .edit()
                                        .putBoolean("poll", false)
                                        .apply();
                                finish();
                            }
                        })
                        .show();
            }
        });

    }

    private void indicateProgress(boolean yes) {
        if (yes) {
            findViewById(R.id.login).setEnabled(false);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.login).setEnabled(true);
            findViewById(R.id.progressBar).setVisibility(View.GONE);
        }
    }

    @Override
    public void finish() {
        new Utility(LoginActivity.this)
                .getSharedPreferences()
                .edit()
                .putBoolean("login", true)
                .apply();

        super.finish();
    }
}
