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
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceManager;

import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.model.entry.EntryField;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static godau.fynn.dsbdirect.model.entry.EntryField.*;

public class Untis extends FlexibleReader {

    private Document d;

    public Untis() {
    }

    @Override
    public void addEntries() {

        if (d == null) {
            d = Jsoup.parse(html);
        }

        // Tables are inside center tags
        Elements centers = d.getElementsByTag("center");


        // Every other center contains an advertisement for Untis
        for (int centerIndex = 0; centerIndex < centers.size(); centerIndex += 2) {

            Element center = centers.get(centerIndex);

            // Get which date this center is about
            String dateString = center.selectFirst("div").text();

            Date date;
            try {
                date = new SimpleDateFormat("dd.MM.yyyy").parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
                date = null;
            }

            // Get info box, if present
            Elements infoTables = center.getElementsByClass("info");
            if (infoTables.size() != 0) {
                Element infoTableBody = infoTables.first().getElementsByTag("tbody").first();

                Elements infoTableTrs = infoTableBody.getElementsByTag("tr");

                /* First tr will (probably) contain "Nachrichten zum Tag" headline, but we check it anyway because
                 * the headline is inside a th tag, not a td tag.
                 */
                for (Element tr : infoTableTrs) {

                    Elements tds = tr.getElementsByTag("td");

                    //Moved "&& !isUselessLine(tds.first().text())" to "Filter"
                    if (tds.size() > 0) {
                        // Construct an entry for this line

                        // If there are two columns: separate them with a ':'
                        addInfoEntry(
                                Utility.smartConcatenate(tds.toArray(), ": "),
                                date
                        );
                    }


                }

            }

            // Get main table
            Elements mainTables = center.getElementsByClass("mon_list"); // There should be exactly one
            if (mainTables.size() > 0) {
                Element mainTableBody = mainTables.first().getElementsByTag("tbody").first();

                if (mainTableBody == null) {
                    // No actual table yet on this page?
                    continue;
                }

                Elements mainTableTrs = mainTableBody.getElementsByTag("tr");

                /* Test whether definitions will be in the first or the second row –
                 * some schools have configured a weird header that spans two lines.
                 */
                boolean secondRowIsDefinition = mainTableTrs.size() > 1 &&
                        !(
                                (mainTableTrs.get(1).hasClass("list odd")
                                        || mainTableTrs.get(1).hasClass("list even")
                                )
                        );

                if (!secondRowIsDefinition)
                    setMasterTableColumns(mainTableTrs.first().getElementsByTag("th"));
                else {
                    // The second row has to be considered
                    Elements line0 = mainTableTrs.get(0).getElementsByTag("th");
                    Elements line1 = mainTableTrs.get(1).getElementsByTag("th");

                    String[] definitions = new String[line0.size()];
                    for (int i = 0; i < line0.size(); i++) {
                        definitions[i] = Utility.smartConcatenate(
                                new String[]{line0.get(i).text(), line1.get(i).text()}, "\n"
                                );
                    }

                    setMasterTableColumns(definitions);
                }

                // Guess the purpose of a potential inline header
                EntryField inlineHeaderColumn = guessInlineHeaderColumn(getMasterTableColumns());

                String inlineHeader = null;

                // Get every row
                row:
                for (int trIndex = (secondRowIsDefinition? 1 : 0) + 1; trIndex < mainTableTrs.size(); trIndex++) {
                    Element tr = mainTableTrs.get(trIndex);

                    Elements tds = tr.getElementsByTag("td");
                    Map<EntryField, String> masterRow = new EnumMap<>(EntryField.class);

                    // Get value from every column
                    for (int tdIndex = 0; tdIndex < tds.size(); tdIndex++) {
                        Element td = tds.get(tdIndex);



                        String s = td.text();

                        if (td.hasClass("inline_header")) {
                            // This is an inline header
                            inlineHeader = s;
                            continue row;
                        } else {

                            if (s.matches("<s(trike)*>.+</s(trike)*>\\?.*")) {
                                // See https://notabug.org/fynngodau/DSBDirect/issues/58
                                s = s.split("\\?")[1];
                            }

                            masterRow.put(getMasterTableColumns()[tdIndex], s);
                        }
                    }

                    // Copy inline header to master row
                    if (inlineHeader != null) {
                        masterRow.put(inlineHeaderColumn, inlineHeader);
                    }

                    addEntry(masterRow, date);

                }

            }


        }

    }

