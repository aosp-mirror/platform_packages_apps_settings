/*
 * Copyright (C) 2012 Slimroms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.mahdi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NavBarButtonStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "NavBarButtonStyle";
    private static final String PREF_NAV_BUTTON_COLOR = "nav_button_color";
    private static final String PREF_NAV_BUTTON_COLOR_MODE = "nav_button_color_mode";
    private static final String PREF_NAV_GLOW_COLOR = "nav_button_glow_color";

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DLG_RESET = 0;

    private boolean mCheckPreferences;

    ColorPickerPreference mNavigationBarButtonColor;
    ListPreference mNavigationBarButtonColorMode;
    ColorPickerPreference mNavigationBarGlowColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    private PreferenceScreen refreshSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.navbar_button_style);

        prefs = getPreferenceScreen();

        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return null;
        }

        mNavigationBarButtonColor = (ColorPickerPreference) findPreference(PREF_NAV_BUTTON_COLOR);
        mNavigationBarButtonColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_TINT, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                "com.android.systemui:color/navigationbar_button_default_color", null, null));
            mNavigationBarButtonColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mNavigationBarButtonColor.setSummary(hexColor);
        }
        mNavigationBarButtonColor.setNewPreviewColor(intColor);

        mNavigationBarGlowColor = (ColorPickerPreference) findPreference(PREF_NAV_GLOW_COLOR);
        mNavigationBarGlowColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_GLOW_TINT, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                "com.android.systemui:color/navigationbar_button_glow_default_color", null, null));
            mNavigationBarGlowColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mNavigationBarGlowColor.setSummary(hexColor);
        }
        mNavigationBarGlowColor.setNewPreviewColor(intColor);

        mNavigationBarButtonColorMode =
            (ListPreference) prefs.findPreference(PREF_NAV_BUTTON_COLOR_MODE);
        int navigationBarButtonColorMode = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_BUTTON_TINT_MODE, 0);
        mNavigationBarButtonColorMode.setValue(String.valueOf(navigationBarButtonColorMode));
        mNavigationBarButtonColorMode.setSummary(mNavigationBarButtonColorMode.getEntry());
        mNavigationBarButtonColorMode.setOnPreferenceChangeListener(this);

        updateColorPreference();

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefs;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mNavigationBarButtonColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_TINT, intHex);
            return true;
        } else if (preference == mNavigationBarButtonColorMode) {
            int index = mNavigationBarButtonColorMode.findIndexOfValue((String) newValue);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_TINT_MODE,
                    value);
            mNavigationBarButtonColorMode.setSummary(
                mNavigationBarButtonColorMode.getEntries()[index]);
            updateColorPreference();
            return true;
        } if (preference == mNavigationBarGlowColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_GLOW_TINT, intHex);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void updateColorPreference() {
        int navigationBarButtonColorMode = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_BUTTON_TINT_MODE, 0);
        mNavigationBarButtonColor.setEnabled(navigationBarButtonColorMode != 3);
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        NavBarButtonStyle getOwner() {
            return (NavBarButtonStyle) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_BUTTON_TINT, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                   Settings.System.NAVIGATION_BAR_BUTTON_TINT_MODE, 0);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_GLOW_TINT, -2);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

}
