/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Config;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Lock Settings
    
    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "tactilefeedback";
    private static final int CONFIRM_PATTERN_THEN_DISABLE_AND_CLEAR_REQUEST_CODE = 55;

    private LockPatternUtils mLockPatternUtils;
    private CheckBoxPreference mLockEnabled;
    private CheckBoxPreference mVisiblePattern;
    private CheckBoxPreference mTactileFeedback;
    private Preference mChoosePattern;

    private CheckBoxPreference mShowPassword;
    
    // Location Settings
    
    private static final String LOCATION_NETWORK = "location_network";
    private static final String LOCATION_GPS = "location_gps";

    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mGps;
    private LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.security_settings);

        mLockPatternUtils = new LockPatternUtils(getContentResolver());

        createPreferenceHierarchy();
        
        // Get the available location providers
        mLocationManager = (LocationManager)
            getSystemService(Context.LOCATION_SERVICE);

        mNetwork = (CheckBoxPreference) getPreferenceScreen().findPreference(LOCATION_NETWORK);
        mGps = (CheckBoxPreference) getPreferenceScreen().findPreference(LOCATION_GPS);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        updateToggles();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = this.getPreferenceScreen();

        // Inline preferences
        PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
        inlinePrefCat.setTitle(R.string.lock_settings_title);
        root.addPreference(inlinePrefCat);

        // autolock toggle
        mLockEnabled = new LockEnabledPref(this);
        mLockEnabled.setTitle(R.string.lockpattern_settings_enable_title);
        mLockEnabled.setSummary(R.string.lockpattern_settings_enable_summary);
        mLockEnabled.setKey(KEY_LOCK_ENABLED);
        inlinePrefCat.addPreference(mLockEnabled);

        // visible pattern
        mVisiblePattern = new CheckBoxPreference(this);
        mVisiblePattern.setKey(KEY_VISIBLE_PATTERN);
        mVisiblePattern.setTitle(R.string.lockpattern_settings_enable_visible_pattern_title);
        inlinePrefCat.addPreference(mVisiblePattern);

        // tactile feedback
        mTactileFeedback = new CheckBoxPreference(this);
        mTactileFeedback.setKey(KEY_TACTILE_FEEDBACK_ENABLED);
        mTactileFeedback.setTitle(R.string.lockpattern_settings_enable_tactile_feedback_title);
        inlinePrefCat.addPreference(mTactileFeedback);

        // change pattern lock
        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                    "com.android.settings.ChooseLockPatternTutorial");
        mChoosePattern = getPreferenceManager().createPreferenceScreen(this);
        mChoosePattern.setIntent(intent);
        inlinePrefCat.addPreference(mChoosePattern);
        
        PreferenceScreen simLockPreferences = getPreferenceManager()
                .createPreferenceScreen(this);
        simLockPreferences.setTitle(R.string.sim_lock_settings_category);
        // Intent to launch SIM lock settings
        intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.SimLockSettings");
        simLockPreferences.setIntent(intent);
        
        PreferenceCategory simLockCat = new PreferenceCategory(this);
        simLockCat.setTitle(R.string.sim_lock_settings_title);
        root.addPreference(simLockCat);
        simLockCat.addPreference(simLockPreferences);

        // Passwords
        PreferenceCategory passwordsCat = new PreferenceCategory(this);
        passwordsCat.setTitle(R.string.security_passwords_title);
        root.addPreference(passwordsCat);
        
        CheckBoxPreference showPassword = mShowPassword = new CheckBoxPreference(this);
        showPassword.setKey("show_password");
        showPassword.setTitle(R.string.show_password);
        showPassword.setSummary(R.string.show_password_summary);
        showPassword.setPersistent(false);
        passwordsCat.addPreference(showPassword);
        
        return root;
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean patternExists = mLockPatternUtils.savedPatternExists();
        mLockEnabled.setEnabled(patternExists);
        mVisiblePattern.setEnabled(patternExists);
        mTactileFeedback.setEnabled(patternExists);

        mLockEnabled.setChecked(mLockPatternUtils.isLockPatternEnabled());
        mVisiblePattern.setChecked(mLockPatternUtils.isVisiblePatternEnabled());
        mTactileFeedback.setChecked(mLockPatternUtils.isTactileFeedbackEnabled());

        int chooseStringRes = mLockPatternUtils.savedPatternExists() ?
                R.string.lockpattern_settings_change_lock_pattern :
                R.string.lockpattern_settings_choose_lock_pattern;
        mChoosePattern.setTitle(chooseStringRes);
        
        mShowPassword
                .setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        final String key = preference.getKey();

        if (KEY_LOCK_ENABLED.equals(key)) {
            mLockPatternUtils.setLockPatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            mLockPatternUtils.setVisiblePatternEnabled(isToggled(preference));
        } else if (KEY_TACTILE_FEEDBACK_ENABLED.equals(key)) {
            mLockPatternUtils.setTactileFeedbackEnabled(isToggled(preference));
        } else if (preference == mShowPassword) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    mShowPassword.isChecked() ? 1 : 0);
        }
        
        return false;
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateToggles() {
        String providers = getAllowedProviders();
        mNetwork.setChecked(providers.contains(LocationManager.NETWORK_PROVIDER));
        mGps.setChecked(providers.contains(LocationManager.GPS_PROVIDER));
    }

    private void updateProviders() {
        String preferredProviders = "";
        if (mNetwork.isChecked()) {
            preferredProviders += LocationManager.NETWORK_PROVIDER;
        }
        if (mGps.isChecked()) {
            preferredProviders += "," + LocationManager.GPS_PROVIDER;
        }
        setProviders(preferredProviders);
    }

    private void setProviders(String providers) {
        // Update the secure setting LOCATION_PROVIDERS_ALLOWED
        Settings.Secure.putString(getContentResolver(),
            Settings.Secure.LOCATION_PROVIDERS_ALLOWED, providers);
        if (Config.LOGV) {
            Log.v("Location Accuracy", "Setting LOCATION_PROVIDERS_ALLOWED = " + providers);
        }
        // Inform the location manager about the changes
        mLocationManager.updateProviders();
    }

    /**
     * @return string containing a list of providers that have been enabled for use
     */
    private String getAllowedProviders() {
        String allowedProviders =
            Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (allowedProviders == null) {
            allowedProviders = "";
        }
        return allowedProviders;
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (LOCATION_NETWORK.equals(key) || LOCATION_GPS.equals(key)) {
            updateProviders();
        }
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }


    /**
     * For the user to disable keyguard, we first make them verify their
     * existing pattern.
     */
    private class LockEnabledPref extends CheckBoxPreference {

        public LockEnabledPref(Context context) {
            super(context);
        }

        @Override
        protected void onClick() {
            if (mLockPatternUtils.savedPatternExists() && isChecked()) {
                confirmPatternThenDisableAndClear();
            } else {
                super.onClick();
            }
        }
    }

    /**
     * Launch screen to confirm the existing lock pattern.
     * @see #onActivityResult(int, int, android.content.Intent)
     */
    private void confirmPatternThenDisableAndClear() {
        final Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.ConfirmLockPattern");
        startActivityForResult(intent, CONFIRM_PATTERN_THEN_DISABLE_AND_CLEAR_REQUEST_CODE);
    }

    /**
     * @see #confirmPatternThenDisableAndClear
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final boolean resultOk = resultCode == Activity.RESULT_OK;

        if ((requestCode == CONFIRM_PATTERN_THEN_DISABLE_AND_CLEAR_REQUEST_CODE) && resultOk) {
            mLockPatternUtils.setLockPatternEnabled(false);
            mLockPatternUtils.saveLockPattern(null);
        }
    }
}
