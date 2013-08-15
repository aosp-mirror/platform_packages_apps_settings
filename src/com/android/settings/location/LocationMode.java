/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.location;

import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.android.settings.R;

/**
 * A page with 3 radio buttons to choose the location mode.
 *
 * There are 3 location modes when location access is enabled:
 *
 * High accuracy: use both GPS and network location.
 *
 * Battery saving: use network location only to reduce the power consumption.
 *
 * Sensors only: use GPS location only.
 */
public class LocationMode extends LocationSettingsBase
        implements RadioButtonPreference.OnClickListener {
    private static final String KEY_HIGH_ACCURACY = "high_accuracy";
    private RadioButtonPreference mHighAccuracy;
    private static final String KEY_BATTERY_SAVING = "battery_saving";
    private RadioButtonPreference mBatterySaving;
    private static final String KEY_SENSORS_ONLY = "sensors_only";
    private RadioButtonPreference mSensorsOnly;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        createPreferenceHierarchy();
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_mode);
        root = getPreferenceScreen();

        mHighAccuracy = (RadioButtonPreference) root.findPreference(KEY_HIGH_ACCURACY);
        mBatterySaving = (RadioButtonPreference) root.findPreference(KEY_BATTERY_SAVING);
        mSensorsOnly = (RadioButtonPreference) root.findPreference(KEY_SENSORS_ONLY);
        mHighAccuracy.setOnClickListener(this);
        mBatterySaving.setOnClickListener(this);
        mSensorsOnly.setOnClickListener(this);

        refreshLocationMode();
        return root;
    }

    private void updateRadioButtons(RadioButtonPreference activated) {
        if (activated == mHighAccuracy) {
            mHighAccuracy.setChecked(true);
            mBatterySaving.setChecked(false);
            mSensorsOnly.setChecked(false);
        } else if (activated == mBatterySaving) {
            mHighAccuracy.setChecked(false);
            mBatterySaving.setChecked(true);
            mSensorsOnly.setChecked(false);
        } else if (activated == mSensorsOnly) {
            mHighAccuracy.setChecked(false);
            mBatterySaving.setChecked(false);
            mSensorsOnly.setChecked(true);
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference emiter) {
        int mode = LocationSettingsBase.MODE_LOCATION_OFF;
        if (emiter == mHighAccuracy) {
            mode = LocationSettingsBase.MODE_HIGH_ACCURACY;
        } else if (emiter == mBatterySaving) {
            mode = LocationSettingsBase.MODE_BATTERY_SAVING;
        } else if (emiter == mSensorsOnly) {
            mode = LocationSettingsBase.MODE_SENSORS_ONLY;
        }
        setLocationMode(mode);
    }

    @Override
    public void onModeChanged(int mode) {
        switch (mode) {
            case MODE_LOCATION_OFF:
                Intent intent = new Intent();
                PreferenceActivity pa = (PreferenceActivity) getActivity();
                pa.finishPreferencePanel(LocationMode.this, Activity.RESULT_OK, intent);
                break;
            case MODE_SENSORS_ONLY:
                updateRadioButtons(mSensorsOnly);
                break;
            case MODE_BATTERY_SAVING:
                updateRadioButtons(mBatterySaving);
                break;
            case MODE_HIGH_ACCURACY:
                updateRadioButtons(mHighAccuracy);
                break;
            default:
                break;
        }
        boolean enabled = (mode != MODE_LOCATION_OFF);
        mHighAccuracy.setEnabled(enabled);
        mBatterySaving.setEnabled(enabled);
        mSensorsOnly.setEnabled(enabled);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }
}
