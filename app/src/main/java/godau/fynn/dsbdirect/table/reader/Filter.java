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

package godau.fynn.dsbdirect.table.reader;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.activity.MainActivity;
import godau.fynn.dsbdirect.model.entry.Entry;
import godau.fynn.dsbdirect.model.entry.ErrorEntry;
import godau.fynn.dsbdirect.model.entry.InfoEntry;

import java.util.*;

import static android.content.Context.MODE_PRIVATE;
import static godau.fynn.dsbdirect.model.entry.EntryField.*;

/**
 * Receives an array of entries and filters out those that do not match
 * the criteria configured in the preferences. Entries may also be
 * highlighted.
 */
public final class Filter {

    private Filter() {
    } // Filter shouldn't be initialized


    public static ArrayList<Entry> filterUserFilters(@NonNull ArrayList<Entry> entries, Context context) {
        // get sharedPreferences
        final SharedPreferences sharedPreferences = context.getSharedPreferences("default", MODE_PRIVATE);

        if (!MainActivity.filterEnabled || !sharedPreferences.getBoolean("filter", false)) {
            // Filters are disabled
            return entries;
        }

        String number = sharedPreferences.getString("number", "");
        String letter = sharedPreferences.getString("letter", "");

        String[] classStrings = new String[]{number, letter};

        String name = sharedPreferences.getString("name", "");

        String[] teacherStrings = new String[]{name};

        Set<String> courses = sharedPreferences.getStringSet("courses", new TreeSet<String>());
        String[] courseStrings = courses.toArray(new String[courses.size()]);
        String[] courseStringsWithSpaces = new String[courseStrings.length*2];

        for(int i = 0; i < courseStrings.length; i++){
            courseStringsWithSpaces[i] = courseStrings[i];
            courseStringsWithSpaces[i+courseStrings.length] = courseStrings[i].replaceAll("-", " ");
            courseStringsWithSpaces[i+courseStrings.length] = courseStringsWithSpaces[i+courseStrings.length].replaceAll("  ", " ");
        }


        boolean courseFilterActive = courses.size() > 0 && !(courseStringsWithSpaces[0].equals("") && courses.size() == 1);

        // Declare adjectives
        boolean forceDisplayInfo = sharedPreferences.getBoolean("displayGeneral", true);
        boolean classFilterActive = !letter.isEmpty() || !number.isEmpty();
        boolean teacherFilterActive = !name.isEmpty();

        // Determine whether past should be filtered
        boolean filterPast = sharedPreferences.getBoolean("filterPast", true);
        boolean filterUselessInfoActive = sharedPreferences.getBoolean("filterUselessInfo", true);



        if (!classFilterActive && !teacherFilterActive && !filterPast && !courseFilterActive) { // Let's not filter when no filter is enabled
            for (int i = entries.size(); i > 0; i--) {

                Entry e = entries.get(i - 1);

                if (e instanceof ErrorEntry) {
                    // Always display error entries
                    continue;

                }

                if (e.contains(
                        new String[]{
                                "eigenem Unt."
                        }, true, true, TYPE
                )) entries.remove(i - 1);


            }
            return entries;
        }


        for (int i = entries.size(); i > 0; i--) {

            Entry e = entries.get(i - 1);


            if (e instanceof ErrorEntry) {
                // Always display error entries
                continue;

            }

            if (filterPast) {
                // Filter past entries

                Calendar zeroOClockCalendar = Utility.zeroOClock(Calendar.getInstance());

                long today = zeroOClockCalendar.getTimeInMillis();
                if (e.getDate() != null) {
                    long entry = e.getDate().getTime();

                    // Entry is in the past if it was before today
                    boolean past = entry < today;

                    if (past) {
                        entries.remove(i - 1);

                        // Don't check other criteria as the entry is already gone
                        continue;
                    }
                }

                if (!classFilterActive && !teacherFilterActive && !courseFilterActive) {
                    // If only filtering past, don't check other criteria
                    continue;
                }

            }


            // Declare more adjectives

            boolean unterrichtsfrei = e.contains(
                    new String[]{
                            "Unterrichtsfrei", "Unterrichtsende", "Unterrichtsschluss"
                    }, true, true, INFO
            );

            boolean filterUselessInfo = e.contains(
                    new String[]{
                            "Abwesende Klassen", "Abwesende Lehrer", "Blockierte Räume", "Betroffene Klassen", "Betroffene Lehrer", "Betroffene Räume"
                    }, true, true, INFO
            );

            boolean grade_event = e.contains(
                    new String[]{
                            "Zeugnisausgabe", "Stufenversammlung", "Versammlung"
                    }, true, true, INFO
            );

            boolean classInClass = e.contains(
                    classStrings, false, false, CLASS, OLD_CLASS
            );

            boolean classInInfo = e.contains(classStrings, false, false, INFO);

            boolean teacherAppears = e.contains(teacherStrings, true, false,
                    TEACHER,
                    OLD_TEACHER,
                    INFO
            );

            boolean courseAppears = e.contains(courseStringsWithSpaces, true, true,
                    CLASS,
                    OLD_CLASS,
                    SUBJECT,
                    INFO
            );
            boolean courseAppearsNoLessonMgw = e.contains(courseStringsWithSpaces, true, true,
                    TEACHER,
                    OLD_SUBJECT,
                    INFO
            );

            boolean justType = e.contains(courseStringsWithSpaces, true, true,
                    TYPE
            );


            boolean infoOnly = e instanceof InfoEntry;

            int matchesFilters = 0;


            if (filterUselessInfo && filterUselessInfoActive) {
                matchesFilters = -10;
            }

            // first possibility: Unterrichtsfrei / Unterrichtsende / Unterrichtsschluss
            if (unterrichtsfrei) {
                // pass, everyone is interested in that
                matchesFilters++;
            }

            // second possibility: class appears
            if (

                    (classInClass // test class in affected class
                            || (classInInfo && infoOnly) // test class in info if info only
                    ) && classFilterActive

            ) {
                // pass
                matchesFilters++;
            }

            // third possibility: teacher appears
            if (teacherAppears && teacherFilterActive) {
                // pass
                matchesFilters++;
            }

            if (e.contains(
                    new String[]{
                            "eigenem Unt."
                    }, true, true, TYPE
            )) matchesFilters = -10;

            // fourth possibility: course appears
            if ((courseAppears && courseFilterActive) || (courseAppearsNoLessonMgw && courseFilterActive)) {
                // pass
                matchesFilters++;

                //Courses from other grades won't show up when class filter is active
                if((!classInClass) && classFilterActive){
                    matchesFilters--;
                }
            }

            // fifth possibility: info only
            if (infoOnly && forceDisplayInfo) {
                // pass
                matchesFilters++;
            }

            if (matchesFilters <= 0) {
                // failed to match any filters
                entries.remove(i - 1);
            } else if (matchesFilters >= 2) {
                // matched multiple filters; highlight
                e.setHighlighted();
            }

            if (grade_event) {
                e.setHighlighted();
            }

        }

        return entries;
    }

    public static ArrayList<Entry> filterToday(@NonNull ArrayList<Entry> entries) {
        for (int i = entries.size(); i > 0; i--) {
            Entry entry = entries.get(i - 1);

            Date entryDate = entry.getDate();

            if (entryDate == null) {
                continue;
            }

            Calendar entryCalendar = Calendar.getInstance();
            entryCalendar.setTime(entryDate);

            Calendar todayCalendar = Calendar.getInstance();

            int entryDay = entryCalendar.get(Calendar.DAY_OF_YEAR);
            int todayDay = todayCalendar.get(Calendar.DAY_OF_YEAR);

            int entryYear = entryCalendar.get(Calendar.YEAR);
            int todayYear = todayCalendar.get(Calendar.YEAR);

            boolean yearMatches = entryYear == todayYear;
            boolean dayMatches = entryDay == todayDay;

            boolean sameDay = yearMatches && dayMatches;

            if (!sameDay) {
                entries.remove(entry);
            }
        }

        return entries;
    }
}
