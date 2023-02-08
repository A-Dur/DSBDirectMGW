/*
 * DSBDirect
 * Copyright (C) 2020 Fynn Godau
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

package godau.fynn.dsbdirect.download;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import godau.fynn.dsbdirect.download.exception.LoginFailureException;
import godau.fynn.dsbdirect.download.exception.UnexpectedResponseException;
import godau.fynn.dsbdirect.model.noticeboard.NewsItem;
import godau.fynn.dsbdirect.model.noticeboard.Notice;
import godau.fynn.dsbdirect.model.noticeboard.NoticeBoardItem;
import godau.fynn.dsbdirect.model.Table;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import godau.fynn.dsbdirect.model.Login;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.table.reader.Reader;

/**
 * Queries app.dsbcontrol.de for DSB data (or endpoints which have
 * the same data structure) (legacy)
 */
public class DsbAppDownloadManager extends DownloadManager {

    List<Notice> notices = null;
    List<NewsItem> news = null;

    public DsbAppDownloadManager(Context context) {
        super(context);
    }

    /**
     * Make a GET request (synchronously)
     *
     * @param url           URL to be requested
     * @param body          Request body (JSON String)
     * @param requestMethod Usually either GET or POST
     * @return Response
     * @throws IOException If networking error or other IO exception
     */
    @Override
    @NonNull
    protected InputStream request(String url, @Nullable String body, String requestMethod) throws IOException {

        if (!isNetworkAvailable()) throw new IOException();

        // Encode the url correctly, as the file name part can contain Umlaute or other weird things
        String[] urlParts =
                url.split("/(?!.*/)"); // Matches only the last '/' character (using a negative lookahead)

        url = urlParts[0] + "/" + URLEncoder.encode(
                urlParts[1] // This is the part that has to be encoded correctly
                        .replaceAll(
                                "%20", " " /* Spaces are already encoded as "%20". Let's decode them quickly so we won't have
                                 * %20 encoded as something like "%2520"
                                 */
                        ), "ISO-8859-1" // UTF-8 won't work here
        )
                .replaceAll(
                        "\\+", "%20" /* Spaces are encoded again, but they are now '+' chars. That's unfortunately not
                                      * correct.  Let's replace them with "%20"s.
                                      */
                );

        URL connectwat = new URL(url);
        HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

        Security.setSocketFactory(urlConnection);

        urlConnection.setRequestMethod(requestMethod);

        // Add DNT header, as if it does anything
        urlConnection.addRequestProperty("DNT", "1");

        if (body != null) {

            // Get headers from sharedPreferences so they can be set through news
            HashSet queryHeaders = (HashSet) new Utility(context).getSharedPreferences()
                    .getStringSet("queryHeaders", new HashSet<>(Arrays.asList(
                            "Referer: https://www.dsbmobile.de/",
                            "Content-Type: application/json;charset=utf-8"
                    )));

            // Add each header to query
            for (Object header : queryHeaders) {
                String queryHeader = (String) header;

                // Split header into two parts
                String[] queryHeaderParts = queryHeader.split(": ");

                // Check whether header really is two parts
                if (queryHeaderParts.length != 2) {
                    Log.e("DOWNLOADHEADER", "invalid header: " + queryHeader);
                    continue;
                }

                // Add header to request
                urlConnection.setRequestProperty(queryHeaderParts[0], queryHeaderParts[1]);

                Log.d("DOWNLOADHEADER", queryHeader);
            }

            OutputStream outputStream = urlConnection.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write(body);
            outputStreamWriter.flush();
            outputStreamWriter.close();
            outputStream.close();
        }

        urlConnection.connect();

        return new BufferedInputStream(urlConnection.getInputStream());
    }

    @Override
    public Table[] downloadTables(Login login) throws IOException {

        JSONArray contentArray = downloadContentJSONArray(login);

        Table[] tables = null;

        try {
            for (int i = 0; i < contentArray.length(); i++) {
                JSONObject contentObject = contentArray.getJSONObject(i);

                String contentName = contentObject.getString("Title");

                JSONArray childs = contentObject.getJSONObject("Root").getJSONArray("Childs");

                // It has been observed that News are before Pläne if they exist
                switch (contentName) {
                    case "Pläne":
                        tables = Reader.readTableList(childs);
                        break;
                    case "News":
                        news = Reader.readNewsList(childs);
                        break;
                    case "Aushänge":
                        notices = Reader.readNoticeList(childs);
                }
            }
        } catch (JSONException e) {
            // Response is invalid, throw further
            throw new UnexpectedResponseException(e.getCause());
        }

        return tables;
    }

