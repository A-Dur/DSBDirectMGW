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

package godau.fynn.dsbdirect.model;

import java.io.Serializable;
import java.util.Date;

import static godau.fynn.dsbdirect.table.reader.Reader.*;

public class Table implements Serializable {
    private String uri;
    private Date publishedTime;
    private int contentType;
    private String title;

    public Table(String uri, Date publishedTime, int contentType, String title) {
        this.uri = uri;
        this.publishedTime = publishedTime;
        this.contentType = contentType;
        this.title = title;
    }

    public Table(String uri, Date publishedTime, boolean isHtml, String title) {
        this.uri = uri;
        this.publishedTime = publishedTime;
        if (isHtml) {
            contentType = CONTENT_HTML;
        } else {
            contentType = CONTENT_IMAGE;
        }
        this.title = title;
    }

    public Date getPublishedDate() {
        return publishedTime;
    }

    public String getUri() {
        return uri;
    }

    public boolean isHtml() {
        return contentType == CONTENT_HTML || contentType == CONTENT_HTML_MYSTERY;
    }

    /**
     * Don't use this method to verify if a content is HTML, use {@link #isHtml()} for that
     * @return The content type – either CONTENT_HTML, CONTENT_HTML_MYSTERY or CONTENT_IMAGE.
     */
    public int getContentType() {
        return contentType;
    }

    public String getTitle() {
        return title;
    }
}
