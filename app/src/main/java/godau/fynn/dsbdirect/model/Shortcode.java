package godau.fynn.dsbdirect.model;

import javax.annotation.Nullable;

/**
 * Stores a shortcode to be replaced with a proper teacher name
 */
public class Shortcode {

    /**
     * Short code and corresponding teacher's name
     */
    private String shortcode, teacherName;

    public Shortcode(String shortcode, String teacherName) throws IllegalArgumentException {

        // Check for forbidden characters
        checkForIllegalCharacters(shortcode);
        checkForIllegalCharacters(teacherName);

        this.shortcode = shortcode;
        this.teacherName = teacherName;
    }

    /**
     * Deserialize a serialized shortcode
     * @param serialized A pair of shortcode and teacherName separated by a → character
     */
    public Shortcode(String serialized) throws IllegalArgumentException {
        String[] split = serialized.split("→");

        // Check whether amount is correct
        if (split.length != 2) {
            throw new IllegalArgumentException("serialized string does not contain exactly one '→' char: " + serialized);
        }

        // Check for → and \n chars
        checkForIllegalCharacters(split[0]);
        checkForIllegalCharacters(split[1]);

        shortcode = split[0];
        teacherName = split[1];

    }

    /**
     * Check whether a given String contains characters that break things
     * @param s The String to be checked
     * @throws IllegalArgumentException if s contains a '→' (breaks serialization) or a newline character (breaks storage)
     */
    private void checkForIllegalCharacters(String s) {
        if (s.contains("→") || s.contains("\n")) {
            throw new IllegalArgumentException("Neither shortcode nor teacher name may contain a '→' or newline character");
        }
    }


    /**
     * Replace shortcodes with teacher names
     * @param s The String in which a shortcode might appear that has to be replaced
     * @return The String where shortcode is replaced with teacher name
     */
    public String replace(String s) {
        return s.replaceAll("\\b" + shortcode + "\\b", teacherName);
    }

    /**
     * @param s The String in which a shortcode might appear
     * @return Whether the String contains the shortcode
     */
    public boolean containedIn(String s) {
        return s.contains(shortcode);
    }

    /**
     * Serialize to "shortcode→teacherName" to store in sharedPreferences
     * @return serialized String
     */
    public String serialize() {
        return shortcode + "→" + teacherName;
    }

    /**
     * @return a String that displays this shortcode
     */
    public String getDisplayName() {
        return shortcode + " → " + teacherName;
    }

    public String getFrom() {
        return shortcode;
    }

    public String getTo() {
        return teacherName;
    }

    public void setFrom(String shortcode) {
        this.shortcode = shortcode;
    }

    public void setTo(String teacherName) {
        this.teacherName = teacherName;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Shortcode) {
            return ((Shortcode) obj).serialize().equals(serialize());
        }
        return super.equals(obj);
    }
}
