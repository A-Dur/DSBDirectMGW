package godau.fynn.dsbdirect.model;

import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class Login {

    private String id, pass;
    @Nullable private String displayName, token;

    public Login(String id, String pass) {
        this.id = id;
        this.pass = pass;
    }

    public Login(String id, String pass, String displayName) {
        this.id = id;
        this.pass = pass;
        this.displayName = displayName;
    }

    /**
     * Deserialize a serialized login in this format: {@code "display name" id / pass}
     * @param serialized A String in the above format
     */
    public Login(String serialized) {

        if (serialized.startsWith("\"")) {
            String[] split = serialized.split("\"");
            displayName = split[1];
            serialized = serialized.replace("\"" + displayName + "\" ", "");
        }

        String[] split = serialized.split(" / ");

        // Check whether amount is valid
        if (split.length < 2) {
            throw new IllegalArgumentException("serialized string does not contain at least one \" / \" divider");
        }

        String[] tokenSplit = split[split.length - 1].split(" \\(");

        split[split.length - 1] = tokenSplit[0];

        if (tokenSplit.length == 2) {
            token = tokenSplit[1].split("\\)")[0];
        }

        pass = "";

        for (String s :
                split) {
            if (id == null) id = s;
            else pass = pass + s;
        }

    }

    /**
     * @return The name that this login should be displayed as
     */
    public String getDisplayName() {
        if (displayName != null) {
            return displayName;
        } else {
            return id;
        }
    }

    /**
     * @return Whether {@link #displayName} is not null
     */
    public boolean hasDisplayName() {
        return displayName != null;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void updatePass(Login login) {
        pass = login.pass;
        token = login.token;
    }

    /**
     * @return {@code true} if {@link #id} and {@link #pass} both have a length larger than 0, otherwise {@code false}
     */
    public boolean isNonZeroLength() {
        return id.length() > 0 && pass.length() > 0;
    }

    /**
     * Inserts credentials into query json body
     * @param body Body to insert credentials into
     */
    public void put(JSONObject body) throws JSONException {
        body.put("UserId", id)
                .put("UserPw", pass);

    }

    public String getPass() {
        return pass;
    }

    @Nullable
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Serialize this login to a String
     * @return The login serialized to the format {@code "displayName" id / pass} or {@code id / pass}
     */
    public String serialize() {

        StringBuilder builder = new StringBuilder();

        if (displayName != null)
            builder.append("\"").append(displayName).append("\" ");

        builder.append(id)
                .append(" / ")
                .append(pass);

        if (token != null)
            builder.append(" (")
            .append(token)
            .append(")");

        return builder.toString();

    }

    /**
     * @return {@code true} if object is a login with the same id, otherwise {@code super.equals(obj)}.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Login) {
            return ((Login) obj).getId().equals(id);
        } else {
            return super.equals(obj);
        }
    }
}
