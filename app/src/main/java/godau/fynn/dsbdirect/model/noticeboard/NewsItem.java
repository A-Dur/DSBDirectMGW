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

package godau.fynn.dsbdirect.model.noticeboard;


import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Date;

public class NewsItem extends NoticeBoardItem implements Serializable {

    private String message;

    public NewsItem(@Nullable String title, Date date, String message) {
        super(title, date);

        if (message.endsWith("\n"))
            message = message.substring(0, message.length() - 1);

        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
