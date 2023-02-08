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

import java.util.Date;
import java.util.EnumMap;

/**
 * Many substitution plan systems can generate a separate table with extra announcements.
 * <br/><br/>
 * Naturally, they have to be read separately from the rest of the table. To account for
 * this, they are implemented as a subclass of TableEntry.
 */

public class InfoEntry extends Entry {

    public InfoEntry(String info, Date date) {
        super(new EnumMap<EntryField, String>(EntryField.class), date, null);

        fields.put(EntryField.INFO, info);
    }

    public String getInfo() {
        return get(EntryField.INFO);
    }
}
