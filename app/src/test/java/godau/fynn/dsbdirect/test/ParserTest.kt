package godau.fynn.dsbdirect.test

import godau.fynn.dsbdirect.table.reader.BlueWilli
import godau.fynn.dsbdirect.table.reader.ReaderFactory
import org.junit.Test

class ParserTest {

    @Test
    fun testBlueWilliCss() {
        val html = TestUtil.readDocument("/blue_willi_css.htm")

        val reader = ReaderFactory.getReader(null, "", html)

        check(reader is BlueWilli) { "Could not autodetect blue willi.css parser!" }

        reader.setHtml(html)

        TableRenderer.renderToLog(
            reader.read()
        )

        // ToDo: Check entries
    }
}
