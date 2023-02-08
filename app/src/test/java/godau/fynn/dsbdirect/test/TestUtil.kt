package godau.fynn.dsbdirect.test

import java.io.BufferedReader
import java.io.InputStreamReader

object TestUtil {
    val LINE_SEPARATOR: String = System.getProperty("line.separator")!!

    fun readReader(reader: BufferedReader): String {
        val stringBuilder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
            stringBuilder.append(LINE_SEPARATOR)
        }
        stringBuilder.deleteCharAt(stringBuilder.length - 1)
        reader.close()
        return stringBuilder.toString()
    }

    fun readDocument(name: String): String {
        return readReader(BufferedReader(InputStreamReader(TestUtil::class.java.getResourceAsStream(name))))
    }
}
