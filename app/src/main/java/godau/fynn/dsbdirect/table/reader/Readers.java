package godau.fynn.dsbdirect.table.reader;

public abstract class Readers {

    /**
     * @return A list of all readers that can be used.
     */
    public static Reader[] getReaders() {
        return new Reader[]{
                new Untis(),
                new GoetheHamburg(),
                new WhiteYellow(),
                new BlueWilli(),

                // Add new readers above this line
        };
    }
}
