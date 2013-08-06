/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.R;

/**
 * Location access settings.
 */
public class LocationSettings extends LocationSettingsBase
        implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = LocationSettings.class.getSimpleName();
    private static final String KEY_LOCATION_MODE = "location_mode";
    private static final String KEY_RECENT_LOCATION_REQUESTS = "recent_location_requests";
    private static final String KEY_LOCATION_SERVICES = "location_services";

    private Switch mSwitch;
    private boolean mValidListener;
    private PreferenceScreen mLocationMode;
    private PreferenceCategory mRecentLocationRequests;
    private PreferenceCategory mLocationServices;

    public LocationSettings() {
        mValidListener = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        createPreferenceHierarchy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitch = new Switch(getActivity());
        mSwitch.setOnCheckedChangeListener(this);
        mValidListener = true;
        createPreferenceHierarchy();
    }

    @Override
    public void onPause() {
        super.onPause();
        mValidListener = false;
        mSwitch.setOnCheckedChangeListener(null);
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_settings);
        root = getPreferenceScreen();

        mLocationMode = (PreferenceScreen) root.findPreference(KEY_LOCATION_MODE);
        mLocationMode.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        PreferenceActivity preferenceActivity =
                                (PreferenceActivity) getActivity();
                        preferenceActivity.startPreferencePanel(
                                LocationMode.class.getName(), null,
                                R.string.location_mode_screen_title, null, LocationSettings.this,
                                0);
                        return true;
                    }
                });
        mRecentLocationRequests =
                (PreferenceCategory) root.findPreference(KEY_RECENT_LOCATION_REQUESTS);
        mLocationServices = (PreferenceCategory) root.findPreference(KEY_LOCATION_SERVICES);

        Activity activity = getActivity();

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            // Only show the master switch when we're not in multi-pane mode, and not being used as
            // Setup Wizard.
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                mSwitch.setPaddingRelative(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(mSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));
            }
        } else {
            Log.wtf(TAG, "Current activity is not an instance of PreferenceActivity!");
        }

        setHasOptionsMenu(true);

        refreshLocationMode();
        return root;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }

    @Override
    public void onModeChanged(int mode) {
        switch (mode) {
            case MODE_LOCATION_OFF:
                mLocationMode.setSummary(R.string.location_mode_location_off_title);
                break;
            case MODE_SENSORS_ONLY:
                mLocationMode.setSummary(R.string.location_mode_sensors_only_title);
                break;
            case MODE_BATTERY_SAVING:
                mLocationMode.setSummary(R.string.location_mode_battery_saving_title);
                break;
            case MODE_HIGH_ACCURACY:
                mLocationMode.setSummary(R.string.location_mode_high_accuracy_title);
                break;
            default:
                break;
        }

        boolean enabled = (mode != MODE_LOCATION_OFF);
        mLocationMode.setEnabled(enabled);
        mRecentLocationRequests.setEnabled(enabled);
        mLocationServices.setEnabled(enabled);

        if (enabled != mSwitch.isChecked()) {
            // set listener to null so that that code below doesn't trigger onCheckedChanged()
            if (mValidListener) {
                mSwitch.setOnCheckedChangeListener(null);
            }
            mSwitch.setChecked(enabled);
            if (mValidListener) {
                mSwitch.setOnCheckedChangeListener(this);
            }
        }
    }

    /**
     * Listens to the state change of the location master switch.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            setLocationMode(MODE_HIGH_ACCURACY);
        } else {
            setLocationMode(MODE_LOCATION_OFF);
        }
    }
}
