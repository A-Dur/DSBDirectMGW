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

package godau.fynn.dsbdirect.model.entry;

import android.text.Html;
import android.util.Log;
import androidx.annotation.Nullable;

import godau.fynn.dsbdirect.activity.MainActivity;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.persistence.ShortcodeManager;
import godau.fynn.dsbdirect.table.reader.FlexibleReader;
import godau.fynn.dsbdirect.view.adapter.Adapter;
import okhttp3.internal.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import static godau.fynn.dsbdirect.model.entry.EntryField.*;
import static godau.fynn.dsbdirect.view.adapter.Adapter.LAYOUT_CARDS;

public class Entry {

    protected Date date;

    protected Map<EntryField, String> fields;

    private boolean highlighted = false;

    private Adapter adapter;

    private CompatEntry compatEntry;

    public Entry(Map<EntryField, String> fields, @Nullable Date date, @Nullable ShortcodeManager shortcodeManager) {
        this.date = date;
        this.fields = fields;
        compatEntry = new CompatEntry(shortcodeManager);
    }

    public String get(EntryField column) {
        return fields.get(column);
    }

    /**
     * Tests whether this Entry contains the given string in a certain way.
     *
     * @param strings    Strings that might be contained in this Entry.
     * @param ignoreCase Only some comparisons can be case-sensitive.
     *                   For details see <a href="https://notabug.org/fynngodau/DSBDirect/pulls/19">#19</a>
     * @param or         If true, an or condition is applied instead of an and condition.
     * @param columns    Parts of entry to be checked for strings
     * @return Whether the String contains all or, if or is true, one of the things. If no strings are given, false
     * is returned.
     */
    public boolean contains(String[] strings, boolean ignoreCase, boolean or, EntryField... columns) {

        // Get parts of entry to be checked against

        ArrayList<String> entryPartsList = new ArrayList<>();

        for (EntryField column :
                columns) {

            String part = get(column);

            if (part != null) {
                /* Bad to have strikethrough tags while filtering
                 * ("<strike>6d</strike>" would contain an 'e')
                 */
                part = part.replaceAll("</?s(trike)*>", "~");
                entryPartsList.add(part);
            }
        }

        String[] entryParts = entryPartsList.toArray(new String[0]);

        if (ignoreCase) {

            for (int i = 0; i < entryParts.length; i++) {
                entryParts[i] = entryParts[i].toLowerCase();
            }

            for (int i = 0; i < strings.length; i++) {
                strings[i] = strings[i].toLowerCase();
            }
        }


        for (String part : entryParts) {
            /* If or is false, okay will be true at first and set to false once a string is reached that entryParts doesn't contain.
             * If or is true, okay will be false at first and set to true once a thing is reached that entryParts does contain.
             * For a more detailed explanation (of an older version of this method), see #33
             */

            boolean okay = !or;
            for (String string : strings) {
                if (part.contains(string) == or) {
                    okay = or;
                    break;
                }
            }

            if (okay) {
                return true;
            }

        }

        // None of the parts contain all (or one of) the things
        return false;
    }

    @Deprecated
    public CompatEntry getCompatEntry() {
        return compatEntry;
    }

