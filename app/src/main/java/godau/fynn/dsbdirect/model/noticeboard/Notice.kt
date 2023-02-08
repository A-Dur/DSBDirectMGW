/*
* Notice for DSBDirect
* Copyright (C) 2019 Jasper Michalke <jasper.michalke@jasmich.ml>
* Created by Jasper Michalke <jasper.michalke@jasmich.de> under license EUPL 1.2.
 *
 * This software is not affiliated with heinekingmedia GmbH, the
 * developer of the DSB platform.
*/

package godau.fynn.dsbdirect.model.noticeboard

import godau.fynn.dsbdirect.table.reader.Reader
import java.io.Serializable
import java.util.*

class Notice(title: String?, date: Date?, var data: Array<String>, var conType: Int) : NoticeBoardItem(title, date), Serializable {

    fun isImage(): Boolean {
        return conType == Reader.CONTENT_IMAGE
    }
}