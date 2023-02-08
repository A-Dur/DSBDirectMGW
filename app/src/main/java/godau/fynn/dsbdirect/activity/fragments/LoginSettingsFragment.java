package godau.fynn.dsbdirect.activity.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import godau.fynn.dsbdirect.activity.SettingsActivity;
import godau.fynn.dsbdirect.model.Login;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.activity.LoginActivity;
import godau.fynn.dsbdirect.persistence.LoginManager;

import static android.app.Activity.RESULT_OK;


public class LoginSettingsFragment extends SettingsActivity.PreferenceFragment {

    private static final int REQUEST_LOGIN = 0;

    @Override
    public void setPreferences() {

        final LoginManager loginManager = new LoginManager(sharedPreferences);

        // Load correct sharedPreferences
        getPreferenceManager().setSharedPreferencesName("default");

        // Thanks, https://stackoverflow.com/a/37745292
        addPreferencesFromResource(R.xml.preferences_none);
        PreferenceScreen preferenceScreen = this.getPreferenceScreen();

        findPreference("settings_none_description")
                .setSummary(R.string.settings_login_description);

        // Add logins
        for (final Login l : loginManager.getLogins()) {
            Preference loginPreference = new Preference(getActivity());

            loginPreference.setTitle(l.getDisplayName());
            if (l.hasDisplayName()) {
                loginPreference.setSummary(l.getId());
            }

            // Prompt for new display name
            loginPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    // Spawn editText layout
                    final View promptView = LayoutInflater.from(getActivity()).inflate(R.layout.edittext, null);
                    final EditText editText = promptView.findViewById(R.id.editText);

                    if (l.hasDisplayName()) {
                        editText.setText(l.getDisplayName());
                    }
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getActivity().getString(R.string.settings_login_alias_popup, l.getId()))
                            .setMessage(R.string.settings_login_alias_popup_message)
                            .setView(promptView)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (editText.length() <= 0) {
                                        // To allow display name to be set to school name automatically
                                        l.setDisplayName(null);
                                    } else {
                                        l.setDisplayName(editText.getText().toString());
                                    }
                                    // Write changed login alias to shared preferences
                                    loginManager.write();

                                    // Reload list by recreating
                                    getActivity().recreate();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .setNeutralButton(R.string.settings_login_remove, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    loginManager.removeLogin(l);

                                    // Reload list by recreating
                                    getActivity().recreate();
                                }
                            })
                            .show();
                    return true;
                }
            });

            preferenceScreen.addPreference(loginPreference);
        }

        // Add Add login button

        Preference newLoginPreference = new Preference(getActivity());
        newLoginPreference.setTitle(R.string.action_add_login);

        // TODO: This button could have a '+' icon, but depending on the theme, it would need to be black or white.

        newLoginPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                startActivityForResult(new Intent(getActivity(), LoginActivity.class), REQUEST_LOGIN);

                // TODO: A drawback of this button is that school name can only be set as display alias when its plan is viewed with parsing enabled, not immediately when added here.

                return true;
            }
        });

        preferenceScreen.addPreference(newLoginPreference);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            getActivity().recreate();
        }
    }
}
