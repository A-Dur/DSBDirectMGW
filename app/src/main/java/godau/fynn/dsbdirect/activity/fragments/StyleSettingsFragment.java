/*
 * DSBDirect
 * Copyright (C) 2019 Fynn Godau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with heinekingmedia GmbH, the
 * developer of the DSB platform.
 */

package godau.fynn.dsbdirect.activity.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.Preference;
import androidx.annotation.ColorInt;
import android.view.LayoutInflater;
import android.view.View;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.activity.SettingsActivity;
import godau.fynn.dsbdirect.util.Utility;
import uz.shift.colorpicker.LineColorPicker;
import uz.shift.colorpicker.OnColorChangedListener;

import static godau.fynn.dsbdirect.activity.SettingsActivity.EXTRA_HTML_ONLY;

public class StyleSettingsFragment extends SettingsActivity.PreferenceFragment {

    // 500
    // Thanks, https://stackoverflow.com/a/33261150
    private static final int RED_500 = 0xFFF44336;
    private static final int PINK_500 = 0xFFE91E63;
    private static final int PURPLE_500 = 0xFF9C27B0;
    private static final int DEEP_PURPLE_500 = 0xFF673AB7;
    private static final int INDIGO_500 = 0xFF3F51B5;
    private static final int BLUE_500 = 0xFF2196F3;
    private static final int LIGHT_BLUE_500 = 0xFF03A9F4;
    private static final int CYAN_500 = 0xFF00BCD4;
    private static final int TEAL_500 = 0xFF009688;
    private static final int GREEN_500 = 0xFF4CAF50;
    private static final int LIGHT_GREEN_500 = 0xFF8BC34A;
    private static final int LIME_500 = 0xFFCDDC39;
    private static final int YELLOW_500 = 0xFFFFEB3B;
    private static final int AMBER_500 = 0xFFFFC107;
    private static final int ORANGE_500 = 0xFFFF9800;
    private static final int DEEP_ORANGE_500 = 0xFFFF5722;
    private static final int BROWN_500 = 0xFF795548;
    private static final int GREY_500 = 0xFF9E9E9E;
    private static final int BLUE_GREY_500 = 0xFF607D8B;

    // Black
    private static final int BLACK = 0xFF000000;

    // 700
    private static final int RED_700 = 0xFFD32F2F;
    private static final int PINK_700 = 0xFFC2185B;
    private static final int PURPLE_700 = 0xFF7B1FA2;
    private static final int DEEP_PURPLE_700 = 0xFF512DA8;
    private static final int INDIGO_700 = 0xFF303F9F;
    private static final int BLUE_700 = 0xFF1976D2;
    private static final int LIGHT_BLUE_700 = 0xFF0288D1;
    private static final int CYAN_700 = 0xFF0097A7;
    private static final int TEAL_700 = 0xFF00796B;
    private static final int GREEN_700 = 0xFF388E3C;
    private static final int LIGHT_GREEN_700 = 0xFF689F38;
    private static final int LIME_700 = 0xFFAFB42B;
    private static final int YELLOW_700 = 0xFFFBC02D;
    private static final int AMBER_700 = 0xFFFFA000;
    private static final int ORANGE_700 = 0xFFF57C00;
    private static final int DEEP_ORANGE_700 = 0xFFE64A19;
    private static final int BROWN_700 = 0xFF5D4037;
    private static final int GREY_700 = 0xFF616161;
    private static final int BLUE_GREY_700 = 0xFF455A64;

    // A200
    private static final int RED_A200 = 0xFFFF5252;
    private static final int PINK_A200 = 0xFFFF4081;
    private static final int PURPLE_A200 = 0xFFE040FB;
    private static final int DEEP_PURPLE_A200 = 0xFF7C4DFF;
    private static final int INDIGO_A200 = 0xFF536DFE;
    private static final int BLUE_A200 = 0xFF448AFF;
    private static final int LIGHT_BLUE_A200 = 0xFF40C4FF;
    private static final int CYAN_A200 = 0xFF18FFFF;
    private static final int TEAL_A200 = 0xFF64FFDA;
    private static final int GREEN_A200 = 0xFF69F0AE;
    private static final int LIGHT_GREEN_A200 = 0xFFB2FF59;
    private static final int LIME_A200 = 0xFFEEFF41;
    private static final int YELLOW_A200 = 0xFFFFFF00;
    private static final int AMBER_A200 = 0xFFFFD740;
    private static final int ORANGE_A200 = 0xFFFFAB40;
    private static final int DEEP_ORANGE_A200 = 0xFFFF6E40;


