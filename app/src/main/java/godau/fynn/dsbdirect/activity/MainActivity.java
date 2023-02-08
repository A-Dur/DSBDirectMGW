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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;
import com.ortiz.touchview.TouchImageView;
import com.wefika.horizontalpicker.HorizontalPicker;
import godau.fynn.dsbdirect.BuildConfig;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.activity.fragments.MainSettingsFragment;
import godau.fynn.dsbdirect.download.DownloadManager;
import godau.fynn.dsbdirect.download.NewsQuery;
import godau.fynn.dsbdirect.download.exception.LoginFailureException;
import godau.fynn.dsbdirect.download.exception.NoContentException;
import godau.fynn.dsbdirect.download.exception.UnexpectedResponseException;
import godau.fynn.dsbdirect.model.Login;
import godau.fynn.dsbdirect.model.Table;
import godau.fynn.dsbdirect.model.entry.Entry;
import godau.fynn.dsbdirect.model.entry.ErrorEntry;
import godau.fynn.dsbdirect.model.noticeboard.NoticeBoardItem;
import godau.fynn.dsbdirect.persistence.FileManager;
import godau.fynn.dsbdirect.persistence.LoginManager;
import godau.fynn.dsbdirect.table.reader.ReaderRunnable;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.view.adapter.Adapter;
import humanize.Humanize;
import humanize.time.TimeMillis;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOGIN = 1;

    private Table mCurrentTable;

    private Login login;

    private Table[] mTables;
    private Date mTimetabledate;

    private List<NoticeBoardItem> mNoticeBoardItemList;

    private boolean mTimetabledateDisplayed = false;

    private static Utility u;
    private FileManager mFileManager;
    private DownloadManager mDownloadManager;
    private LoginManager mLoginManager;

    private final Queue<Runnable> mReaderTasks = new LinkedList<>();

    private TextView mTextView;

    private WebView mWebView;

    private Adapter mAdapter;
    private final BlockingQueue<Runnable> onMenuCreated = new LinkedBlockingQueue<>();

    private boolean mParse = true;
    private boolean mMerge = true;

    private Menu mMenu;
    private SwipeRefreshLayout mSwipeLayout;
    private Thread menuOperationsThread = null;

    public static boolean filterEnabled;
    public static boolean initialized = false; //only set filter_enabled once
    private static int offlineFilterPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        u = new Utility(MainActivity.this);
        u.stylize();

        long drawTimeStart = System.currentTimeMillis();

        setContentView(R.layout.activity_main);

        // Style horizontal picker
        findViewById(R.id.page).setBackgroundColor(u.getColorPrimary());

        long drawTimeEnd = System.currentTimeMillis();
        Log.d("DRAW", "rendering layout took " + (drawTimeEnd - drawTimeStart) + " milliseconds");

        final SharedPreferences sharedPreferences = u.getSharedPreferences();

        mParse = sharedPreferences.getBoolean("parse", true);
        mMerge = sharedPreferences.getBoolean("merge", true);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(u.getColorPrimary());
        setSupportActionBar(toolbar);

        setOverflowButtonColor(this, getResources().getColor(R.color.white));

        mTextView = findViewById(R.id.text);

        // Get managers
        mFileManager = new FileManager(MainActivity.this);
        mDownloadManager = DownloadManager.getDownloadManager(this);
        mLoginManager = new LoginManager(MainActivity.this);

        int previousVersion = sharedPreferences.getInt("version", BuildConfig.VERSION_CODE);

        // Migrate 1.8.1 (version code 13) users to avoid login screen
        if (previousVersion <= 13) {
            sharedPreferences.edit().putBoolean("login", true).apply();
        }

        // Delete useless auth token from versions up to 2.4.1 (version code 22)
        if (previousVersion <= 22) {
            sharedPreferences.edit().remove("token").apply();
        }

        // Migrate login from up to version 2.5.5 (version code 29)
        if (previousVersion <= 29) {
            mLoginManager.addLogin(new Login(
                    sharedPreferences.getString("id", ""),
                    sharedPreferences.getString("pass", "")
            ));
            sharedPreferences.edit().remove("pass").apply();
        }

        // Migrate shortcodes stored in a StringSet preference from up to version 2.6 (version code 32)
        if (previousVersion <= 32) {
            Set<String> shortcodeSet = sharedPreferences.getStringSet("shortcodes", new LinkedHashSet<String>());
            String[] shortcodes = shortcodeSet.toArray(new String[shortcodeSet.size()]);
            Arrays.sort(shortcodes);
            StringBuilder shortcodesRaw = new StringBuilder();

            Arrays.sort(shortcodes); //sort alphabetical
            for (int i = 0; i < shortcodes.length; i++) {
                shortcodesRaw.append(shortcodes[i].replace("\n", ""));
                if(i != shortcodes.length - 1)
                    shortcodesRaw.append("\n");
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("shortcodes");
            editor.putString("shortcodes", shortcodesRaw.toString());
            editor.apply();
        }

        // Inform users about name change since version 2.6 or 2.6.1 (which was not tagged in master – version code 33)
        if (previousVersion <= 33) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.migration_dsbdirect_rename)
                    .setMessage(R.string.migration_dsbdirect_rename_message)
                    .setPositiveButton(R.string.ok, null)
                    .setNeutralButton(R.string.email_the_dev, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainSettingsFragment.emailTheDev(MainActivity.this);
                        }
                    })
                    .show();
        }

        // Inform about depreciation of self update after version 3.2.1 (version code 38)
        if (previousVersion <= 38 && BuildConfig.FLAVOR.equals("notabug")) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.migration_depreciation_self_update)
                    .setMessage(R.string.migration_depreciation_self_update_message)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }


        // Migrate users to most recent version by wiping news after update
        if (previousVersion < BuildConfig.VERSION_CODE) {
            Log.d("MIGRATION", "wiping news");
            NewsQuery.wipeNews(MainActivity.this);
        }

        // Start loading or show login screen
        if (mLoginManager.canLogin()) {
            login = mLoginManager.getActiveLogin();

            if (!initialized) {
                filterEnabled = u.getSharedPreferences().getBoolean("filter", false);
                initialized = true;
            }

            if (mSwipeLayout == null) {
                mSwipeLayout = findViewById(R.id.swipe_layout);
                mSwipeLayout.setOnRefreshListener(this::recreate);
            }

            // Start loading
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getContent();
                }
            }).start();
        } else {
            // Login required
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(loginIntent, REQUEST_LOGIN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            u.schedulePolling();
        }
        // Save version code for potential What's new stuff or migrations
        sharedPreferences.edit().putInt("version", BuildConfig.VERSION_CODE).apply();
    }

    private void openNoticeBoard() {
        Intent intent = new Intent(MainActivity.this, NoticeBoardActivity.class);
        if (mNoticeBoardItemList != null && !mNoticeBoardItemList.isEmpty())
            intent.putExtra(NoticeBoardActivity.EXTRA_NOTICE_BOARD_ITEMS, (Serializable) mNoticeBoardItemList);
        startActivity(intent);
    }

    private void networkErrorToUi(Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar
                        .make(findViewById(R.id.root),
                                getString(R.string.you_are_offline),
                                Snackbar.LENGTH_SHORT)
                        .show();
                // Enter offline mode
                offlineMode();

            }
        });

        e.printStackTrace();
    }

    private void getContent() {

        try {
            mTables = mDownloadManager.downloadTables(login);

            // Start download of notices and news
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mNoticeBoardItemList = mDownloadManager.downloadNoticeBoardItems(login);

                        // Display notices button, even if no content otherwise
                        if (mNoticeBoardItemList.size() > 0) {
                            onMenuCreated.add(new Runnable() {
                                @Override
                                public void run() {
                                    mMenu.add(R.string.action_notices)
                                            .setIcon(R.drawable.ic_newspaper)
                                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                                @Override
                                                public boolean onMenuItemClick(MenuItem item) {
                                                    openNoticeBoard();
                                                    return true;
                                                }
                                            }).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                                }
                            });
                        }

                    } catch (IOException e) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar
                                        .make(findViewById(R.id.contentCoordinator),
                                                R.string.network_request_notice_board_generic_error_snackbar,
                                                Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        });

                        e.printStackTrace();
                    }
                }
            }).start();



            // No content
            if (mTables == null || mTables.length == 0) {
                throw new NoContentException();
            }


            // Find out whether all tables might be parsable
            boolean findAllHtml = true;
            for (Table table : mTables) {
                if (!table.isHtml()) {
                    findAllHtml = false;
                    break;
                }
            }

            final boolean allHtml = findAllHtml;

            // It's not possible to merge if not all pages are html
            if (!allHtml) mMerge = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    mTextView.setText(getString(R.string.timetable_uri_acquired));

                    // Find date
                    mTimetabledate = mTables[0].getPublishedDate();

                    if (mMerge && mParse && allHtml) {
                        // All tables can be displayed at once
                        displayMultipleHtmlTimetables(new ArrayList<>(Arrays.asList(mTables)));

                        // Ensure pagePicker is gone
                        findViewById(R.id.page).setVisibility(View.GONE);
                    } else {
                        // Every table has to be displayed separately
                        // Display the first table
                        mCurrentTable = mTables[0];
                        displayTimetable(mTables[0]);

                        // Let user view other tables if necessary
                        if (mTables.length > 1) {
                            HorizontalPicker pagePicker = findViewById(R.id.page);
                            pagePicker.setVisibility(View.VISIBLE);

                            // Display table titles as values in horizontal picker
                            String[] titles = new String[mTables.length];
                            for (int i = 0; i < mTables.length; i++) {
                                titles[i] = mTables[i].getTitle();
                            }
                            pagePicker.setValues(titles);

                            pagePicker.setOnItemSelectedListener(new HorizontalPicker.OnItemSelected() {
                                @Override
                                public void onItemSelected(int index) {
                                    displayTimetable(mTables[index]);
                                }
                            });

                            pagePicker.setOnItemClickedListener(new HorizontalPicker.OnItemClicked() {
                                @Override
                                public void onItemClicked(int index) {
                                    displayTimetabledate(R.string.timetable_published, mTables[index].getPublishedDate());
                                }
                            });
                        }
                    }
                }
            });
        } catch (final UnexpectedResponseException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (e instanceof LoginFailureException) {
                        mTextView.setText(R.string.network_login_denied);

                        final Button reauth = findViewById(R.id.reauthenticate);
                        reauth.setVisibility(View.VISIBLE);
                        reauth.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), REQUEST_LOGIN);
                            }
                        });
                    } else if (e instanceof NoContentException) {
                        mTextView.setText(R.string.network_no_content);

                        // Don't display fix button
                        return;

                    } else {
                        mTextView.setText(R.string.network_invalid_response);
                    }

                    // Display fix button
                    final Button fix = findViewById(R.id.fix);
                    fix.setVisibility(View.VISIBLE);
                    fix.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            fix.setVisibility(View.GONE);
                            new Thread(new NewsQuery(MainActivity.this, mDownloadManager)).start();
                        }
                    });
                }
            });
            e.printStackTrace();
        } catch (IOException e) {
            networkErrorToUi(e);
        }
    }

    /**
     * Displays timetable, no matter whether it has already been downloaded or not.
     * @param table The table to be displayed
     */
    private void displayTimetable(final Table table) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    if (table.isHtml()) {

                        // Possibly init WebView early
                        final SharedPreferences sharedPreferences = u.getSharedPreferences();
                        if (sharedPreferences.getBoolean("renderWebViewEarly", false)) {
                            // Do init async to this thread: use new thread (async to this thread) to then run on ui thread (synchronous to new thread)
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d("DRAW", "(possibly) initializing WebView while getting file");
                                            initWebView();

                                            // We don't know whether it should be rendered early next time again
                                            sharedPreferences
                                                    .edit()
                                                    .putBoolean("renderWebViewEarly", false)
                                                    .apply();
                                        }
                                    });
                                }
                            }).start();
                        }

                        final String html = mFileManager.getHtmlTable(table, mDownloadManager);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayHtml(html);
                            }
                        });
                    } else {
                        final Bitmap image = mFileManager.getImageTable(table, mDownloadManager);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayImage(image);
                            }
                        });
                    }

                } catch (final IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            networkErrorToUi(e);
                        }
                    });
                    e.printStackTrace();
                }

            }
        }).start();
    }

    /**
     * Displays file available offline already
     *
     * @param file   File to be displayed
     * @param isHtml Whether the file is html
     */
    private void displayFile(File file, boolean isHtml) {
        if (isHtml) {
            String html = mFileManager.readHtmlFile(file);
            displayHtml(html);
        } else {
            Bitmap bitmap = mFileManager.readBitmapFile(file);
            displayImage(bitmap);
        }
    }

    private void displayHtml(final String response) {
        // In case an image had been shown previously, hide it
        ImageView image = findViewById(R.id.tableimage);
        image.setVisibility(View.GONE);

        if (mParse) {
            // Parse table

            // Create runnable
            final ReaderRunnable readerRunnable = new ReaderRunnable(MainActivity.this, response,
                    login.getId(), mReaderTasks
            );
            readerRunnable.addHandlers(
                    new Handler(new Handler.Callback() {
                        @Override
                        public boolean handleMessage(Message msg) {
                            // Display parsed entries
                            displayEntries(readerRunnable.getResult());

                            String schoolName = readerRunnable.getSchoolName();
                            if (schoolName != null) {
                                    //for users from the Mariengymnasium Warendorf, set window title to "Vertretungen am MGW"
                                    if (schoolName.equals("Mariengymnasium Warendorf ")) setTitle("Vertretungsplan am MGW");
                                    else setTitle(schoolName);

                                // Set login display name to school name if doesn't already have one
                                if (!mLoginManager.getActiveLogin().hasDisplayName()) {
                                    mLoginManager.getActiveLogin().setDisplayName(schoolName);
                                    mLoginManager.write();
                                }
                            }

                            return false;
                        }
                    }),
                    new Handler(new Handler.Callback() {
                        @Override
                        public boolean handleMessage(Message msg) {
                            // Display error message
                            displayErrorEntry();
                            return false;
                        }
                    })
            );

            // Start and add thread
            mReaderTasks.add(readerRunnable);

            // If the task we just added is the only task queued
            if (mReaderTasks.size() == 1) {
                // Start this task in a new thread
                new Thread(readerRunnable).start();
            }


        } else {
            ((TextView) findViewById(R.id.date)).setText("");

            // Render HTML code in WebView

            initWebView();
            mWebView.setVisibility(View.VISIBLE);

            // Save that the WebView should be rendered early next time
            u.getSharedPreferences()
                    .edit()
                    .putBoolean("renderWebViewEarly", true)
                    .apply();

            try {
                mWebView.loadDataWithBaseURL(null, response, "text/html", "UTF-8", null);
            } catch (NullPointerException e) {
                e.printStackTrace();
                mWebView.setVisibility(View.GONE);
                mTextView.setText(getString(R.string.error_displaying));
                mTextView.setVisibility(View.VISIBLE);
            }
        }

        potentiallyDisplayTimetabledate();
    }

    private void initWebView() {
        if (mWebView == null) {
            long webViewInitStart = System.currentTimeMillis();
            mWebView = new WebView(this);
            mWebView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            );
            ((ViewGroup) findViewById(R.id.content)).addView(mWebView);

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setBlockNetworkLoads(true);
            webSettings.setJavaScriptEnabled(false);

            mWebView.setVisibility(View.GONE);

            Log.d("DRAW", "initialized WebView within " + (System.currentTimeMillis() - webViewInitStart)
                    + " milliseconds");
        }
    }

    private void displayErrorEntry() {
        ArrayList<Entry> error = new ArrayList<>();
        error.add(new ErrorEntry(this));
        displayEntries(error);
    }

    private void displayEntries(final ArrayList<Entry> entries) {

        RecyclerView recyclerView = findViewById(R.id.table);

        if (recyclerView.getLayoutManager() == null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            if (u.getSharedPreferences().getString("layout", Adapter.LAYOUT_CARDS)
                    .equals(Adapter.LAYOUT_LIST))
                // Add divider
                recyclerView.addItemDecoration(
                        new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
                );
            else
                recyclerView.setPadding(0, 0, 0, u.dpToPx(4));

            recyclerView.setItemViewCacheSize(20);
        }

        if (mAdapter == null || !mMerge) {
            mAdapter = new Adapter(MainActivity.this, entries);
            mAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);

            ((TextView) findViewById(R.id.date)).setText("");

            recyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.addAll(entries);
        }

        recyclerView.setVisibility(View.VISIBLE);


        if (mAdapter.getItemCount() > 0) {
            mTextView.setVisibility(View.GONE);

            final TextView dateView = findViewById(R.id.date);
            dateView.setVisibility(View.VISIBLE);

            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    try {
                        int firstVisibleItem =
                                ((LinearLayoutManager) recyclerView.getLayoutManager())
                                        .findFirstVisibleItemPosition();
                        dateView.setText(u.formatDate(mAdapter.get(firstVisibleItem).getDate()));
                    } catch (IndexOutOfBoundsException | NullPointerException e) {
                        // does not matter because this happens when list is empty
                    }

                }
            });

        } else {
            mTextView.setText(R.string.empty);
            mTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    /**
     * Display multiple html tables at once (concatenate them)
     *
     * @param tables Tables to be potentially downloaded and displayed
     */
    private void displayMultipleHtmlTimetables(final ArrayList<Table> tables) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Table table = tables.get(0);

                tables.remove(table);
                try {
                    final String html = mFileManager.getHtmlTable(table, mDownloadManager);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            displayHtml(html);

                            // Display remaining tables
                            // This has to be done after the previous entries have been parsed to not mix up the order
                            if (tables.size() > 0) {
                                // To understand recursion, you first have to understand recursion
                                displayMultipleHtmlTimetables(tables);
                            }
                        }
                    });
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayErrorEntry(); // Missing just a part!

                        }
                    });
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void displayImage(Bitmap bitmap) {
        // Hide text
        mTextView.setVisibility(View.GONE);

        // Hide list, date text and webView in case they had previously been displayed
        RecyclerView recyclerView = findViewById(R.id.table);
        TextView dateText = findViewById(R.id.date);
        dateText.setText("");
        dateText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        if (mWebView != null) {
            mWebView.setVisibility(View.GONE);
        }

        // Zoom out
        TouchImageView image = findViewById(R.id.tableimage);
        image.resetZoom();

        BitmapDrawable drawable = new BitmapDrawable(bitmap);

        // Invert colors
        if (u.getSharedPreferences().getBoolean("invertImages", false)) {
            // Thanks, https://stackoverflow.com/a/17871384
            final float[] NEGATIVE = {
                    -1.0f, 0, 0, 0, 255, // red
                    0, -1.0f, 0, 0, 255, // green
                    0, 0, -1.0f, 0, 255, // blue
                    0, 0, 0, 1.0f, 0  // alpha
            };
            drawable.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
        }


        // Show image
        image.setImageDrawable(drawable);
        image.setVisibility(View.VISIBLE);


        potentiallyDisplayTimetabledate();

    }

    private void offlineMode() {

        // Change title to notify user offline mode is enabled
        getSupportActionBar().setTitle(R.string.app_name_offline);

        // Check checkbox
        onMenuCreated.add(new Runnable() {
            @Override
            public void run() {
                mMenu.findItem(R.id.action_view_history).setChecked(true);
                setTempFilterIcon(mMenu);
            }
        });


        // Don't merge
        mMerge = false;

        // Find every file, sorted descending by last modified
        final File[] files = mFileManager.getFilesSorted();

        if (files.length == 0) {
            // We're done
            return;
        }

        // Read every file to a Table
        mTables = new Table[files.length];

        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            String url = file.getName().replaceAll("\\d+-", "");

            Date date;
            // Who knows what files might be in our file system
            try {
                String time = file.getName().split("-")[0];
                date = new Date(Long.parseLong(time));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                date = new Date();
            }

            boolean isHtml;
            try {
                String[] fileNameParts = file.getName().split("\\.");
                String suffix = fileNameParts[fileNameParts.length - 1];

                isHtml = suffix.contains("htm");
            } catch (ArrayIndexOutOfBoundsException e) {
                // In this case, there was no dot in the filename

                isHtml = false;
            }

            mTables[i] = new Table(url, date, isHtml, Humanize.naturalTime(date));
        }

        // Don't do it. It's dumb
        //mTimetabledate = mTables[0].getPublishedDate();

        // Display the first table
        HorizontalPicker pagePicker = findViewById(R.id.page);
        pagePicker.setSelectedItem(0);
        mCurrentTable = mTables[0];
        if(offlineFilterPage != 0) {
            displayFile(files[offlineFilterPage], mTables[offlineFilterPage].isHtml());
            displayTimetabledate(R.string.timetable_published, mTables[offlineFilterPage].getPublishedDate());
            pagePicker.setSelectedItem(offlineFilterPage);
        }else
            displayFile(files[0], mTables[0].isHtml());

        // Let user view other files
        if (mTables.length > 1) {
            pagePicker.setVisibility(View.VISIBLE);

            // Initialize CharSequence[] for setting them as values
            CharSequence[] values = new CharSequence[mTables.length];
            for (int i = 0; i < mTables.length; i++) {
                values[i] = Humanize.naturalTime(mTables[i].getPublishedDate());
            }

            pagePicker.setValues(values);
            pagePicker.setOnItemSelectedListener(new HorizontalPicker.OnItemSelected() {
                @Override
                public void onItemSelected(int index) {
                    displayFile(files[index], mTables[index].isHtml());
                    offlineFilterPage = index;
                }
            });

            pagePicker.setOnItemClickedListener(new HorizontalPicker.OnItemClicked() {
                @Override
                public void onItemClicked(int index) {
                    displayTimetabledate(R.string.timetable_published, mTables[index].getPublishedDate());
                }
            });
        }
    }

    private void potentiallyDisplayTimetabledate() {
        // Only do this once
        if (!mTimetabledateDisplayed && mTimetabledate != null) {
            displayTimetabledate(R.string.timetable_last_changed, mTimetabledate);
            mTimetabledateDisplayed = true;
        }
    }

    private void displayTimetabledate(@StringRes int message, Date mTimetabledate) {
        String ago = Humanize.naturalTime(mTimetabledate, TimeMillis.HOUR);
        if (ago.isEmpty()) {
            ago = Humanize.naturalTime(mTimetabledate, TimeMillis.SECOND);
        }
        if (System.currentTimeMillis() - mTimetabledate.getTime() < 60 * 1000 || ago.isEmpty()) {
            ago = getString(R.string.timetable_updated_now);
        }
        Log.d("TIMETABLEDATE", ago);
        Snackbar
                .make(findViewById(R.id.contentCoordinator),
                        getString(message, ago),
                        Snackbar.LENGTH_LONG)
                .show();
    }

    public static void setOverflowButtonColor(final Activity activity, final int color) {
        // from https://stackoverflow.com/a/36278375 by https://stackoverflow.com/users/5064289/barun-kumar
        final String overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<View>();
                decorView.findViewsWithText(outViews, overflowDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) {
                    return;
                }
                AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
                overflow.setColorFilter(color);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        if (getResources().getBoolean(R.bool.news_check_button)) {
            menu.add(Menu.NONE, 4, Menu.NONE, R.string.action_check_news);
        }

        // Add all inactive logins as a submenu if there is one
        if (mLoginManager.getLoginCount() > 1) {
            SubMenu loginMenu = menu.addSubMenu(R.string.action_switch_login);
            for (Login l :
                    mLoginManager.getInactiveLogins()) {
                loginMenu.add(Menu.NONE, Integer.parseInt(l.getId()), Menu.NONE, l.getDisplayName());
            }
            loginMenu.add(Menu.NONE, 5, Menu.NONE, R.string.action_add_login).setIcon(R.drawable.ic_add_black_24dp);
        }

        setTempFilterIcon(menu);

        // Pass it around!
        mMenu = menu;

        // Start thread which executes onMenuCreated queue
        menuOperationsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    for (;;) {
                        Runnable r = onMenuCreated.take();
                        runOnUiThread(r);
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        menuOperationsThread.start();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reload:
                recreate();

                break;
            case R.id.action_temp_filters:
                //enable/disable filters for current session
                filterEnabled = !filterEnabled;
                if(mMenu.findItem(R.id.action_view_history).isChecked())
                    offlineMode();
                else
                    recreate();
                break;
            case R.id.action_view_history:
                if (item.isChecked()) {
                    offlineFilterPage = 0;
                    recreate();
                } else {
                    item.setChecked(true);
                    offlineMode();
                }
                break;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);

                // Pass along whether plan contains html files and whether plan only consists of html files
                if (mTables != null) {
                    // At least one file must be html
                    boolean containsHtml = false;
                    boolean allHtml = true;
                    for (Table table : mTables) {
                        if (table.isHtml()) {
                            // This table is html, therefore the plan contains html
                            containsHtml = true;
                        } else {
                            // This table is not html, therefore it can't be that all pages are html
                            allHtml = false;
                        }
                    }

                    settingsIntent.putExtra(SettingsActivity.EXTRA_CONTAINS_HTML, containsHtml);
                    settingsIntent.putExtra(SettingsActivity.EXTRA_HTML_ONLY, allHtml);
                }

                startActivityForResult(settingsIntent,
                        SettingsActivity.class.getName().length() // I needed a constant number, so I chose something simple & easy to remember
                );
                break;
            case 5:
                startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), REQUEST_LOGIN);
                break;
            case 4:
                new Thread(new NewsQuery(MainActivity.this, mDownloadManager)).start();
                break;
            default:
                // Find out whether this id fits any login id
                for (Login l :
                        mLoginManager.getLogins()) {
                    if (item.getItemId() == Integer.parseInt(l.getId())) {
                        Log.d("LOGINSWITCH", "to login " + l.getDisplayName());
                        mLoginManager.setActiveLogin(l);
                        recreate();
                        return true;
                    }
                }

                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
        return true;
    }

    private void showAuthPrompt() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.credentials_popup_title)
                .setMessage(R.string.credentials_popup_message)
                .setNegativeButton(R.string.dismiss, null)
                .setPositiveButton(R.string.login_login, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), REQUEST_LOGIN);
                    }
                })
                .show();
    }

    @Override
    public void recreate() {
        super.recreate();

        // Reset pagePicker position
        ((HorizontalPicker) findViewById(R.id.page)).setSelectedItem(0);

        if (mSwipeLayout != null) {
            mSwipeLayout.setRefreshing(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        recreate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //int settingsActivityRequestCode = SettingsActivity.class.getName().length(); // A way to get a cool number
        // Whatever request this is happened, we want to reload if it was successful
        if (resultCode == RESULT_OK) {
            recreate(); // Because we can't really refresh very well
        } else if (resultCode == RESULT_CANCELED && requestCode == REQUEST_LOGIN) {
            // Login was cancelled, quit if not debug and there are no logins
            if (getResources().getBoolean(R.bool.authenticate_quit_on_cancel) && mLoginManager.getLoginCount() < 1)
                finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    private void setTempFilterIcon(@NonNull Menu menu){
        if(u.getSharedPreferences().getBoolean("filter", false)&&u.getSharedPreferences().getBoolean("parse", false)){
            if(filterEnabled){
                menu.findItem(R.id.action_temp_filters).setIcon(R.drawable.ic_visibility_24px);
                menu.findItem(R.id.action_temp_filters).setTitle(R.string.action_temp_filters_disable);
            } else{
                menu.findItem(R.id.action_temp_filters).setIcon(R.drawable.ic_visibility_off_24px);
                menu.findItem(R.id.action_temp_filters).setTitle(R.string.action_temp_filters_enable);
            }
            menu.findItem(R.id.action_temp_filters).setVisible(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (menuOperationsThread != null) {
            menuOperationsThread.interrupt();
        }
        super.onDestroy();
    }
}