    @Override
    public String getSchoolName() {

        try {

            if (d == null) {
                d = Jsoup.parse(html);
            }


            Element monHead = d.getElementsByClass("mon_head").first();
            Element p = monHead.getElementsByTag("tbody").first()
                    .getElementsByTag("tr").first()
                    .getElementsByAttributeValue("align", "right").first()
                    .getElementsByTag("p").first();

            return p.textNodes().get(0).text();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected EntryField getMasterTableColumn(String definition) {

        // Handle two-line rows
        if (definition.contains("\n")) {
            /* Precedence for two-line rows:
             * 1. Separate lines with ' '
             * 2. Separate lines not at all (e.g. Entfall-Fach)
             * 3. Leave out first line
             * 4. Leave out second line
             */
            String[] definitionLines = definition.split("\n");

            // Ensure there are only two lines
            if (definitionLines.length != 2)
                Log.e("UNTISREAD", "Unexpectedly, the following column definition has " + definitionLines.length
                        + " instead of 2 lines – only the first two lines will be considered: " + definition);

            EntryField result =
                    getMasterTableColumn(definitionLines[0] + ' ' + definitionLines[1]);
            if (result == UNDEFINED) result =
                    getMasterTableColumn(definitionLines[0] + definitionLines[1]);
            if (result == UNDEFINED) result =
                    getMasterTableColumn(definitionLines[1]);
            if (result == UNDEFINED) result =
                    getMasterTableColumn(definitionLines[0]);
            return result;
        }

        switch (definition) {
            case "Klasse(n)":
            case "Klasse":
                return CLASS;
            case "Fach":
                return SUBJECT;
            case "Stunde":
            case "Std.":
            case "Std":
                return LESSON;
            case "Art":
                return TYPE;
            case "Vertreter":
            case "Lehrk.":
            case "neuer Lehrer":
            case "Wer vertritt?":
                return TEACHER;
            case "Raum":
                return ROOM;
            case "(Fach)":
            case "Entfall-Fach":
                return OLD_SUBJECT;
            case "(Lehrer)":
            case "(Lehrk.)":
            case "Entfall-Lehrer":
            case "Vertretung für":
            case "Wer wird vertreten?":
                return OLD_TEACHER;
            case "(Klasse(n))":
                return OLD_CLASS;
            case "Vertretungs-Text":
            case "Vertr.Text":
            case "Text":
            case "Bemerkungen und Hinweise":
            case "Bemerkung":
            case "Information":
                return INFO;
            case "Vertr. von":
            case "Info":
                // Apparently this represents a point in time at which the lesson was originally supposed to be held
                return OLD_TIME;
            default:
                return UNDEFINED;
        }
    }

    private EntryField guessInlineHeaderColumn(EntryField[] columns) {
        if (!contains(columns, CLASS)) return CLASS;
        else if (!contains(columns, TEACHER)) return TEACHER;
        else return UNDEFINED;
    }

    private static <T> boolean contains(T[] is, T i) {
        for (T isi :
                is) {
            if (isi == i) return true;
        }
        return false;
    }

    //This method got replaced by the filterUseLessInfo if-statement in the filter class
    private boolean isUselessLine(String string) {
        String[] uselessLines = {"Abwesende Klassen", "Abwesende Lehrer", "Blockierte Räume",
                "Betroffene Klassen", "Betroffene Lehrer", "Betroffene Räume"};

        for (String useless :
                uselessLines) {
            if (string.contains(useless)) {
                // If it contains something useless, this line is useless
                return true;
            }
        }

        // It didn't contain anything useless and thus is not useless
        return false;
    }


    //trying to get Checkbox .isChecked() from preferences.xml
    //SharedPreferences settings = PreferenceManager.
    //private boolean uselessLineFilterActive =

    @Override
    public String getId() {
        return "untis";
    }

    @Override
    public boolean canParseHtml(String html) {
        return html.contains("Untis Stundenplan Software");
    }
}
