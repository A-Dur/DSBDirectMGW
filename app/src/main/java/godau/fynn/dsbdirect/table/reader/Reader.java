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

package godau.fynn.dsbdirect.table.reader;

import android.content.Context;

import androidx.annotation.NonNull;

import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.persistence.ShortcodeManager;
import godau.fynn.dsbdirect.model.entry.EntryField;
import godau.fynn.dsbdirect.model.entry.Entry;
import godau.fynn.dsbdirect.model.entry.ErrorEntry;
import godau.fynn.dsbdirect.model.entry.InfoEntry;
import godau.fynn.dsbdirect.model.noticeboard.NewsItem;
import godau.fynn.dsbdirect.model.noticeboard.Notice;
import godau.fynn.dsbdirect.model.Table;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;

import java.util.*;

import static godau.fynn.dsbdirect.model.entry.EntryField.*;

/**
 * @see <a href="https://notabug.org/fynngodau/DSBDirect/wiki/Contributing+%E2%80%93+How+to+write+a+reader">
 *      Contributing – How to write a reader</a>
 */
public abstract class Reader {
    // Reader is just used as a synonym for Parser

    /* from configuration.js:
        Dsbmobile.SPOTTYPS = {
            NONE:   0,
            FOLDER: 1,
            SYNC:   2,
            HTML:   3,
            IMG:    4,
            NEWS:   5,
            URL:    6,
            VIDEO:  7,
            PDF:    8,
        };
     */

    /**
     * Not for actual content, mustn't appear in Table objects as only their children are read out
     * in {@link godau.fynn.dsbdirect.table.reader.Reader}
     */
    private static final int CONTENT_ROOT = 0;
    /**
     * Unknown what this is good for, therefore a mystery
     */
    private static final int CONTENT_MYSTERY = 1;
    /**
     * Not for actual content, might hold further tables or notices in its children. Mustn't become an object
     */
    private static final int CONTENT_PARENT = 2;
    /**
     * HTML content
     */
    public static final int CONTENT_HTML = 3;
    /**
     * Image content
     */
    public static final int CONTENT_IMAGE = 4;
    /**
     * Text content
     */
    private static final int CONTENT_TEXT = 5;
    /**
     * Also HTML content…? We don't know the difference to {@link #CONTENT_HTML}, so it's a mystery
     */
    public static final int CONTENT_HTML_MYSTERY = 6;

    protected String html;
    private ShortcodeManager shortcodeManager = null;
    private Context context;
    /**
     * Entries are centrally managed by Reader, to which they are added with
     * one of the add…Entry methods and then returned by {@link #read()}.
     */
    private final ArrayList<Entry> entries = new ArrayList<>();

    public void setHtml(String html) {
        // Leave brs be
        html = html.replaceAll("(?i)<br ?/?>", "&lt;br&gt;");

        // Leave strikes be
        html = html.replaceAll("(?i)<s(trike)*>", "&lt;strike&gt;");
        html = html.replaceAll("(?i)</s(trike)*>", "&lt;&#47;strike&gt;");

        this.html = html;
    }

    public void setContext(Context context) {
        shortcodeManager = new ShortcodeManager(context);
        this.context = context;
    }

    /**
     * Reads the provided HTML file and returns all contained entries while removing duplicates.
     */
    public final ArrayList<Entry> read() {
        addEntries();
        return entries;
    }

    /**
     * Adds all entries contained in the {@link #html} by calling
     * {@link #addEntry(String, String, String, String, Date)} for each entry.
     */
    public abstract void addEntries();

    /**
     * @see #entries
     */
    @Deprecated
    protected final void addEntry(@Nullable String affectedClass, @Nullable String lesson, @Nullable String replacementTeacher,
                                       @Nullable String info, @Nullable Date date) {

        Entry e = new Entry.Builder(shortcodeManager)
                .put(CLASS, affectedClass)
                .put(LESSON, lesson)
                .put(TEACHER, replacementTeacher)
                .put(INFO, info)
                .setDate(date)
                .build();

        addEntry(e);
    }

    protected final void addEntry(Map<EntryField, String> fields, @Nullable Date date) {

        Entry e = new Entry(fields, date, shortcodeManager);

        addEntry(e);
    }

