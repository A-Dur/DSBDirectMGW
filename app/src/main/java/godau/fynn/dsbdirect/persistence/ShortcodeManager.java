package godau.fynn.dsbdirect.persistence;

import android.content.Context;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.model.Shortcode;

import java.util.Arrays;

public class ShortcodeManager {

    private final Context context;
    private Shortcode[] shortcodes;

    public ShortcodeManager(Context context) {
        this.context = context;
    }

    public Shortcode[] read() {

        String[] shortcodeStrings;
        String raw = new Utility(context).getSharedPreferences().getString("shortcodes", "");
        if(raw.isEmpty())
            shortcodeStrings = new String[] {};
        else
            shortcodeStrings = raw.split("\n");

        Shortcode[] shortcodes = new Shortcode[shortcodeStrings.length];

        for (int i = 0; i < shortcodeStrings.length; i++) {
            try {
                shortcodes[i] = new Shortcode(shortcodeStrings[i]);
            } catch (IllegalArgumentException e) {
                // Better not be null
                shortcodes[i] = new Shortcode("", "");
                e.printStackTrace();
            }
        }

        this.shortcodes = shortcodes;

        return shortcodes;
    }

    public void write(Shortcode[] shortcodes) {
        this.shortcodes = shortcodes;
        write();
    }

    public void write() {
        String[] shortcodeStrings = new String[shortcodes.length];
        StringBuilder shortcodesRaw = new StringBuilder();

        for (int i = 0; i < shortcodes.length; i++) {
            shortcodeStrings[i] = shortcodes[i].serialize().replace("\n", "");
        }
        Arrays.sort(shortcodeStrings); //sort alphabetical
        for (int i = 0; i < shortcodes.length; i++) {
            shortcodesRaw.append(shortcodeStrings[i]);
            if(i != shortcodes.length - 1)
                shortcodesRaw.append("\n");
        }

        new Utility(context).getSharedPreferences()
                .edit()
                .putString("shortcodes", shortcodesRaw.toString())
                .apply();

    }

    /**
     * @param shortcode The shortcode to remove
     * @return Whether the shortcode was successfully removed
     */
    public boolean removeShortcode(Shortcode shortcode) {

        // Don't remove if not contained
        boolean contained = false;
        for (Shortcode s :
                shortcodes) {
            if (s.equals(shortcode)) contained = true;
        }
        if (!contained) return false;


        // Create new shortcodes array with a size smaller than the old one by 1
        Shortcode[] shortcodes = new Shortcode[this.shortcodes.length - 1];

        int added = 0; // Track the next unused index in new shortcodes array; incremented when accessed

        for (int i = 0; i < this.shortcodes.length; i++) {
            // Don't add the login to be removed again
            if (!this.shortcodes[i].equals(shortcode)) {
                shortcodes[added++] = this.shortcodes[i];
            }
        }

        // Save new logins array
        write(shortcodes);

        return true;
    }

    /**
     * @param shortcode The shortcode to store
     * @return Whether the login was added
     */
    public boolean addShortcode(Shortcode shortcode) {

        // Don't save logins that are lacking either from or to
        if (shortcode.getFrom().isEmpty() || shortcode.getTo().isEmpty()) {
            return false;
        }

        // Create new shortcodes array with a size larger than the old one by 1
        shortcodes = Arrays.copyOf(shortcodes, shortcodes.length + 1);
        // Save new shortcode in new space
        shortcodes[shortcodes.length - 1] = shortcode;

        write();

        return true;
    }

    public String replace(String s) {
        if(!new Utility(context).getSharedPreferences().getBoolean("shortcodes_enabled", false))
            return s;
        if (shortcodes == null) {
            read();
        }
        for (Shortcode shortcode :
                shortcodes) {
            if (shortcode.containedIn(s)) {
                s = shortcode.replace(s);
                break;
            }
        }
        return s;
    }
}
