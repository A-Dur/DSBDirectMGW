package godau.fynn.dsbdirect.activity.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import de.bixilon.elternportal.ElternPortal;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.activity.SettingsActivity;
import godau.fynn.dsbdirect.download.DownloadManager;
import godau.fynn.dsbdirect.model.Shortcode;
import godau.fynn.dsbdirect.persistence.ShortcodeManager;
import godau.fynn.dsbdirect.view.PreferenceCategory;

import java.io.IOException;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class ShortcodeSettingsFragment extends SettingsActivity.PreferenceFragment {

    @Override
    public void setPreferences() {

        final ShortcodeManager shortcodeManager = new ShortcodeManager(getActivity());

        // Load correct sharedPreferences
        getPreferenceManager().setSharedPreferencesName("default");

        // Thanks, https://stackoverflow.com/a/37745292
        addPreferencesFromResource(R.xml.preferences_shortcodes);
        PreferenceScreen preferenceScreen = this.getPreferenceScreen();

        // Download shortcodes from eltern-portal.org
        findPreference("settings_shortcodes_elternportal").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // spawn layout
                View promptView = LayoutInflater.from(getActivity()).inflate(R.layout.action_shortcodes_elternportal, null);


                // find fields
                final EditText inputURL = promptView.findViewById(R.id.input_url);
                final EditText inputEmail = promptView.findViewById(R.id.input_email);
                final EditText inputPassword = promptView.findViewById(R.id.input_password);
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.settings_shortcodes_elternportal_popup_title)
                        .setMessage(R.string.settings_shortcodes_elternportal_popup_message)
                        .setCancelable(false)
                        .setNegativeButton(R.string.cancel, null)
                        .setView(promptView)
                        .setPositiveButton(R.string.import_, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                //show loader
                                final ProgressDialog progressDialog = ProgressDialog.show(
                                        getActivity(), null,
                                        getActivity().getString(R.string.request_parser_uploading_message),
                                        true
                                );
                                new Thread(() -> {
                                    try {

                                        DownloadManager downloadManager = DownloadManager.getDownloadManager(getActivity());
                                        if (!downloadManager.isNetworkAvailable()) {
                                            throw new IOException();
                                        }
                                        ElternPortal elternPortal = ElternPortal.Companion.createByURL(inputURL.getText().toString());
                                        elternPortal.login(inputEmail.getText().toString(), inputPassword.getText().toString());
                                        Map<String, String> shortcodesList = elternPortal.fetchSchoolInfo().getShortcodes();

                                        Shortcode[] shortcodes = new Shortcode[shortcodesList.size()];

                                        int index = 0;
                                        for (Map.Entry<String, String> entry : shortcodesList.entrySet()) {
                                            shortcodes[index++] = new Shortcode(entry.getKey(), entry.getValue());
                                        }
                                        new ShortcodeManager(getActivity()).write(shortcodes);


                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                progressDialog.dismiss();

                                                new AlertDialog.Builder(getActivity())
                                                        .setTitle(R.string.settings_shortcodes_elternportal_popup_success_title)
                                                        .setMessage(R.string.settings_shortcodes_elternportal_popup_success_message)
                                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog1, int which1) {
                                                                getActivity().recreate();
                                                            }
                                                        })
                                                        .setCancelable(false) // setOnDismissListener is API 17+ only
                                                        .show();
                                            }
                                        });

                                    } catch (IllegalArgumentException e) {
                                        e.printStackTrace();
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog.dismiss();

                                                new AlertDialog.Builder(getActivity())
                                                        .setTitle(R.string.settings_shortcodes_elternportal_popop_fail_url)
                                                        .setMessage(R.string.settings_shortcodes_elternportal_popop_fail_url_message)
                                                        .setPositiveButton(R.string.ok, null)
                                                        .show();
                                            }
                                        });
                                    } catch (IOException e) {
                                        e.printStackTrace();

                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog.dismiss();

                                                new AlertDialog.Builder(getActivity())
                                                        .setTitle(R.string.network_generic_error)
                                                        .setMessage(R.string.settings_shortcodes_elternportal_popup_fail_network)
                                                        .setPositiveButton(R.string.ok, null)
                                                        .show();
                                            }
                                        });
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog.dismiss();

                                                new AlertDialog.Builder(getActivity())
                                                        .setTitle(R.string.settings_shortcodes_elternportal_popup_fail_title)
                                                        .setMessage(e.getClass().getName() + ": " + e.getMessage())
                                                        .setPositiveButton(R.string.ok, null)
                                                        .show();
                                            }
                                        });
                                    }
                                }).start();

                            }
                        })
                        .show();

                return true;
            }
        });

        // Add shortcodes
        final PreferenceCategory categoryEdit = (PreferenceCategory) findPreference("shortcodes");

        for (final Shortcode s : shortcodeManager.read()) {
            final Preference shortcodePreference = new Preference(getActivity());

            shortcodePreference.setTitle(s.getDisplayName());

            // Prompt for new display name
            shortcodePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    // Spawn editText layout
                    final View promptView = LayoutInflater.from(getActivity()).inflate(R.layout.action_edit_shortcode, null);
                    final EditText editText = promptView.findViewById(R.id.editText);
                    final EditText editText1 = promptView.findViewById(R.id.editText1);

                    editText.setText(s.getFrom());
                    editText1.setText(s.getTo());

                    new AlertDialog.Builder(getActivity())
                            .setTitle(getActivity().getString(R.string.settings_shortcodes_edit_popup))
                            .setView(promptView)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    // Write changed shortcode to shared preferences

                                    s.setFrom(editText.getText().toString());
                                    s.setTo(editText1.getText().toString());

                                    shortcodeManager.write();

                                    shortcodePreference.setTitle(s.getDisplayName());
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .setNeutralButton(R.string.settings_shortcodes_edit_popup_remove, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    shortcodeManager.removeShortcode(s);
                                    categoryEdit.removePreference(shortcodePreference);
                                }
                            })
                            .show();
                    return true;
                }
            });

            categoryEdit.addPreference(shortcodePreference);
        }

        // Add Add shortcode button

        Preference newLoginPreference = new Preference(getActivity());
        newLoginPreference.setTitle(R.string.settings_shortcodes_add);

        // TODO: This button could have a '+' icon, but depending on the theme, it would need to be black or white.

        newLoginPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                // Spawn editText layout
                final View promptView = LayoutInflater.from(getActivity()).inflate(R.layout.action_edit_shortcode, null);
                final EditText editText = promptView.findViewById(R.id.editText);
                final EditText editText1 = promptView.findViewById(R.id.editText1);

                new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getString(R.string.settings_shortcodes_add_popup))
                        .setView(promptView)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // Write new shortcode to shared preferences
                                try {
                                    Shortcode s = new Shortcode(editText.getText().toString(), editText1.getText().toString());

                                    if (shortcodeManager.addShortcode(s)) {
                                        // Reload list by recreating fragment
                                        getFragmentManager().beginTransaction()
                                                .replace(android.R.id.content, new ShortcodeSettingsFragment())
                                                .commit();
                                    } else {
                                        new AlertDialog.Builder(getActivity())
                                                .setTitle(R.string.settings_shortcodes_edit_popup_failure)
                                                .setMessage(R.string.settings_shortcodes_edit_popup_failure_needs_from)
                                                .setPositiveButton(R.string.ok, null)
                                                .show();
                                    }

                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                    new AlertDialog.Builder(getActivity())
                                            .setTitle(R.string.settings_shortcodes_edit_popup_failure)
                                            .setMessage(R.string.settings_shortcodes_edit_popup_failure_illegal_character)
                                            .setPositiveButton(R.string.ok, null)
                                            .show();
                                }

                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
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