    private static final int[] COLOR_PALETTE = {
            RED_500, PINK_500, PURPLE_500, DEEP_PURPLE_500, INDIGO_500, BLUE_500, LIGHT_BLUE_500, CYAN_500, TEAL_500,
            GREEN_500, LIGHT_GREEN_500, LIME_500, YELLOW_500, AMBER_500, ORANGE_500, DEEP_ORANGE_500, BROWN_500,
            GREY_500, BLUE_GREY_500, BLACK
    };

    private static final int[] COLOR_PALETTE_DARK = {
            RED_700, PINK_700, PURPLE_700, DEEP_PURPLE_700, INDIGO_700, BLUE_700, LIGHT_BLUE_700, CYAN_700, TEAL_700,
            GREEN_700, LIGHT_GREEN_700, LIME_700, YELLOW_700, AMBER_700, ORANGE_700, DEEP_ORANGE_700, BROWN_700,
            GREY_700, BLUE_GREY_700, BLACK
    };

    private static final int[] COLOR_PALETTE_ACCENT = {
            RED_A200, PINK_A200, PURPLE_A200, DEEP_PURPLE_A200, INDIGO_A200, BLUE_A200, LIGHT_BLUE_A200, CYAN_A200, TEAL_A200,
            GREEN_A200, LIGHT_GREEN_A200, LIME_A200, YELLOW_A200, AMBER_A200, ORANGE_A200, DEEP_ORANGE_A200
    };

    @Override
    public void setPreferences() {

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_styling);

        final boolean html = getActivity().getIntent().getBooleanExtra(EXTRA_HTML_ONLY, true);
        if (html) {
            // There are no images to be inverted
            Preference invertImages = findPreference("invertImages");
            invertImages.setEnabled(false);
            invertImages.setSummary(R.string.settings_view_invert_images_no_images);
        }

            // Reload settings to change Theme when Theme preference is changed
        findPreference("style").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getActivity().recreate();
                return true;
            }
        });

        // Let user choose Primary color when tapping Primary color
        findPreference("colorPrimary").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setColorPopup(getActivity(), COLOR_PALETTE, u.getColorPrimary(), "colorPrimary"); // TODO summary

                return true;
            }
        });

        // Let user choose Primary color dark after clicking Primary color dark
        findPreference("colorPrimaryDark").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setColorPopup(getActivity(), COLOR_PALETTE_DARK, u.getColorPrimaryDark(), "colorPrimaryDark"); // TODO summary

                return true;
            }
        });

        // Let user choose Accent color on touch of Accent color
        findPreference("colorAccent").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setColorPopup(getActivity(), COLOR_PALETTE_ACCENT, u.getColorAccent(), "colorAccent"); // TODO summary

                return true;
            }
        });

    }

    public static void setColorPopup(Context context, int[] colorPalette, @ColorInt int selectedColor, final String preferenceName) {

        // spawn layout
        View promptView = LayoutInflater.from(context).inflate(R.layout.action_pick_color, null);

        // get sharedPreferences
        Utility u = new Utility(context);
        final SharedPreferences sharedPreferences1 = u.getSharedPreferences();

        // find fields
        final LineColorPicker colorPicker = promptView.findViewById(R.id.picker);

        colorPicker.setColors(colorPalette);



        // select currently selected color
        colorPicker.setSelectedColor(selectedColor);

        final AlertDialog al = new AlertDialog.Builder(context)
                .setView(promptView)
                .setTitle(R.string.action_pick_color_popup)
                .setMessage(R.string.action_pick_color_popup_message)
                .show();

        // Dismiss on color pick
        colorPicker.setOnColorChangedListener(new OnColorChangedListener() {
            @Override
            public void onColorChanged(int i) {
                sharedPreferences1.edit().putInt(preferenceName, i)
                        .apply();

                al.cancel();
            }
        });
    }
}
