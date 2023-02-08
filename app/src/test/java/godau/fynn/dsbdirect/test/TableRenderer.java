/*
 * DSBDirect
 * Copyright (C) 2021 Fynn Godau
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

package godau.fynn.dsbdirect.test;

import godau.fynn.dsbdirect.model.entry.Entry;
import godau.fynn.dsbdirect.model.entry.EntryField;

import java.util.ArrayList;

public class TableRenderer {

    public static void renderToLog(ArrayList<Entry> entries) {

        int[] measures = new int[EntryField.values().length];

        for (EntryField field : EntryField.values()) {

            measures[field.ordinal()] = field.name().length();

            for (Entry entry : entries) {
                if (entry.get(field) != null)
                    measures[field.ordinal()] =
                            Math.max(entry.get(field).length(), measures[field.ordinal()]);
            }

            System.out.print("| " + lengthify(field.name(), measures[field.ordinal()]) + " ");
        }

        System.out.println("|");


        for (Entry entry : entries) {

            for (EntryField field : EntryField.values()) {
                System.out.print("| " + lengthify(
                        entry.get(field) == null? "" : entry.get(field),
                        measures[field.ordinal()]) + " ");

            }
            System.out.println("|");
        }
    }

    private static String lengthify(String s, int l) {
        while (s.length() < l) {
            s += " ";
        }
        return s;
    }
}
