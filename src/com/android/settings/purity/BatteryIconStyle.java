/*
 * Copyright (C) 2012 Slimroms Project
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

package com.android.settings.purity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.Date;

public class BatteryIconStyle extends SettingsPreferenceFragment
    implements OnPreferenceChangeListener {

    private static final String TAG = "BatteryIconStyle";

    private static final String PREF_STATUS_BAR_BATTERY = "battery_icon";
    private static final String PREF_STATUS_BAR_BATTERY_COLOR = "battery_color";
    private static final String PREF_STATUS_BAR_BATTERY_TEXT_COLOR = "battery_text_color";
    private static final String PREF_STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR = "battery_text_charging_color";
    private static final String PREF_STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED = "circle_battery_animation_speed";


    private static final int MENU_RESET = Menu.FIRST;

    private ListPreference mStatusBarBattery;
    private ColorPickerPreference mBatteryColor;
    private ColorPickerPreference mBatteryTextColor;
    private ColorPickerPreference mBatteryTextChargingColor;
    private ListPreference mCircleAnimSpeed;

    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        mCheckPreferences = false;
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        addPreferencesFromResource(R.xml.purity_battery_style);
        prefSet = getPreferenceScreen();

        int intColor;
        String hexColor;

        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return null;
        }

        mStatusBarBattery = (ListPreference) prefSet.findPreference(PREF_STATUS_BAR_BATTERY);
        mStatusBarBattery.setOnPreferenceChangeListener(this);
        int statusBarBattery = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY, 0);
        mStatusBarBattery.setValue(String.valueOf(statusBarBattery));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());

        mBatteryColor = (ColorPickerPreference) findPreference(PREF_STATUS_BAR_BATTERY_COLOR);
        mBatteryColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_COLOR, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/batterymeter_charge_color", null, null));
            mBatteryColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mBatteryColor.setSummary(hexColor);
        }
        mBatteryColor.setNewPreviewColor(intColor);

        mBatteryTextColor =
            (ColorPickerPreference) findPreference(PREF_STATUS_BAR_BATTERY_TEXT_COLOR);
        mBatteryTextColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR, -2);
        mBatteryTextColor.setSummary(getResources().getString(R.string.default_string));
        if (intColor == -2 && statusBarBattery == 2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/batterymeter_bolt_color", null, null));
        } else if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/batterymeter_charge_color", null, null));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mBatteryTextColor.setSummary(hexColor);
        }
        mBatteryTextColor.setNewPreviewColor(intColor);

        mBatteryTextChargingColor = (ColorPickerPreference)
            findPreference(PREF_STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR);
        mBatteryTextChargingColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR, -2);
        mBatteryTextChargingColor.setSummary(getResources().getString(R.string.default_string));
        if (intColor == -2 && (statusBarBattery == 2 || statusBarBattery == 0)) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/batterymeter_bolt_color", null, null));
        } else if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/batterymeter_charge_color", null, null));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mBatteryTextChargingColor.setSummary(hexColor);
        }
        mBatteryTextChargingColor.setNewPreviewColor(intColor);

        mCircleAnimSpeed =
            (ListPreference) findPreference(PREF_STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED);
        mCircleAnimSpeed.setOnPreferenceChangeListener(this);
        mCircleAnimSpeed.setValue((Settings.System
                .getInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED, 3))
                + "");
        mCircleAnimSpeed.setSummary(mCircleAnimSpeed.getEntry());

        updateBatteryIconOptions(statusBarBattery);

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefSet;
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
                resetToDefault();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.reset);
        alertDialog.setMessage(R.string.battery_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                iconColorReset();
                createCustomView();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mStatusBarBattery) {
            int statusBarBattery = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, statusBarBattery);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            createCustomView();
            return true;
        } else if (preference == mBatteryColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_COLOR, intHex);
            return true;
        } else if (preference == mBatteryTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR, intHex);
            return true;
        } else if (preference == mBatteryTextChargingColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR, intHex);
            return true;
        } else if (preference == mCircleAnimSpeed) {
            int val = Integer.parseInt((String) newValue);
            int index = mCircleAnimSpeed.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED, val);
            mCircleAnimSpeed.setSummary(mCircleAnimSpeed.getEntries()[index]);
            return true;
        }
        return false;
    }

    private void iconColorReset() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_COLOR, -2);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_TEXT_COLOR, -2);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING_COLOR, -2);
    }

    private void updateBatteryIconOptions(int batteryIconStat) {
        mBatteryTextChargingColor.setTitle(R.string.battery_text_charging_color);
        if (batteryIconStat == 0) {
            mBatteryColor.setEnabled(true);
            mBatteryTextColor.setEnabled(false);
            mBatteryTextChargingColor.setEnabled(true);
            mBatteryTextChargingColor.setTitle(R.string.battery_bolt_color);
            mCircleAnimSpeed.setEnabled(false);
        } else if (batteryIconStat == 2) {
            mBatteryColor.setEnabled(true);
            mBatteryTextColor.setEnabled(true);
            mBatteryTextChargingColor.setEnabled(true);
            mCircleAnimSpeed.setEnabled(false);
        } else if (batteryIconStat == 3 || batteryIconStat == 5) {
            mBatteryColor.setEnabled(true);
            mBatteryTextColor.setEnabled(false);
            mBatteryTextChargingColor.setEnabled(false);
            mCircleAnimSpeed.setEnabled(true);
        } else if (batteryIconStat == 4 || batteryIconStat == 6) {
            mBatteryColor.setEnabled(true);
            mBatteryTextColor.setEnabled(true);
            mBatteryTextChargingColor.setEnabled(true);
            mCircleAnimSpeed.setEnabled(true);
        } else {
            mBatteryColor.setEnabled(false);
            mBatteryTextColor.setEnabled(true);
            mBatteryTextChargingColor.setEnabled(true);
            mCircleAnimSpeed.setEnabled(false);
        }
    }

}