    public String getShareText(Utility u) {
        String text = Utility.smartConcatenate(new String[]{
                u.formatDate(date),
                compatEntry.getAffectedClass(),
                compatEntry.getLesson(),
                compatEntry.getReplacementTeacher(),
                compatEntry.getInfo()
        }, " · ");

        // There might be some html in there that needs to go away
        text = text.replaceAll("<br>", "\n");
        text = text.replaceAll("</*strike>", "~");

        return text;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Entry) {

            Map<EntryField, String> compareMap = (((Entry) obj).fields);

            return fields.equals(compareMap);

        } else return false;
    }

    public void setHighlighted() {
        highlighted = true;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public final @Nullable
    Date getDate() {
        return date;
    }

    public static class Builder {

        private Map<EntryField, String> fields;
        private Date date;
        private @Nullable
        ShortcodeManager shortcodeManager;

        public Builder(@Nullable ShortcodeManager shortcodeManager) {
            fields = new EnumMap<>(EntryField.class);
            this.shortcodeManager = shortcodeManager;
        }

        public Builder put(EntryField column, String string) {

            if (string != null && string.isEmpty()) string = null;

            fields.put(column, string);
            return this;
        }

        public Builder setDate(Date date) {
            this.date = date;
            return this;
        }

        public Entry build() {
            if (date == null) Log.w("ENTRY", "Date not set! Don't forget to call setDate(Date)");
            return new Entry(fields, date, shortcodeManager);
        }
    }

    @Deprecated
    public class CompatEntry {

        private @Nullable
        ShortcodeManager shortcodeManager;

        public CompatEntry(@Nullable ShortcodeManager shortcodeManager) {
            this.shortcodeManager = shortcodeManager;
        }

        public String getAffectedClass() {

            String classString = FlexibleReader.ratherThisThanThat(fields.get(CLASS), fields.get(OLD_CLASS));
            String subject = FlexibleReader.ratherThisThanThat(fields.get(SUBJECT), fields.get(OLD_SUBJECT));

            if (fields.get(TYPE).equals("eigenem Unt.")) return Utility.smartConcatenate(new String[]{"", subject}, " · ");
            return Utility.smartConcatenate(new String[]{classString, subject}, " · ");
        }

        public String getLesson() {
            String lesson = fields.get(LESSON);

            if (lesson == null) return "";
            else return lesson.replaceAll(" ", "");
        }

        public String getSubject() {
            String subject = fields.get(SUBJECT);

            if (subject == null) return "";
            else return subject.replaceAll(" ", "-");
        }

        public String getOldSubject() {
            String oldSubject = fields.get(OLD_SUBJECT);

            if (oldSubject == null) return "";
            else return oldSubject.replaceAll(" ", "-");
        }

        public String getReplacementTeacher() {
            String teacher = FlexibleReader.ratherThisThanThat(fields.get(TEACHER), fields.get(OLD_TEACHER));
            if (teacher == null) return "";
            if (fields.get(TYPE).equals("Klausur") && teacher.equals("???")) return "Klausur: Beaufs. noch unklar";
            if (fields.get(TYPE).equals("Klausur")) return "Klausur: Beaufsichtigt von " + teacher;
            if (teacher.equals("+")) return "Kein Vertreter";
            if (teacher.equals("???")) return "Vertretung noch unklar";
            if (shortcodeManager != null) teacher = shortcodeManager.replace(teacher);
            return teacher;
        }

        public String getReplacementTeacherReversedCards() {
            String teacher = FlexibleReader.ratherThisThanThat(fields.get(TEACHER), fields.get(OLD_TEACHER));
            if (teacher == null) return "";
            if (fields.get(TYPE).equals("Klausur")) return "Klausur · " + teacher;
            if (teacher.equals("+")) return "Kein Vertreter";
            if (teacher.equals("???")) return "Unbekannt";
            if (shortcodeManager != null) teacher = shortcodeManager.replace(teacher);

            return teacher;
        }

        //if subject dropped, keep off multiple "Entfall" entries
        public String getEntryType() {
            String entryType = fields.get(TYPE);

            if (entryType.equals("Entfall") && fields.get(INFO).equals("Entfall wegen Klausur")) return "";
            if (entryType.equals("Klausur")) return "";
            if (entryType.equals("Vertretung statt")) return "Vertretung statt eigenem Unterricht";
            if (entryType.equals("eigenem Unt.")) return "";
            if (entryType == null) return "";
            return entryType;
        }
        public String getEntryTypeTeacher() {
            String entryType = fields.get(TYPE);

            if (entryType == null) return "";
            else return entryType;
        }

        public String getRoom() {
            String room = fields.get(ROOM);

            if (room == null) return "";
            else if (fields.get(TYPE).equals("Entfall") && room.equals("---")) return "";
            else if (room.equals("???") && (fields.get(TYPE).equals("Vertretung") || fields.get(TYPE).equals("Raum-Vtr."))) return "Raum noch unklar";
            else return room;
        }

        public String getInfo() {
            return Utility.smartConcatenate(new String[]{
                    getCompatEntry().getEntryType(), fields.get(OLD_TIME),
                    getCompatEntry().getRoom(), fields.get(INFO)
            }, " · ");
        }

        public Date getDate() {
            return date;
        }
    }
}
