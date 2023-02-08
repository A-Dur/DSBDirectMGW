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
import godau.fynn.dsbdirect.model.Login;
import godau.fynn.dsbdirect.persistence.LoginManager;
import godau.fynn.dsbdirect.download.exception.LoginFailureException;
import godau.fynn.dsbdirect.download.exception.UnexpectedResponseException;
import godau.fynn.dsbdirect.model.noticeboard.NoticeBoardItem;
import godau.fynn.dsbdirect.model.Table;
import godau.fynn.dsbdirect.table.reader.Reader;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DsbMobileApiDownloadManager extends DownloadManager {

    private String token;

    protected DsbMobileApiDownloadManager(Context context) {
        super(context);
    }

    @Override
    public Table[] downloadTables(Login login) throws IOException {

        if (token == null) token = login.getToken();
        if (token == null) token = downloadAuthToken(login);

        Table[] tables = downloadTables(token);

        if (tables.length == 0) {
            // Token could be invalid, try again with new token
            token = downloadAuthToken(login);
            tables = downloadTables(token);
        }

        return tables;
    }

    private Table[] downloadTables(String token) throws IOException {
        String plansQueryUrl = "https://mobileapi.dsbcontrol.de/dsbtimetables"
                + '?' + "authid=" + token;
        String plansString = string(request(plansQueryUrl, null, "GET"), "UTF-8");

        try {
            return Reader.readTableList(new JSONArray(plansString));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new UnexpectedResponseException(e);
        }
    }

    @Override
    public List<NoticeBoardItem> downloadNoticeBoardItems(Login login) throws IOException {

        if (token == null) token = login.getToken();
        if (token == null) token = downloadAuthToken(login);

        String noticesQueryUrl = "https://mobileapi.dsbcontrol.de/dsbdocuments"
                + '?' + "authid=" + token;
        String noticesString = string(request(noticesQueryUrl, null, "GET"), "UTF-8");

        String newsQueryUrl = "https://mobileapi.dsbcontrol.de/newstab"
                + '?' + "authid=" + token;
        String newsString = string(request(newsQueryUrl, null, "GET"), "UTF-8");

        try {
            List<NoticeBoardItem> noticeBoardItems = new ArrayList<>();
            noticeBoardItems.addAll(Reader.readNoticeList(new JSONArray(noticesString)));
            noticeBoardItems.addAll(Reader.readNewsList(new JSONArray(newsString)));
            return noticeBoardItems;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new UnexpectedResponseException(e);
        }
    }

    private String downloadAuthToken(Login login) throws IOException {
        if (!login.isNonZeroLength()) {
            // Empty credentials are not valid
            throw new LoginFailureException();
        }

        String authUrl = "https://mobileapi.dsbcontrol.de/authid"
                + '?' + "user=" + login.getId()
                + '&' + "password=" + login.getPass()
                + '&' + "bundleid"
                + '&' + "appversion"
                + '&' + "osversion"
                + '&' + "pushid";

        String token = string(request(authUrl, null, "GET"), "UTF-8")
                .replaceAll("\"", "");

        if (token.isEmpty()) throw new LoginFailureException();
        else {
            login.setToken(token);

            new LoginManager(context).addLogin(login);

            return token;
        }
    }
}
