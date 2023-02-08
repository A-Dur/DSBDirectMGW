package godau.fynn.dsbdirect.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import godau.fynn.dsbdirect.model.Login;
import godau.fynn.dsbdirect.util.Utility;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class LoginManager {

    private final SharedPreferences sharedPreferences;

    private Login[] logins;

    public LoginManager(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        logins = read();
    }

    public LoginManager(Utility u) {
        sharedPreferences = u.getSharedPreferences();
        logins = read();
    }

    public LoginManager(Context context) {
        sharedPreferences = new Utility(context).getSharedPreferences();

        logins = read();
    }


    /**
     * @param login The login to store
     * @return Whether the login was added
     */
    public boolean addLogin(Login login) {

        // Don't save logins that are lacking either id or pass
        if (!login.isNonZeroLength()) {
            return false;
        }

        // Detect duplicate logins
        for (Login l :
                logins) {
            if (l.getId().equals(login.getId())) {
                // Update login password
                l.updatePass(login);
                write();

                // Don't save this login separately as it is already added
                return false;
            }
        }

        // Create new logins array with a size larger than the old one by 1
        logins = Arrays.copyOf(logins, logins.length + 1);
        // Save new login in new space
        logins[logins.length - 1] = login;

        write();

        // Let most recently added login be the active one
        setActiveLogin(login);

        return true;
    }

    /**
     * @param login The login to remove
     * @return Whether the login was successfully removed
     */
    public boolean removeLogin(Login login) {

        // Don't bother if this login is not present here
        if (!contains(login)) {
            return false;
        }

        // Create new logins array with a size smaller than the old one by 1
        Login[] logins = new Login[this.logins.length - 1];

        int added = 0; // Track the next unused index in new logins array; incremented when accessed

        for (int i = 0; i < getLoginCount(); i++) {
            // Don't add the login to be removed again
            if (!this.logins[i].equals(login)) {
                logins[added++] = this.logins[i];
            }
        }

        // Save new logins array
        this.logins = logins;

        write();

        return true;
    }

    /**
     * @param login The login to be searched for
     * @return Whether {@link #logins} contains {@code login}
     */
    private boolean contains(Login login) {
        for (Login l :
                logins) {
            if (l.equals(login)) return true;
        }
        return false;
    }

    public Login[] read() {

        Set<String> loginSet = sharedPreferences.getStringSet("logins", new TreeSet<String>());
        String[] loginStrings = loginSet.toArray(new String[loginSet.size()]);

        Login[] logins = new Login[loginSet.size()];

        for (int i = 0; i < loginSet.size(); i++) {
            try {
                logins[i] = new Login(loginStrings[i]);
            } catch (IllegalArgumentException e) {
                // Better not be null
                // TODO logins[i] = new Login("", "");
                e.printStackTrace();
            }
        }

        this.logins = logins;

        return logins;
    }

    /**
     * Serialize logins and write them to shared preferences
     */
    public void write() {
        String[] loginStrings = new String[logins.length];

        for (int i = 0; i < loginStrings.length; i++) {
            loginStrings[i] = logins[i].serialize();
        }

        sharedPreferences
                .edit()
                .putStringSet("logins", new TreeSet<>(Arrays.asList(loginStrings)))
                .apply();

    }

    /**
     * Get login that is chosen at the moment
     * @return The login, or null if none is present
     */
    public Login getActiveLogin() {
        String activeLoginId = getActiveLoginId();

        for (Login l :
                logins) {
            if (l.getId().equals(activeLoginId)) {
                return l;
            }
        }

        // No active login found, let the first one in the array be the active one
        setActiveLogin(logins[0]);
        return logins[0];
    }

    private void setActiveLogin(String id) {
        sharedPreferences.edit().putString("id", id).apply();
    }

    /**
     * Read id of active login from shared preferences
     */
    private String getActiveLoginId() {
        return sharedPreferences
                .getString("id", "");
    }

    /**
     * @return Whether a valid login is present that can be used to log in
     */
    public boolean canLogin() {
        return logins.length > 0 && logins[0].isNonZeroLength();
    }

    public int getLoginCount() {
        return logins.length;
    }

    /**
     * @return An array of all currently not active logins
     */
    public Login[] getInactiveLogins() {
        Login[] inactiveLogins = new Login[logins.length - 1];
        String activeLogin = getActiveLoginId();
        int index = 0; // Index of next not filled slot in inactive logins array (incremented while accessing)
        for (Login l :
                logins) {
            if (!l.getId().equals(activeLogin)) {
                inactiveLogins[index++] = l;
            }
        }

        return inactiveLogins;
    }

    public Login[] getLogins() {
        return logins;
    }

    public void setActiveLogin(Login l) {
        setActiveLogin(l.getId());
    }
}
