package godau.fynn.dsbdirect.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.activity.fragments.MainSettingsFragment;
import godau.fynn.dsbdirect.util.Utility;

public class SettingsActivity extends FragmentActivity {

    public static final String EXTRA_CONTAINS_HTML = "htmlTable";
    public static final String EXTRA_HTML_ONLY = "htmlOnlyTable";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Utility(SettingsActivity.this).stylize();

        if (savedInstanceState == null)
            // Display the fragment as the main content
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new MainSettingsFragment())
                    .commit();

    }

    @Override
    public void onBackPressed() {
        // Possibly trigger recreate
        setResult(RESULT_OK);

        super.onBackPressed();
    }

    public static abstract class PreferenceFragment extends PreferenceFragmentCompat {

        protected Utility u;
        protected SharedPreferences sharedPreferences;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            // Load correct sharedPreferences
            getPreferenceManager().setSharedPreferencesName("default");

            u = new Utility(getActivity());
            sharedPreferences = u.getSharedPreferences();

            setPreferences();

            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
                Preference preference = getPreferenceScreen().getPreference(i);
                preference.setIconSpaceReserved(false);

                if (preference instanceof PreferenceGroup) {
                    for (int j = 0; j < ((PreferenceGroup) preference).getPreferenceCount(); j++) {
                        Preference preference1 = ((PreferenceGroup) preference).getPreference(j);
                        preference1.setIconSpaceReserved(false);
                    }
                }
            }
        }

        protected abstract void setPreferences();

        protected void openFragment(PreferenceFragment fragment) {
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left,
                            R.anim.slide_in_right, R.anim.slide_out_right)
                    .replace(android.R.id.content, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

}
