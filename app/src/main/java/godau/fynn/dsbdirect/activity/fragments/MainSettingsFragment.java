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

package godau.fynn.dsbdirect.activity.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import godau.fynn.dsbdirect.activity.SettingsActivity;
import godau.fynn.dsbdirect.table.reader.Reader;
import godau.fynn.dsbdirect.table.reader.Readers;
import godau.fynn.dsbdirect.util.AboutLibrariesConfig;
import godau.fynn.dsbdirect.BuildConfig;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.activity.MainActivity;
import godau.fynn.dsbdirect.persistence.FileManager;
import godau.fynn.dsbdirect.persistence.LoginManager;
import godau.fynn.dsbdirect.download.DownloadManager;
import godau.fynn.dsbdirect.model.Table;
import godau.fynn.dsbdirect.view.FilterConfigDialog;
import humanize.Humanize;

import java.io.IOException;
import java.util.*;

import static godau.fynn.dsbdirect.activity.SettingsActivity.EXTRA_CONTAINS_HTML;
import static godau.fynn.dsbdirect.activity.SettingsActivity.EXTRA_HTML_ONLY;

// Thanks, https://stackoverflow.com/a/12806877

public class MainSettingsFragment extends SettingsActivity.PreferenceFragment {

    private static final int REQUEST_CODE_STYLING = 1;

