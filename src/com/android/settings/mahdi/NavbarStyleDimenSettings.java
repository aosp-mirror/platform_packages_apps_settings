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
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
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

import com.android.internal.util.mahdi.DeviceUtils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class NavbarStyleDimenSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG =
        "NavBarStyleDimen";
    private static final String PREF_NAVIGATION_BAR_HEIGHT =
        "navigation_bar_height";
    private static final String PREF_NAVIGATION_BAR_HEIGHT_LANDSCAPE =
        "navigation_bar_height_landscape";
    private static final String PREF_NAVIGATION_BAR_WIDTH =
        "navigation_bar_width";
    private static final String KEY_DIMEN_OPTIONS =
        "navbar_dimen";

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DLG_RESET = 0;

    ListPreference mNavigationBarHeight;
    ListPreference mNavigationBarHeightLandscape;
    ListPreference mNavigationBarWidth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.navbar_style_dimen_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mNavigationBarHeight =
            (ListPreference) findPreference(PREF_NAVIGATION_BAR_HEIGHT);
        mNavigationBarHeight.setOnPreferenceChangeListener(this);

        mNavigationBarHeightLandscape =
            (ListPreference) findPreference(PREF_NAVIGATION_BAR_HEIGHT_LANDSCAPE);
        mNavigationBarHeightLandscape.setOnPreferenceChangeListener(this);

        mNavigationBarWidth =
            (ListPreference) findPreference(PREF_NAVIGATION_BAR_WIDTH);
        if (!DeviceUtils.isPhone(getActivity())) {
            prefSet.removePreference(mNavigationBarWidth);
            mNavigationBarWidth = null;
        } else {
            mNavigationBarWidth.setOnPreferenceChangeListener(this);
        }

        updateDimensionValues();

        setHasOptionsMenu(true);
    }

    private void updateDimensionValues() {
        int navigationBarHeight = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_HEIGHT, -2);
        if (navigationBarHeight == -2) {
            navigationBarHeight = (int) (getResources().getDimension(
                    com.android.internal.R.dimen.navigation_bar_height)
                    / getResources().getDisplayMetrics().density);
        }
        mNavigationBarHeight.setValue(String.valueOf(navigationBarHeight));

        int navigationBarHeightLandscape = Settings.System.getInt(getContentResolver(),
                            Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE, -2);
        if (navigationBarHeightLandscape == -2) {
            navigationBarHeightLandscape = (int) (getResources().getDimension(
                    com.android.internal.R.dimen.navigation_bar_height_landscape)
                    / getResources().getDisplayMetrics().density);
        }
        mNavigationBarHeightLandscape.setValue(String.valueOf(navigationBarHeightLandscape));

        if (mNavigationBarWidth == null) {
            return;
        }
        int navigationBarWidth = Settings.System.getInt(getContentResolver(),
                            Settings.System.NAVIGATION_BAR_WIDTH, -2);
        if (navigationBarWidth == -2) {
            navigationBarWidth = (int) (getResources().getDimension(
                    com.android.internal.R.dimen.navigation_bar_width)
                    / getResources().getDisplayMetrics().density);
        }
        mNavigationBarWidth.setValue(String.valueOf(navigationBarWidth));

        boolean navbarCanMove = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_CAN_MOVE, 1) == 1;

        mNavigationBarHeightLandscape.setEnabled(!navbarCanMove);
        mNavigationBarWidth.setEnabled(navbarCanMove);
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
        if (preference == mNavigationBarWidth) {
            String newVal = (String) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_WIDTH,
                    Integer.parseInt(newVal));
            return true;
        } else if (preference == mNavigationBarHeight) {
            String newVal = (String) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT,
                    Integer.parseInt(newVal));
            return true;
        } else if (preference == mNavigationBarHeightLandscape) {
            String newVal = (String) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE,
                    Integer.parseInt(newVal));
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
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

        NavbarStyleDimenSettings getOwner() {
            return (NavbarStyleDimenSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.navbar_dimensions_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_HEIGHT, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_WIDTH, -2);
                            getOwner().updateDimensionValues();
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
