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

package godau.fynn.dsbdirect.util;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.*;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import android.view.Window;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.service.PollingService;
import godau.fynn.dsbdirect.table.reader.*;
import org.jsoup.nodes.Element;

import java.text.SimpleDateFormat;
import java.util.*;

import static android.content.Context.MODE_PRIVATE;

public class Utility {

    public static final String DATE_FORMAT = "EEEE, d.M.yyyy";

    /**
     * For Gadgetbridge compatibility: Gadgetbridge seems to only push content text, not big text text
     */
    public static final String SUPER_SECRET_SETTING_TEXT_AS_CONTENT_TEXT = "textAsContentText";

    /**
     * For debugging: display messages even if the exact same content has already been displayed
     */
    public static final String SUPER_SECRET_SETTING_NOTIFY_ABOUT_NOTHING_NEW = "ignoreAlreadyDisplayed";

    /**
     * For debugging: also poll on weekends
     */
    public static final String SUPER_SECRET_SETTING_POLL_ON_WEEKENDS = "ignoreWeekend";

    /**
     * For convenience: in case you find sharing annoying, you can disable it
     */

    public static final String SUPER_SECRET_SETTING_HOLD_TO_SHARE = "holdToShare";

    /**
     * For remembering which school you're at: display school name as window title even if there is only one login
     */
    public static final String SUPER_SECRET_SETTING_FORCE_SCHOOL_NAME_AS_WINDOW_TITLE = "schoolNameAsWindowTitle";

    private Context mContext;

    public Utility(Context context) {
        this.mContext = context;
    }

    public String formatDate(Date date) {
        Calendar today = Calendar.getInstance();

        Calendar then = Calendar.getInstance();
        then.setTime(date);

        if (then.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        && then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return mContext.getString(R.string.today);
        }

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        if (then.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR)
                && then.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)) {
            return mContext.getString(R.string.tomorrow);
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        if (then.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                && then.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
            return mContext.getString(R.string.yesterday);
        }

        // Date is neither today, yesterday nor tomorrow, format it

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return simpleDateFormat.format(date);
    }

    public @Nullable Reader getReader(String html, String id) {
        return ReaderFactory.getReader(html, id, mContext);
    }

    /**
     * Parse a natural date as provided as a last updated date
     * @param input A date in the format MM.dd.yyyy HH:mm
     * @return A Date
     */
    public static Date parseLastUpdatedDate(String input) {
        String[] dateParts = input.split(" ");
        String[] dateDigits = dateParts[0].split("\\.");
        String[] timeDigits = dateParts[1].split(":");
        return new Date(Integer.parseInt(dateDigits[2]) - 1900 /* years start at 1900 */, Integer.parseInt(dateDigits[1]) - 1 /* months start with 0 */,
                Integer.parseInt(dateDigits[0]), Integer.parseInt(timeDigits[0]), Integer.parseInt(timeDigits[1])); // because SimpleDateTime did deliver good results, again
    }

    public SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences("default", MODE_PRIVATE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void schedulePolling() {
        // Check whether polling is enabled
        SharedPreferences sharedPreferences = getSharedPreferences();
        if (sharedPreferences.getBoolean("poll", false)) {
            // Polling is enabled

            ComponentName serviceComponent = new ComponentName(mContext, PollingService.class);
            JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
            builder.setMinimumLatency(PollingService.getNextPollingTimeDelay(sharedPreferences)); // Poll after at least this time
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // Only execute once network is connected
            builder.setPersisted(true);
            JobScheduler jobScheduler = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(builder.build());
        } else {
            // Cancel potential job
            JobScheduler jobScheduler = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(0);
        }
    }

    /**
     * @return A String containing String representations of all array elements separated by comma
     */
    public static <T> String smartConcatenate(T[] array, String comma) {
        StringBuilder result = new StringBuilder();

        // Remove empty strings
        ArrayList<T> list = new ArrayList<>(Arrays.asList(array));
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) == null || list.get(i).equals("") || list.get(i).equals("<strike></strike>")) {
                list.remove(i);
            }
        }

        // Append remaining strings to result
        for (int i = 0; i < list.size(); i++) {
            T t = list.get(i);

            if (t instanceof Element) {
                result.append(((Element) t).text().replaceAll("\\\\n", "\n"));
            } else
                result.append(t.toString());

            if (i != list.size() - 1) {
                // This is not the last iteration yet
                result.append(comma);
            }

        }

        return String.valueOf(result);
    }

    public static Calendar zeroOClock(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    /**
     * @author https://stackoverflow.com/a/9904844
     */
    public int dpToPx(int dp) {
        // Get the screen's density scale
        final float scale = mContext.getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (dp * scale + 0.5f);
    }

    public @StyleRes
    int getStyle() {
        String preference = getSharedPreferences().getString("style", "light");

        switch (preference) {
            case "light":
            default:
                return R.style.Light;
            case "dark":
                return R.style.Dark;
            case "black":
                return R.style.Black;
        }
    }

    public @ColorInt int getColorPrimary() {
        return getSharedPreferences().getInt("colorPrimary",
                mContext.getResources().getColor(R.color.colorPrimary));
    }

    public @ColorInt int getColorPrimaryDark() {
        return getSharedPreferences().getInt("colorPrimaryDark",
                mContext.getResources().getColor(R.color.colorPrimaryDark));
    }

    public @ColorInt int getColorAccent() {
        return getSharedPreferences().getInt("colorAccent",
                mContext.getResources().getColor(R.color.colorAccent));
    }

    public void stylize() {


        mContext.setTheme(getStyle());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @ColorInt int colorPrimaryDark = getColorPrimaryDark();
            Window window = ((Activity) mContext).getWindow();
            window.setNavigationBarColor(colorPrimaryDark);
            window.setStatusBarColor(colorPrimaryDark);
        }
    }


}
