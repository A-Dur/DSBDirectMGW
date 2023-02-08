package godau.fynn.dsbdirect.download;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.download.exception.UnexpectedResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NewsQuery implements Runnable {

    /*
     * News structure:
     * {
     *  "statusMessage": "Message for the user",
     *  "statusMessageDE": "Nachricht an Nutzer",
     *  "queryBodyBaseJson": "optional: das neue queryBodyBaseJson",
     *  "querySendAppId": boolean, optional: ob die Client irgendeine AppId generieren soll
     *  "querySendAndroidVersion": boolean, optional: ob die Client sich irgendeine Android-Version aussuchen soll
     *  "querySendDeviceModel": boolean, optional: ob die Client sich irgendein Gerätemodell aussuchen soll
     *  "querySendLanguage": boolean, optional: ob die Client sich eine Sprache aussuchen soll
     *  "querySendDate": boolean, optional
     *  "querySendLastDate": boolean, optional
     *  "queryHeaders": ["optional", "Array mit headers"]
     *  "queryEndpoint": 0 (mobile) / 1 (web) / 2 (ihkmobile) / 3 (appihkbb)
     * }
     */

    Activity context;
    DownloadManager downloadManager;
    Utility u;

    public NewsQuery(Activity context, DownloadManager downloadManager) {
        this.context = context;
        this.downloadManager = downloadManager;
        u = new Utility(context);
    }

    @Override
    public void run() {
        try {
            JSONObject news = downloadManager.downloadNews();
            SharedPreferences sharedPreferences = u.getSharedPreferences();

            // Get message from news

            String message;

            // German or not german (hardcoded and uncool)
            if (Locale.getDefault().toString().contains("de")) {
                message = news.getString("statusMessageDE");
            } else {
                message = news.getString("statusMessage");
            }

            // Get new query body base json
            if (news.has("queryBodyBaseJson")) {
                sharedPreferences
                        .edit()
                        .putString("queryBodyBaseJson", news.getString("queryBodyBaseJson"))
                        .apply();
            }

            // Get booleans
            saveBoolean(news, "querySendAppId", sharedPreferences);
            saveBoolean(news, "querySendAndroidVersion", sharedPreferences);
            saveBoolean(news, "querySendDeviceModel", sharedPreferences);
            saveBoolean(news, "querySendLanguage", sharedPreferences);
            saveBoolean(news, "querySendDate", sharedPreferences);
            saveBoolean(news, "querySendLastDate", sharedPreferences);

            // Get new headers
            if (news.has("queryHeaders")) {
                JSONArray headers = news.getJSONArray("queryHeaders");

                Set<String> headerSet = new HashSet<String>();

                for (int i = 0; i < headers.length(); i++) {
                    String header = headers.getString(i);
                    headerSet.add(header);
                }

                u.getSharedPreferences()
                        .edit()
                        .putStringSet("queryHeaders", headerSet)
                        .apply();
            }

            // Get new endpoint
            if (news.has("queryEndpoint")) {
                int endpoint = news.getInt("queryEndpoint");

                u.getSharedPreferences()
                        .edit()
                        .putInt("queryEndpoint", endpoint)
                        .apply();
            }


            // Show news
            popup(message);

        } catch (JSONException | UnexpectedResponseException ex) {
            popup(context.getString(R.string.news_network_invalid_response));
            ex.printStackTrace();
        } catch (IOException ex) {
            popup(context.getString(R.string.news_network_error_generic));
            ex.printStackTrace();
        }
    }

    private void saveBoolean(JSONObject news, String name, SharedPreferences sharedPreferences) throws JSONException {
        if (news.has(name)) {
            sharedPreferences
                    .edit()
                    .putBoolean(name, news.getBoolean(name))
                    .apply();
        }
    }

    private void popup(final String message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setMessage(message)
                        .show();
            }
        });

    }

    public static void wipeNews(Context context) {
        new Utility(context).getSharedPreferences().edit()
                .remove("queryBodyBaseJson")
                .remove("querySendDate")
                .remove("querySendLastDate")
                .remove("queryHeaders")
                .remove("queryEndpoint")
                .apply();
    }
}
