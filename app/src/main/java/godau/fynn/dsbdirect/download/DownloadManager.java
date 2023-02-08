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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.download.exception.LoginFailureException;
import godau.fynn.dsbdirect.download.exception.UnexpectedResponseException;
import godau.fynn.dsbdirect.model.Login;
import godau.fynn.dsbdirect.model.Table;
import godau.fynn.dsbdirect.model.noticeboard.NoticeBoardItem;
import godau.fynn.dsbdirect.persistence.FileManager;
import godau.fynn.dsbdirect.util.Utility;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;

public abstract class DownloadManager {
    protected Context context;

    protected DownloadManager(Context context) {
        this.context = context;

        try {
            Security.setUp();
        } catch (NoSuchProviderException | NoSuchAlgorithmException | KeyManagementException e) {
            // I do not know a scenario in which these exceptions would occur.
            // Let's hope they don't.
            e.printStackTrace();
        }
    }

    public static DownloadManager getDownloadManager(Context context) {

        switch (new Utility(context).getSharedPreferences().getString("endpoint", "mobileapi.dsbcontrol.de")) {
            default:
            case "mobileapi.dsbcontrol.de":
                return new DsbMobileApiDownloadManager(context);
            case "app.dsbcontrol.de":
                return new DsbAppDownloadManager(context);
        }
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
    @NonNull
    protected InputStream request(String url, @Nullable String body, String requestMethod) throws IOException {

        if (!isNetworkAvailable()) throw new IOException();

        URL connectwat = new URL(url);
        HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

        Security.setSocketFactory(urlConnection);

        urlConnection.setRequestMethod(requestMethod);

        // Add DNT header, as if it does anything
        urlConnection.addRequestProperty("DNT", "1");

        if (body != null) {

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

    /**
     * Downloads all Tables currently available from DSB (synchronously)
     *
     * @param login Login to log in with
     * @return A Table array with all contents in DSB
     * @throws LoginFailureException       If the credentials or the request in general are incorrect
     * @throws UnexpectedResponseException If response is invalid JSON
     * @throws IOException                 If request fails in general (networking error?)
     */
    public abstract Table[] downloadTables(Login login) throws IOException;

    /**
     * Downloads all notice board items (Notices and news) from DSB (synchronously)
     *
     * @param login Login to log in with
     * @return A List with all NoticeBoardItems
     * @throws IOException If request fails
     */
    public abstract List<NoticeBoardItem> downloadNoticeBoardItems(Login login) throws IOException;

    /**
     * Downloads an html table file (synchronously)
     *
     * @param table       The table which is to be downloaded
     * @param fileManager A file manager to be used to save the file
     * @return The downloaded html String
     * @throws IOException In case of networking error
     */
    public String downloadHtmlTable(final Table table, final FileManager fileManager) throws IOException { // TODO
        Log.d("DOWNLOAD", "downloading html file at " + table.getUri());

        // Request
        String html = string(request(table.getUri(), null, "GET"), "ISO-8859-1");

        // If these characters appear, the wrong encoding has been used
        if (html.contains("ï»¿")) {
            try {
                html = new String(html.getBytes("ISO-8859-1"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // So be it!
                e.printStackTrace();
            }

        }

        // Save file
        fileManager.saveFile(table, html);

        return html;
    }

    /**
     * Downloads a bitmap table file (synchronously)
     *
     * @param table       The table which is to be downloaded
     * @param fileManager A file manager to be used to save the file
     * @return The bitmap
     * @throws IOException In case of networking error
     */
    public Bitmap downloadImageTable(final Table table, final FileManager fileManager) throws IOException { // TODO
        // We're doing bitmaps.
        Log.d("DOWNLOAD", "downloading image file at " + table.getUri());

        // Request bitmap
        InputStream inputStream = request(table.getUri(), null, "GET");

        // Create bitmap
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        // Save bitmap
        fileManager.saveFile(table, bitmap);

        // Return bitmap
        return bitmap;
    }

    public JSONObject downloadNews() throws IOException {
        Log.d("DOWNLOAD", "downloading news");

        try {
            return new JSONObject(string(request(context.getString(R.string.uri_news), null, "GET"), "UTF-8"));
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e.getCause());
        }
    }

    // Thanks, https://stackoverflow.com/a/35446009
    protected String string(InputStream in, String charsetName) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result.toString(charsetName);

    }

    /**
     * Uploads a url to the server at https://dsb.bixilon.de to ask the developer to develop a parser for it.
     * <br/><br/>The server code is available at <a href="https://notabug.org/fynngodau/dsbdirect-filedump/src/master/requestParser.php">fynngodau/dsbdirect-filedump</a>.
     *
     * @param url The url to upload. Must be at <a href="https://app.dsbcontrol.de">https://app.dsbcontrol.de</a>
     * @return whether the server returned 200 Success
     * @throws IOException in case of a network error
     */
    public boolean uploadParserRequest(String url) throws IOException {
        // request(…) returns an InputStream, not the response code as it is pretty much always 200, so we can't use it here

        if (!isNetworkAvailable()) {
            throw new IOException();
        }

        URL connectwat = new URL(context.getString(R.string.uri_requestparser));
        HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

        Security.setSocketFactory(urlConnection);

        urlConnection.setRequestMethod("POST");

        OutputStream outputStream = urlConnection.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        outputStreamWriter.write("url=" + url);
        outputStreamWriter.flush();
        outputStreamWriter.close();
        outputStream.close();

        urlConnection.connect();

        if (urlConnection.getResponseCode() == 200) return true;
        else return false;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
