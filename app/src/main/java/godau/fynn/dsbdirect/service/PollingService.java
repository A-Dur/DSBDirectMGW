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

package godau.fynn.dsbdirect.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;
import godau.fynn.dsbdirect.model.Login;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.activity.MainActivity;
import godau.fynn.dsbdirect.download.DownloadManager;
import godau.fynn.dsbdirect.persistence.FileManager;
import godau.fynn.dsbdirect.persistence.LoginManager;
import godau.fynn.dsbdirect.model.Table;
import godau.fynn.dsbdirect.model.entry.Entry;
import godau.fynn.dsbdirect.table.reader.Filter;
import godau.fynn.dsbdirect.table.reader.Reader;
import humanize.Humanize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@RequiresApi(LOLLIPOP)
public class PollingService extends JobService {
    private static final String CHANNEL = "plan_update";

    private static final int NOTIFICATION_ID = 70700707; // A random number my brother told me when I asked him for one

    private SharedPreferences mSharedPreferences;
    private DownloadManager mDownloadManager;

    private Utility u;

    private Login login;


    @Override
    public boolean onStartJob(final JobParameters parameters) {

        Log.d("POLLING", "polling now");

        new Thread(new Runnable() {
            @Override
            public void run() {
                u = new Utility(PollingService.this);
                mSharedPreferences = u.getSharedPreferences();
                MainActivity.filterEnabled = mSharedPreferences.getBoolean("filter", false);

                LoginManager loginManager = new LoginManager(PollingService.this);
                login = loginManager.getActiveLogin();

                mDownloadManager = DownloadManager.getDownloadManager(PollingService.this);

                try {

                    onTimetableListDownloaded(mDownloadManager.downloadTables(login));

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Schedule next polling
                u.schedulePolling();

                jobFinished(parameters, false);
            }
        }).start();


        return true;
    }

    /**
     * To be called after the timetable list has been downloaded.
     *
     * @param tables List of timetables
     */
    private void onTimetableListDownloaded(Table[] tables) {

        try {
            ArrayList<Entry> entries = new ArrayList<>();

            FileManager fileManager;
            boolean download = mSharedPreferences.getBoolean("autoDownload", true);
            if (download) {
                fileManager = new FileManager(PollingService.this);
            } else {
                fileManager = null;
            }

            // Process every table
            for (int i = 0; i < tables.length; i++) {
                Table table = tables[i];

                // Only parse if requirements are met
                if (table.isHtml()
                        && mSharedPreferences.getBoolean("parse", true)
                        && download) {

                    String html = fileManager.getHtmlTable(table, mDownloadManager);
                    Reader reader = u.getReader(html, login.getId());
                    entries.addAll(Filter.filterToday(
                            Filter.filterUserFilters(
                                    reader.read(), PollingService.this
                            )
                    ));

                    // Create notification after last iteration
                    if (i == tables.length - 1) {
                        createParsedNotification(table, entries);
                    }


                } else {
                    // Parsing is impossible, contents don't matter

                    // Download anyway if automatic download is enabled
                    if (download) {
                        fileManager.getTable(table, mDownloadManager);
                    }

                    if (i == tables.length - 1) {
                        // Notify about this table if new
                        createNotification(table);
                    }

                }
            }

        } catch (IOException  e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private void createParsedNotification(Table table, ArrayList<Entry> entries) {
        // The entries have finally been received and could be parsed

        SharedPreferences sharedPreferences = u.getSharedPreferences();


        // Create notification text
        StringBuilder text = new StringBuilder();

        if (entries.size() == 0) {
            // There are no entries

            // Get configure behaviour
            boolean displayEmptyNotifications = sharedPreferences.getBoolean("displayEmpty", false);
            if (displayEmptyNotifications) {
                text.append(getString(R.string.notification_plan_html_updates_empty));
            } else {
                // Cancel notification creation
                return;
            }
        } else {
            // Some entries are present
            for (int i = 0; i < entries.size(); i++) {
                Entry.CompatEntry entry = ((Entry) entries.get(i)).getCompatEntry();

                text.append(Utility.smartConcatenate(
                        new String[]{
                                entry.getAffectedClass(),
                                entry.getLesson(),
                                entry.getReplacementTeacher(),
                                entry.getInfo()
                        }, " · "
                ));

                if (i != entries.size() - 1) {
                    // This is not the last iteration yet
                    text.append("\n");
                }

            }
        }


        // Only notify if text is different from the last notification

        String lastText = sharedPreferences.getString("lastNotification", "Attention: the developer is programming late at night again");

        if (text.toString().equals(lastText)
                && sharedPreferences.getBoolean(Utility.SUPER_SECRET_SETTING_NOTIFY_ABOUT_NOTHING_NEW, false) == false // For debugging
        ) {
            // Notification already displayed
            Log.d("POLLINGNOTIFICATION", "notification not displayed because of text match (" + text.toString() + ")");
            return;
        }

        // Notification can be displayed

        // Store text to shared preferences
        sharedPreferences.edit().putString("lastNotification", text.toString()).apply();

        // Create content text / summary
        String contentText;
        if (sharedPreferences.getBoolean(Utility.SUPER_SECRET_SETTING_TEXT_AS_CONTENT_TEXT, false)) { // Developer has special needs, so he implemented a super secret setting
            contentText = text.toString().replaceAll("\n", " – ");
        } else {
            contentText = Humanize.pluralize(
                    getString(R.string.notification_plan_updates_text_pluralize_entries_one),
                    getString(R.string.notification_plan_updates_text_pluralize_entries_many),
                    getString(R.string.notification_plan_html_updates_empty),
                    entries.size());
        }


        // Create notification channel because API 26 demands it
        createNotificationChannel();

        // Expire notification at 0 o'clock
        Calendar timeoutAt = Calendar.getInstance();
        timeoutAt.add(Calendar.DAY_OF_YEAR, 1);
        Utility.zeroOClock(timeoutAt);
        // Timeout is then minus now
        long timeoutAfter = timeoutAt.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

        // Create notification
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, new NotificationCompat.Builder(PollingService.this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_plan_html_updates_title))
                .setContentText(contentText)
                .setContentIntent(PendingIntent.getActivity(this, 2201,
                        new Intent(PollingService.this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setStyle(
                        new NotificationCompat.BigTextStyle().bigText(text.toString().replaceAll("</?s(trike)*>", "~")
                                .replaceAll("</?br/?>", " ")))
                .setColorized(true)
                .setColor(Color.argb(255, 63, 81, 181))
                .setTimeoutAfter(timeoutAfter)
                .setAutoCancel(true) // Parsed notifications should disappear after they had been opened
                .build()
        );
    }

    private void createNotification(Table table) {

        SharedPreferences sharedPreferences = u.getSharedPreferences();

        String uri = table.getUri();

        // Only notify if image is different than last time

        String lastUri = sharedPreferences.getString("lastNotification", "Attention: the developer is programming late at night again");

        if (uri.equals(lastUri)
                && sharedPreferences.getBoolean(Utility.SUPER_SECRET_SETTING_NOTIFY_ABOUT_NOTHING_NEW, false) == false // For debugging
        ) {
            // Notification already displayed
            Log.d("POLLINGNOTIFICATION", "notification not displayed because of uri match (" + uri + ")");
            return;
        }

        // Notification can be displayed

        // Store text to shared preferences
        sharedPreferences.edit().putString("lastNotification", uri).apply();


        // Create notification channel
        createNotificationChannel();

        // Create notification
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, new NotificationCompat.Builder(PollingService.this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_plan_updates_title))
                .setContentIntent(PendingIntent.getActivity(this, 2202,
                        new Intent(PollingService.this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setColorized(true)
                .setColor(Color.argb(255, 63, 81, 181))
                .setAutoCancel(true) // "New plan available" notification should disappear once the new plan has been seen
                .build()
        );

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL, getString(R.string.notification_channel_plan_updates), importance);
            channel.setDescription(getString(R.string.notification_channel_plan_updates_description));

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Methods for getting the next polling time

    public static long getNextPollingTime(SharedPreferences sharedPreferences) {
        return getNextPollingTime(sharedPreferences, Calendar.getInstance());
    }

    public static long getNextPollingTime(SharedPreferences sharedPreferences, Calendar now) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now.getTimeInMillis());

        String pollingIntervalString = sharedPreferences.getString("pollingInterval", "5");

        try {
            calendar.add(Calendar.MINUTE, Integer.parseInt(pollingIntervalString));

            // If super secret setting for polling on weekends is not enabled, don't poll on weekends
            if (sharedPreferences.getBoolean(Utility.SUPER_SECRET_SETTING_POLL_ON_WEEKENDS, false) == false) {

                // If this is a weekend, go to next Monday
                switch (calendar.get(Calendar.DAY_OF_WEEK)) {
                    case Calendar.SATURDAY:
                        // If this a Saturday, go to Monday zero o' clock
                        calendar.add(Calendar.DAY_OF_MONTH, 2);
                        Utility.zeroOClock(calendar);
                        break;
                    case Calendar.SUNDAY:
                        // If this is a Sunday, go to tomorrow zero o' clock
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        Utility.zeroOClock(calendar);
                }

            }

            // If this is late at night, go to the morning
            if (calendar.get(Calendar.HOUR_OF_DAY) < 5) {
                Utility.zeroOClock(calendar);
                calendar.set(Calendar.HOUR_OF_DAY, 5);
            }

            //If this is after 4pm, go to the next morning (If setting is enabled)
            boolean doNotCheckAfter4Pm = sharedPreferences.getBoolean("doNotCheckAfter4Pm", false);

            if (calendar.get(Calendar.HOUR_OF_DAY) >= 16 && doNotCheckAfter4Pm) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 5);
            }

            Log.d("POLLINGTIME", calendar.getTimeInMillis() + ", which is in " + (calendar.getTimeInMillis() - now.getTimeInMillis()));

            return calendar.getTimeInMillis();

        } catch (NumberFormatException e) {
            // There was no int, whatever
        }
        return 0;
    }

    public static long getNextPollingTimeDelay(SharedPreferences sharedPreferences) {
        return getNextPollingTimeDelay(sharedPreferences, Calendar.getInstance());
    }

    public static long getNextPollingTimeDelay(SharedPreferences sharedPreferences, Calendar now) {
        long nextTime = getNextPollingTime(sharedPreferences, now);
        return nextTime - now.getTimeInMillis();
    }
}