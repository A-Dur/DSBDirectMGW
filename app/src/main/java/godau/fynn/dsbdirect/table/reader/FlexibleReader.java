package godau.fynn.dsbdirect.table.reader;

import android.util.Log;
import godau.fynn.dsbdirect.model.entry.EntryField;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * FlexibleReader is used for implementing the concept of a master table
 * more easily. A (theoretical) master table has columns for every column
 * that could appear in a substitution plan and is then used to construct
 * an entry. Therefore, columns found in a substitution plan file are
 * assigned to their corresponding master table columns.
 *
 * @see <a href="https://notabug.org/fynngodau/DSBDirect/wiki/The+FlexibleReader+class">
 *     The FlexibleReader class</a>
 */
public abstract class FlexibleReader extends Reader {

    protected EntryField[] columns;

    /**
     * Creates a mapping of actual columns to master columns stored by
     * calling {@link #getMasterTableColumn(String)} for each column and
     * stores the result in {@link #columns}.
     */
    protected final void setMasterTableColumns(String[] definitions) {

        EntryField[] columns = new EntryField[definitions.length];

        // Get column of each master table from subclass
        for (int i = 0; i < definitions.length; i++) {
            columns[i] = getMasterTableColumn(definitions[i]);
        }

        this.columns = columns;
    }

    /**
     * Gets the text String from each Element and calls {@link #setMasterTableColumns(String[])}
     * @param definingElements Should contain one element for each column of
     *                         the table (likely {@code td} elements)
     */
    protected final void setMasterTableColumns(Elements definingElements) {
        setMasterTableColumns(stringsOfElements(definingElements));
    }

    /**
     * @return What master table column the column with this title belongs to, or,
     * in other words, a mapping of this column title to its {@code MasterTableColumn}
     */
    protected abstract EntryField getMasterTableColumn(String definition);

    /**
     * @return the array of master table column assignments
     * @deprecated This method is marked as deprecated to inform you that you probably
     * don't want to use it, as you should call {@link #constructEntry(Object[], Date)}
     * with the list of Strings as you found them in your table.
     */
    @Deprecated
    protected EntryField[] getMasterTableColumns() {
        return this.columns;
    }

    /**
     * Maps each array element to the corresponding master table column,
     * then adds this entry.
     * @param <T> The type of array that row is – if it is an Element, its
     *           text will be used; otherwise, its contents will be
     *           received using toString().
     */
    protected final <T> void constructEntry(T[] row, Date date) {

        verifyMasterTableConfiguration(row);

        Map<EntryField, String> masterRow = new EnumMap<>(EntryField.class);


        for (int i = 0; i < row.length; i++) {
            if (row[i] instanceof Element)
                masterRow.put(this.columns[i], ((Element) row[i]).text());
            else masterRow.put(this.columns[i], row[i].toString());
        }

        addEntry(masterRow, date);
    }

    /**
     * Converts {@code elements} to an array, then calls
     * {@link #constructEntry(Object[], Date)}.
     */
    protected final void constructEntry(Elements elements, Date date) {
        /* elements.toArray() return an Object[], but constructEntry(T[], Date)
         * will test whether each of the array's item is an instance of Element
         * separately.
         */

        constructEntry(elements.toArray(), date);
    }

    /**
     * Prefers to return string1, but returns string2 surrounded by strike tags if string1 is null, empty or just dashes.
     */
    public static String ratherThisThanThat(String string1, String string2) {

        final String DASHES_REGEX = "-+(?!.)"; // matches one or more "-" if nothing else follows it

        if (string1 != null && !string1.isEmpty() && !string1.matches(DASHES_REGEX)) {
            return string1;
        } else if (string2 != null) { // Don't concatenate around with null

            return "<strike>" + string2 + "</strike>";
        } else {
            return null;
        }
    }

    /**
     * @return An array of Strings with one String for the content of each element in e
     */
    protected final String[] stringsOfElements(Elements e) {
        Object[] strings = e.eachText().toArray();
        // "Cast" to String[]
        return Arrays.copyOf(strings, strings.length, String[].class);
    }

    /**
     * Prevent raw NullPointerException and ArrayIndexOutOfBoundsException and give
     * hopefully useful feedback instead.
     * @param row An array that contains elements that are part of a row that is supposed
     *            to be the same length as the master table configuration.
     */
    private void verifyMasterTableConfiguration(Object[] row) {

        if (this.columns == null) {
            throw new MasterTableColumnsNotMappedException();
        } else
            MasterTableColumnAmountMismatchException.test(row.length, this.columns.length);
    }

    private static class MasterTableColumnsNotMappedException extends NullPointerException {
        public MasterTableColumnsNotMappedException() {
            super("The master table columns aren't set yet! You must first call setMasterTableColumns(…).");
        }
    }

    private static class MasterTableColumnAmountMismatchException extends ArrayIndexOutOfBoundsException {
        public MasterTableColumnAmountMismatchException(int rowLength, int columnLength) {
            super("Your row has " + rowLength + " column" + (rowLength == 1? "" : "s") +
                    ", but your master table only has assignments for " + columnLength +
                    ". Please ensure you have last called setMasterTableColumns(…) for the " +
                    "table you are trying to add Entries for!");
        }

        static void printMismatchWarning(int rowLength, int columnLength) {
            Log.w("FLEXREADER", "Your row, which has " + rowLength +" column" +
                    (rowLength == 1? "" : "s") + ", is shorter than the array of " +
                    "assignments that was generated when you called " +
                    "setMasterTableColumns(…), which only has " + columnLength +
                    " items. Please ensure you have last called setMasterTableColumns(…) " +
                    "for the table you are trying to add Entries for!"
            );
        }

        /**
         * Deals with mismatching combinations of rowLength and columnLength by either
         * throwing an exception if an ArrayOutOfBoundsException would normally occur, or
         * by printing a warning message.
         */
        static void test(int rowLength, int columnLength) {
            if (rowLength > columnLength) {
                throw new MasterTableColumnAmountMismatchException(rowLength, columnLength);
            } else if (rowLength < columnLength) {
                printMismatchWarning(rowLength, columnLength);
            }
        }
    }
}
