/*
 * DSBDirect
 * Copyright (C) 2019 Jasper Michalke <jasper.michalke@jasmich.ml>
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
import android.util.Log;
import godau.fynn.dsbdirect.model.entry.Entry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static godau.fynn.dsbdirect.model.entry.EntryField.*;


public class GoetheHamburg extends Reader {

    /**
     * Every class is just listed once even if there are several substitutions
     */
    private String lastClass = "";

    @Override
    public void addEntries() {

        Document document = Jsoup.parse(html);

        Date date = null;

        Elements divs = document.getElementsByTag("div");

        for (Element div : divs) {

            // Is this the beginning of a new day?
            if (div.hasClass("dayHeader")) {
                try {
                    String dateString = div.text().replaceAll(", \\w+", "");
                    date = new SimpleDateFormat("dd.MM.yyyy").parse(dateString);
                } catch (ParseException e) {
                    date = new Date();
                    e.printStackTrace();
                }
            } else
                // Is this a div containing a table?
                if (div.getElementsByTag("table").size() > 0) {

                    Elements classBlocks = div.select("table");

                    Elements rows = classBlocks.select("tr");

                    for (Element row :
                            rows) {
                        readRow(row, date);
                    }
                }
        }

    }

    private void readRow(Element row, Date date) {
        try {

            Entry.Builder entryBuilder = getEntryBuilder()
                    .setDate(date);

            String infoText;
            String affectedClass;
            try {
                infoText = row.select("td").get(1).text();
                affectedClass = row.select("td").get(0).text();
                lastClass = affectedClass;
            } catch (Exception e) {
                infoText = row.select("td").get(0).text();
                Log.d("Multi row", infoText);
                affectedClass = lastClass;
            }

            entryBuilder.put(CLASS, affectedClass);

            Pattern pattern = Pattern.compile("(\\d.*) Std");
            Matcher matcher = pattern.matcher(infoText);
            if (matcher.find()) entryBuilder.put(LESSON, matcher.group(1).replaceAll("\\.", ""));
            ;


            pattern = Pattern.compile("bei ([A-ZÄÖÜ]{3}, [A-ZÄÖÜ]{3}|[A-ZÄÖÜ]{3})");
            matcher = pattern.matcher(infoText);
            if (matcher.find()) entryBuilder.put(TEACHER, matcher.group(1));


            String subject = "";
            pattern = Pattern.compile("\\d.* Std. (\\w+)");
            matcher = pattern.matcher(infoText);
            if (matcher.find() && !matcher.group(1).equals("bei")) subject = matcher.group(1) + " ";


            String info = "";
            String[] regex = {"im Raum ([HVGFT]\\d{3}|ESH)", "statt bei [A-ZÄÖÜ]{3}", "\\(.*\\)", "(f.llt aus|Vtr. ohne Lehrer)"};
            for (int i = 0; i < regex.length; i++) {
                pattern = Pattern.compile(regex[i]);
                matcher = pattern.matcher(infoText);
                if (matcher.find()) info = matcher.group(0);
            }

            info = subject + info;


            if (!info.contains("im Raum")) {
                pattern = Pattern.compile("im Raum ([HVGFT]\\d{3}|ESH)");
                matcher = pattern.matcher(infoText);
                if (matcher.find()) info += " (" + matcher.group(1) + ")";
            }

            entryBuilder.put(INFO, info);

            addEntry(entryBuilder.build());
        } catch (Exception e) {
            Log.d("GOETHE", "Error in row: " + row.text());
            addInfoEntry(row.text(), date);
        }
    }

    @Nullable
    @Override
    public String getSchoolName() {
        return null;
    }

    @Override
    public String getId() {
        return "goethe";
    }

    @Override
    public String getName() {
        return "HeinekingMedia";
    }

    @Override
    public boolean canParseHtml(String html) {
        return html.contains("HeinekingMedia Vertretungsplan");
    }
}