    protected final void addEntry(Entry e) {
        // Test whether entry already exists
        for (Entry existingEntry :
                entries) {
            if (e.equals(existingEntry)) {
                return;
            }
        }
        entries.add(e);
    }

    protected final void addInfoEntry(String info, @Nullable Date date) {

        InfoEntry e = new InfoEntry(info, date);

        // Test whether entry already exists
        for (Entry existingEntry :
                entries) {
            if (e.equals(existingEntry)) {
                return;
            }
        }

        entries.add(new InfoEntry(info, date));
    }

    protected final void addErrorEntry() {
        entries.add(new ErrorEntry(context));
    }

    protected final Entry.Builder getEntryBuilder() {
        return new Entry.Builder(shortcodeManager);
    }


    /**
     * @return The name of the school that this substitution plan belongs to, or
     * {@code null} if not available.
     */
    public abstract @Nullable String getSchoolName();

    public static Table[] readTableList(JSONArray jsonArray, @Nullable String title) throws JSONException {
        ArrayList<Table> tables = new ArrayList<>();

        parsing:
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonTable = jsonArray.getJSONObject(i);

            int contentType = jsonTable.getInt("ConType");
            // We can only handle html and images at the moment
            if (contentType == CONTENT_HTML || contentType == CONTENT_HTML_MYSTERY || contentType == CONTENT_IMAGE) {

                String url = jsonTable.getString("Detail");

                // Test whether url is duplicate
                for (Table t :
                        tables) {
                    if (t.getUri().equals(url)) {
                        continue parsing;
                    }
                }
                if (title == null) {
                    title = jsonTable.getString("Title");
                }

                Date publishedTime = Utility.parseLastUpdatedDate(jsonTable.getString("Date"));

                // Not a duplicate; add
                tables.add(new Table(url, publishedTime, contentType, title));
            }

            // Recursion! Add all "Childs" of this table to tables
            Collections.addAll(tables, readTableList(jsonTable.getJSONArray("Childs"), jsonTable.getString("Title")));

        }
        return tables.toArray(new Table[0]);
    }

    public static Table[] readTableList(JSONArray jsonArray) throws JSONException {
        return readTableList(jsonArray, null);
    }

    public static ArrayList<Notice> readNoticeList(JSONArray jsonArray) throws JSONException {
        ArrayList<Notice> notices = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject noticeJson = jsonArray.getJSONObject(i);

            // Get image URLs

            JSONArray images = noticeJson.getJSONArray("Childs");

            if (images.length() < 1) continue;

            String[] imageUrls = new String[images.length()];

            for (int j = 0; j < images.length(); j++) {
                imageUrls[j] = images.getJSONObject(j).getString("Detail");
            }

            notices.add(new Notice(noticeJson.getString("Title"),
                    Utility.parseLastUpdatedDate(noticeJson.getString("Date")),
                    imageUrls,
                    // Assuming that all notices in a category have the same ConType
                    images.getJSONObject(0).getInt("ConType"))
            );
        }

        return notices;
    }

    public static ArrayList<NewsItem> readNewsList(JSONArray jsonArray) throws JSONException {
        ArrayList<NewsItem> news = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject newsJson = jsonArray.getJSONObject(i);

            news.add(new NewsItem(newsJson.getString("Title"),
                    Utility.parseLastUpdatedDate(newsJson.getString("Date")),
                    newsJson.getString("Detail"))
            );
        }

        return news;
    }

    /**
     * Default to getting class name as id for new implementations
     */
    public String getId() {
        return getClass().getSimpleName();
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Implementations may hardcode a set of school ids to test id against.
     *
     * @param id The ID of a school which is to be parsed
     * @return Whether this id should be handled by this parser (overwrites
     * other detection mechanisms)
     */
    public boolean canParseId(String id) {
        return false;
    }

    /**
     * Implementations may do a quick test whether this html can be parsed
     * by them or not.
     *
     * @param html Html to parse
     * @return True if this parser should handle this html
     */
    public boolean canParseHtml(String html) {
        return false;
    }
}