    /**
     * Downloads and returns the Childs JSON array of Inhalte from DSB (synchronously)
     *
     * @param login Login to log in with
     * @return JSON array containing the Childs of Inhalte of server's response
     * @throws LoginFailureException       If the credentials or the request in general are incorrect
     * @throws UnexpectedResponseException If response is invalid JSON
     * @throws IOException                 If request fails in general (networking error?)
     */
    private JSONArray downloadContentJSONArray(Login login) throws IOException {
        Log.d("DOWNLOAD", "downloading data");

        if (!login.isNonZeroLength()) {
            // Empty credentials are not valid
            throw new LoginFailureException();
        }

        // Make request body
        JSONObject bodyObject;
        SharedPreferences sharedPreferences = new Utility(context).getSharedPreferences();
        try {
            // Query body base json might be overwritten by news, otherwise use hardcoded value
            String queryBodyBaseJson = sharedPreferences
                    .getString("queryBodyBaseJson", context.getString(R.string.query_body_base_json));
            Log.d("DOWNLOADBASEJSON", queryBodyBaseJson);


            bodyObject = new JSONObject(queryBodyBaseJson);
            login.put(bodyObject);

            // Add things configurable through news
            boolean sendAppId = sharedPreferences.getBoolean("querySendAppId", true);
            boolean sendAndroidVersion = sharedPreferences.getBoolean("querySendAndroidVersion", true);
            boolean sendDeviceModel = sharedPreferences.getBoolean("querySendDeviceModel", true);
            boolean sendLanguage = sharedPreferences.getBoolean("querySendLanguage", true);
            boolean sendDate = sharedPreferences.getBoolean("querySendDate", false);
            boolean sendLastDate = sharedPreferences.getBoolean("querySendLastDate", false);

            // Generate AppId
            if (sendAppId) {
                bodyObject.put("AppId", DsbAppQueryMetadata.getAppId());
            }

            // Attach random android version
            if (sendAndroidVersion) {
                bodyObject.put("OsVersion", DsbAppQueryMetadata.getAndroidVersion());
            }

            // Attach random device name
            if (sendDeviceModel) {
                bodyObject.put("Device", DsbAppQueryMetadata.getDeviceModel());
            }

            // Attach some language
            if (sendLanguage) {
                bodyObject.put("Language", DsbAppQueryMetadata.getLanguage());
            }

            // Send date
            if (sendDate) {
                // Date should look like this: 2019-10-04T14:21:3728600
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSS000");
                String date = dateFormat.format(new Date());
                Log.d("DOWNLOADDATE", date);
                bodyObject.put("Date", date);

                // Send last date
                if (sendLastDate) {
                    bodyObject.put("LastUpdate", sharedPreferences.getString("queryLastDate", ""));
                    sharedPreferences.edit()
                            .putString("queryLastDate", date)
                            .apply();
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            // Shouldn't happen! Throw further as IOException since we don't really know what happened anyway
            throw new IOException(e.getCause());
        }

        // Request url
        String url = getEndpoint(sharedPreferences.getInt("queryEndpoint", 0));
        Log.d("DOWNLOADENDPOINT", url);

        // Request
        String response = string(
                request(url, obfuscateQuery(bodyObject), "POST"),
                "UTF-8"
        );

        // If request is very invalid, there won't be any json in the response, only some plain text…
        if (response.equals("Unzulässige Anforderung")) {
            Log.d("DOWNLOAD", "request failed: " + bodyObject.toString() + " obfuscated to " + obfuscateQuery(bodyObject));
            throw new UnexpectedResponseException();
        }

        try {
            JSONObject responseBody = deobfuscateResponse(response);

            // Check result code
            int resultcode = responseBody.getInt("Resultcode");
            switch (resultcode) {
                case 0:
                    // All is good, continue
                    break;
                case 1:
                    // Invalid credentials ("ResultStatusInfo": "Login fehlgeschlagen")
                    throw new LoginFailureException();
                default:
                    Log.d("DOWNLOAD", "unexpected Resultcode " + resultcode + ": " + response);
                    throw new UnexpectedResponseException();
            }

            JSONArray resultMenuItems = responseBody.getJSONArray("ResultMenuItems");
            resultMenu:
            for (int i = 0; i < resultMenuItems.length(); i++) {
                JSONObject resultMenuItem = resultMenuItems.getJSONObject(i);
                String resultMenuItemTitle = resultMenuItem.getString("Title");

                // Just to be sure that we select Inhalte, in practice it has only been observed to be the first one
                if (resultMenuItemTitle.equals("Inhalte")) {

                    return resultMenuItem.getJSONArray("Childs");
                }
            }

            // No Inhalte…?
            throw new UnexpectedResponseException("No Inhalte found");

        } catch (JSONException | EOFException e) {
            e.printStackTrace();
            // Response is invalid, throw further
            throw new UnexpectedResponseException(e.getCause());
        }
    }

    /**
     * This implementation of downloadNoticeBoardItems(Login) calls
     * {@link #downloadContentJSONArray(Login)} in order to download notices and news
     * if they have not been downloaded yet.
     */
    @Override
    public List<NoticeBoardItem> downloadNoticeBoardItems(Login login) throws IOException {
        List<NoticeBoardItem> noticeBoardItems = new ArrayList<>();
        if (notices == null && news == null) downloadTables(login);

        if (notices != null)
            noticeBoardItems.addAll(notices);
        if (news != null)
            noticeBoardItems.addAll(news);

        return noticeBoardItems;
    }

    /**
     * Return the corresponding url for the endpoint id.
     *
     * @param id 0 (mobile) / 1 (web) / 2 (ihkmobile) / 3 (appihkbb)
     */
    private String getEndpoint(int id) throws IOException {
        switch (id) {
            case 0:
            default:
                return "https://app.dsbcontrol.de/JsonHandler.ashx/GetData";
            case 1:
                String webConfiguration = string(
                        request("https://www.dsbmobile.de/scripts/configuration.js", null, "GET"),
                        "UTF-8"
                );
                return "https://www.dsbmobile.de/" + webConfiguration.split("'")[3]; //bad solution, web endpoint obfuscated -> method outdated

            case 2:
                return "https://ihkmobile.dsbcontrol.de/new/JsonHandlerWeb.ashx/GetData";
            case 3:
                return "https://appihkbb.dsbcontrol.de/new/JsonHandlerWeb.ashx/GetData";
        }
    }

    /**
     * Obfuscate query. The DSB server requires this.
     * <br/><br/>
     * Queries are "compressed" using gzip (saving less than half a kilobyte) and then encoded using base64.
     * The result of that is then again hidden inside some more JSON.
     *
     * @param query The JSON query you want to make
     * @return The body you will have to send to the server to execute the query
     */
    private String obfuscateQuery(JSONObject query) throws IOException {
        String queryString = query.toString();

        // Thanks, https://stackoverflow.com/a/6718707
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(queryString.length());
        GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
        gzip.write(queryString.getBytes());
        gzip.close();
        byte[] gzipped = outputStream.toByteArray();
        outputStream.close();

        String encoded = Base64.encodeToString(gzipped, Base64.NO_WRAP); // Line wraps are useless here!

        return context.getString(R.string.query_body_outer_json, encoded);
    }

    /**
     * Deobfuscate response. The DSB server gives obfuscated responses.
     * <br/><br/>
     * Just the reverse of {@link #obfuscateQuery(JSONObject)}, except that "some more JSON" is different for responses
     * compared to queries.
     *
     * @param response The response the server gave you
     * @return The JSON hidden inside the response
     */
    private JSONObject deobfuscateResponse(String response) throws JSONException, IOException {

        JSONObject responseObject = new JSONObject(response);
        String encoded = responseObject.getString("d");


        byte[] gzipped = Base64.decode(encoded, Base64.DEFAULT);

        // Who knows how this works… thanks again, https://stackoverflow.com/a/6718707
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(gzipped);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder stringBuilder = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            stringBuilder.append(new String(data, 0, bytesRead));
        }
        gis.close();
        is.close();

        return new JSONObject(stringBuilder.toString());
    }

}
