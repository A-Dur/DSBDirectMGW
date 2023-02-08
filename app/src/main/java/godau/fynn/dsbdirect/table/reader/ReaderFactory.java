package godau.fynn.dsbdirect.table.reader;

import android.content.Context;
import androidx.annotation.Nullable;
import godau.fynn.dsbdirect.util.Utility;

public abstract class ReaderFactory {

    /**
     * Get parser for this html by id and user preference.
     *
     * @param html The html the parser is for
     * @param id   Auth id
     * @return A parser set by the user or figured out using id in {@link String getParserIdentifier(id)} or
     * identified through checking the html file for ads.
     */
    @Nullable
    public static Reader getReader(String html, String id, Context context) {

        // User preference overwrite hardcoded preference overwrite dynamic decision
        Reader reader = getReader(getParserUserSetting(context), id, html);

        if (reader != null) {
            reader.setHtml(html);
            reader.setContext(context);
        }

        return reader;
    }

    /**
     * Get user preference or, if user has no preference, decide based on hardcoded ids or autodetect ads.
     *
     * @param userSetting User setting
     * @param html        Html for automatic detection
     * @param id          id to base decision on if user has not configured a preference
     * @return The string name of the parser
     */
    public static Reader getReader(String userSetting, String id, String html) {

        Reader[] readers = Readers.getReaders();

        // First priority: user setting
        for (Reader reader : readers) {
            if (reader.getId().equals(userSetting)) {
                return reader;
            }
        }

        // Second priority: a hardcoded set of school ids which can be parsed
        for (Reader reader : readers) {
            if (reader.canParseId(id)) {
                return reader;
            }
        }

        // Third priority: try to recognize substitution plan from html, e.g. by an ad it contains
        for (Reader reader : readers) {
            if (reader.canParseHtml(html)) {
                return reader;
            }
        }

        // Nothing found? Then we can't parse this, sorry.
        return null;
    }

    /**
     * @return The parser option configured in user's shared preferences
     */
    private static String getParserUserSetting(Context context) {
        return new Utility(context).getSharedPreferences().getString("parser", "automatic");
    }
}
