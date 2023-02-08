package godau.fynn.dsbdirect.table.reader;

import android.content.Context;
import godau.fynn.dsbdirect.model.entry.EntryField;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static godau.fynn.dsbdirect.model.entry.EntryField.*;

public class WhiteYellow extends FlexibleReader {

    public void addEntries() {
        Document d = Jsoup.parse(super.html);

        // Get date
        String titleString = d.getElementsByClass("TextUeberschrift").first().text();
        // "Vertretungsplan für Tag, DD.MM.YYYY (WocheX)"
        titleString = titleString.split(" ")[3];

        Date date;
        try {
            date = new SimpleDateFormat("dd.MM.yyyy").parse(titleString);
        } catch (ParseException e) {
            e.printStackTrace();
            date = null;
        }

        // Get info box – there is optionally a TabelleMitteilung in front of the main table
        Elements optionalTabelleMitteilung = d.getElementsByClass("TabelleMitteilung");

        if (optionalTabelleMitteilung.size() > 0) {
            Element tabelleMitteilung = optionalTabelleMitteilung.first();

            Elements trs = tabelleMitteilung.getElementsByTag("tr");

            for (Element tr :
                    trs) {
                addInfoEntry(tr.text(), date);
            }
        }


        Element tabelleVertretungen = d.getElementsByClass("TabelleVertretungen").first();

        // Get definitions

        Element titelZeile = tabelleVertretungen.getElementsByClass("TitelZeileTabelleVertretungen").first();
        setMasterTableColumns(titelZeile.getElementsByTag("td"));

        // Get data from table

        Elements trs = tabelleVertretungen.getElementsByTag("tr");

        // Skip first row as it contains the definitions
        for (int i = 1; i < trs.size(); i++) {
            Element tr = trs.get(i);

            Elements tds = tr.getElementsByTag("td");

            constructEntry(tds, date);
        }
    }

    @Nullable
    @Override
    public String getSchoolName() {
        return null;
    }

    protected EntryField getMasterTableColumn(String definition) {

        switch (definition) {
            case "Klasse":
                return CLASS;
            case "Std":
                return LESSON;
            case "Fach":
                return SUBJECT;
            case "Lehrer":
            case "Vertretung":
                return TEACHER;
            case "Raum":
                return ROOM;
            case "Sonstiges":
                return INFO;
            default:
                return UNDEFINED;
        }
    }

    @Override
    public String getId() {
        return "whiteyellow";
    }

    @Override
    public boolean canParseId(String id) {
        return id.matches("177841");
    }
}