    @Override
    public void setPreferences() {

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Behave if pictures are involved
        final boolean containsHtml = getActivity().getIntent().getBooleanExtra(EXTRA_CONTAINS_HTML, true);
        final boolean onlyHtml = getActivity().getIntent().getBooleanExtra(EXTRA_HTML_ONLY, true);

        if (!containsHtml) {
            Preference parse = findPreference("parse");
            parse.setEnabled(false);
            ((CheckBoxPreference) parse).setChecked(false);
            parse.setSummary(R.string.settings_view_parse_not_html);

            // Gray out display empty setting as user will definitely see empty notifications
            Preference displayEmpty = findPreference("displayEmpty");
            displayEmpty.setEnabled(false);
            ((CheckBoxPreference) displayEmpty).setChecked(true);
            // Explain why it is not possible
            displayEmpty.setSummary(R.string.settings_view_parse_not_html);
        } else {
            if (sharedPreferences.getBoolean("parse", true)) {
                // Explain why it makes sense to download automatically
                findPreference("autoDownload").setSummary(R.string.settings_polling_download_recommended);

            }

            // Gray out merge preference because merging is not possible when images are involved
            if (!onlyHtml) {
                Preference merge = findPreference("merge");
                merge.setEnabled(false);
                ((CheckBoxPreference) merge).setChecked(false);
                merge.setSummary(R.string.settings_view_parse_not_exclusively_html);
            }
        }

        // Show all Parsers when attempting to switch Parser
        ListPreference parser = findPreference("parser");
        Reader[] readers = Readers.getReaders();
        List<String> readerEntries = new ArrayList<>();
        List<String> readerLabels = new ArrayList<>();
        readerEntries.add("automatic");
        readerLabels.add(getString(R.string.reader_automatic));
        for (Reader reader : readers) {
            readerEntries.add(reader.getId());
            readerLabels.add(reader.getName());
        }
        parser.setEntryValues(readerEntries.toArray(new String[readers.length + 1]));
        parser.setEntries(readerLabels.toArray(new String[readers.length + 1]));

        // Open Style and Colors settings when user taps Style and Colors settings
        findPreference("styling").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openFragment(new StyleSettingsFragment());
                return true;
            }
        });

        // Set temporary filter setting according to Enable filter setting whenever updated
        findPreference("filter").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                MainActivity.filterEnabled = (Boolean) newValue;
                return true;
            }
        });

        // Display set filters Set Filter button
        final Preference setFilter = findPreference("set_filter");
        final Handler updateSetFilterSummary = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Set courseSet = sharedPreferences.getStringSet("courses", new HashSet<String>());
                String[] concat = new String[courseSet.size() + 2];
                courseSet.toArray(concat);
                concat[courseSet.size()] = sharedPreferences.getString("number", "") + sharedPreferences.getString("letter", "");
                concat[courseSet.size() + 1] = sharedPreferences.getString("name", null);
                setFilter.setSummary(Utility.smartConcatenate(concat, ", "));
                return false;
            }
        });
        updateSetFilterSummary.sendEmptyMessage(0);

        // Let Set Filter button open the Set Filter popup
        setFilter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new FilterConfigDialog(getActivity(), null, updateSetFilterSummary).show();

                return false;
            }
        });

        // Disable polling if API level is too low
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Preference poll = findPreference("poll");
            ((CheckBoxPreference) poll).setChecked(false);
            poll.setEnabled(false);
            poll.setSummary(R.string.settings_view_polling_api_too_low);
            findPreference("polling_description").setEnabled(false);
        }

        // Manage logins by tappig Manage logins
        findPreference("login_manage").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openFragment(new LoginSettingsFragment());
                return true;
            }
        });

        // Gray out Delete all files now when no files are available
        final Preference clearCache = findPreference("clear_cache_now");
        final FileManager fileManager = new FileManager(getActivity());
        final int fileCount = fileManager.countFiles();

        if (fileCount <= 0) {
            clearCache.setEnabled(false);
        }

        // Delete all files when Delete all files now is pressed
        clearCache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Prompt for confirmation
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.clear_cache_popup_title)
                        .setMessage(getString(R.string.clear_cache_popup_message,
                                Humanize.pluralize(
                                        getString(R.string.one_file), "{0} " + getString(R.string.files),
                                        "{0} " + getString(R.string.files), fileCount
                                ),
                                Humanize.binaryPrefix(fileManager.occupiedStorage())))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Delete all files
                                fileManager.deleteAllFiles();

                                // There are no more files left (hopefully)
                                clearCache.setEnabled(false);
                            }
                        })
                        .show();

                return true;
            }
        });

        // Schedule or cancel notification when Enable notifications is toggled
        findPreference("poll").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Utility will check whether polling is enabled and act accordingly
                    u.schedulePolling();
                }

                return true;
            }
        });

        // Show version number in About description preference summary
        findPreference("settings_about_description").setSummary(
                getString(R.string.credits) + "\n" +
                        getString(R.string.settings_about_description_summary, BuildConfig.VERSION_NAME)
        );

        // Let Write the developer an email message button enable the user to Write the developer an email message
        findPreference("settings_about_email").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                emailTheDev(getActivity());
                return false;
            }
        });

        // Request a parser to be developed button should allow users to Request a parser to be developed
        findPreference("settings_about_request_parser").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.request_parser_popup_title)
                        .setMessage(R.string.request_parser_popup_message)
                        .setNegativeButton(R.string.dont_request, null)
                        .setPositiveButton(R.string.transfer_now, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                final ProgressDialog progressDialog = ProgressDialog.show(
                                        getActivity(), null,
                                        getActivity().getString(R.string.request_parser_uploading_message),
                                        true
                                );

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {

                                            DownloadManager downloadManager = DownloadManager.getDownloadManager(getActivity());
                                            LoginManager loginManager = new LoginManager(sharedPreferences);

                                            // Download timetable list
                                            Table[] tables = downloadManager.downloadTables(loginManager.getActiveLogin());

                                            final boolean success = downloadManager.uploadParserRequest(tables[0].getUri());

                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    progressDialog.dismiss();

                                                    new AlertDialog.Builder(getActivity())
                                                            .setTitle(success ?
                                                                    R.string.request_parser_uploading_successful_title :
                                                                    R.string.request_parser_uploading_failed_title
                                                            )
                                                            .setMessage(success ?
                                                                    R.string.request_parser_uploading_successful_message :
                                                                    R.string.request_parser_uploading_failed_message
                                                            )
                                                            .setPositiveButton(R.string.ok, null)
                                                            .show();
                                                }
                                            });

                                        } catch (IOException e) {
                                            e.printStackTrace();

                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressDialog.dismiss();

                                                    new AlertDialog.Builder(getActivity())
                                                            .setTitle(R.string.network_generic_error)
                                                            .setMessage(R.string.network_generic_error_request)
                                                            .setPositiveButton(R.string.ok, null)
                                                            .show();
                                                }
                                            });
                                        }
                                    }
                                }).start();


                            }
                        })
                        .show();

                return true;
            }
        });

        // Manage shortcodes when choosing to Manage shortcodes
        findPreference("shortcode_manage").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openFragment(new ShortcodeSettingsFragment());
                return false;
            }
        });

        // Check out repository when Check out repository is selected
        findPreference("settings_about_repository").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent checkOutRepo = new Intent(Intent.ACTION_VIEW);
                checkOutRepo.setData(Uri.parse(getString(R.string.uri_repository)));
                startActivity(checkOutRepo);
                return true;
            }
        });

        findPreference("settings_about_mgw_repository").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent checkOutRepo = new Intent(Intent.ACTION_VIEW);
                checkOutRepo.setData(Uri.parse(getString(R.string.uri_mgw_repository)));
                startActivity(checkOutRepo);
                return true;
            }
        });

        // Lead user to page where they can File an issue once they click File an issue
        findPreference("settings_about_issue").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent fileAnIssue = new Intent(Intent.ACTION_VIEW);
                fileAnIssue.setData(Uri.parse(getString(R.string.uri_issue_tracker)));
                startActivity(fileAnIssue);
                return true;
            }
        });

        // Show information About libraries upon touching About libraries
        findPreference("settings_about_libraries").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                startActivity(AboutLibrariesConfig.getIntent(getActivity()));

                return true;
            }
        });

        findPreference("settings_about_show_changes").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Resources res = getResources();
                        String[] mgwChangesArray = res.getStringArray(R.array.show_mgw_changes);
                        String mgwChanges = String.join("\n\n", mgwChangesArray);
                        new AlertDialog.Builder(getActivity())
                                .setMessage(mgwChanges)
                                .show();
                    }
                });

                return true;
            }
        });
    }



    public static void emailTheDev(Context context) {
        Intent emailTheDev = new Intent(Intent.ACTION_VIEW);
        emailTheDev.setData(Uri.parse(context.getString(R.string.email_uri, context.getString(R.string.email_body))));

        context.startActivity(emailTheDev);
    }
}

