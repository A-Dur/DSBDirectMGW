package godau.fynn.dsbdirect.table.reader

import godau.fynn.dsbdirect.model.entry.EntryField
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

class BlueWilli : FlexibleReader() {

    override fun addEntries() {
        val document = Jsoup.parse(super.html)


        var currentDate: Date? = null
        var currentClassElement: Element? = null
        var rowspan = 0

        for (element in document.select("a[name=oben],h2:containsOwn(VERTRETUNGEN),table")) {
            // Get date

            if (element.tag().name == "a") {
                currentDate = null
                continue
            }
            if (element.tag().name == "h2") {
                val dateString = DATE_REGEX.find(element.text())?.groups?.get(1)?.value ?: continue
                currentDate = DATE_FORMAT.parse(dateString)
                continue
            }

            check(currentDate != null) { "Found data without date!" }

            val tables = document.getElementsByTag("table")


            tables.first()?.let {
                setMasterTableColumns(it.getElementsByTag("tr").first()!!.getElementsByTag("th"))

                super.columns = with(super.columns.toMutableList()) {
                    this += EntryField.INFO
                    toTypedArray()
                }
                val masterTableSize = masterTableColumns.size

                // Skip first row as it contains the definitions
                for (tr in it.select("tr")) {
                    val tds = tr.select("th,td")
                    var tdArray = tds.toArray()
                    if (masterTableSize == tds.size + 1) {
                        if (rowspan-- <= 0) {
                            error("No rowspan but master table does not fit!")
                        }
                        // add class to rowspan
                        tdArray = with(tdArray.toMutableList()) {
                            add(0, currentClassElement)
                            toTypedArray()
                        }
                    } else {
                        // ToDo: Get dynamic, this is static with the class
                        currentClassElement = tds.first()
                        rowspan = currentClassElement?.attributes()?.get("rowspan")?.toIntOrNull()?.dec() ?: 0 // decrement by one, because we already add one row
                    }

                    constructEntry(tdArray, currentDate)
                }
            }

            tables.getOrNull(1)?.getElementsByTag("th")?.let {
                for (info in it) {
                    addInfoEntry(info.text(), currentDate)
                }
            }
        }
    }

    override fun getSchoolName(): String? {
        return null
    }

    override fun getId(): String {
        return "bluewilli"
    }

    override fun canParseHtml(html: String?): Boolean {
        return html?.contains("willi.css") == true
    }

    override fun getMasterTableColumn(definition: String?): EntryField {
        return when (definition) {
            "Klasse" -> EntryField.CLASS
            "Lkr." -> EntryField.OLD_TEACHER
            "Std." -> EntryField.LESSON
            "vertreten durch" -> EntryField.TEACHER
            "Raum" -> EntryField.ROOM
            "" -> EntryField.INFO
            else -> EntryField.UNDEFINED
        }
    }

    companion object {
        private val DATE_REGEX = "VERTRETUNGEN für \\w+, (\\d{1,2}\\.\\d{1,2}\\.\\d{4})".toRegex()
        private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy")
    }
}
