/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.bluetooth;

import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothProfileManager.Profile;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;

/**
 * ConnectSpecificProfilesActivity presents the user with all of the profiles
 * for a particular device, and allows him to choose which should be connected
 * (or disconnected).
 */
public class ConnectSpecificProfilesActivity extends PreferenceActivity
        implements LocalBluetoothDevice.Callback, Preference.OnPreferenceChangeListener {
    private static final String TAG = "ConnectSpecificProfilesActivity";

    private static final String KEY_ONLINE_MODE = "online_mode";
    private static final String KEY_TITLE = "title";
    private static final String KEY_PROFILE_CONTAINER = "profile_container";

    public static final String EXTRA_ADDRESS = "address";
    
    private LocalBluetoothManager mManager;
    private LocalBluetoothDevice mDevice;
    
    private PreferenceGroup mProfileContainer;
    private CheckBoxPreference mOnlineModePreference;

    /**
     * The current mode of this activity and its checkboxes (either online mode
     * or offline mode). In online mode, user interactions with the profile
     * checkboxes will also toggle the profile's connectivity. In offline mode,
     * they will not, and only the preferred state will be saved for the
     * profile.
     */
    private boolean mOnlineMode;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String address;
        if (savedInstanceState != null) {
            address = savedInstanceState.getString(EXTRA_ADDRESS);
        } else {
            Intent intent = getIntent();
            address = intent.getStringExtra(EXTRA_ADDRESS);
        }

        if (TextUtils.isEmpty(address)) {
            Log.w(TAG, "Activity started without address");
            finish();
        }
        
        mManager = LocalBluetoothManager.getInstance(this);
        mDevice = mManager.getLocalDeviceManager().findDevice(address);
        if (mDevice == null) {
            Log.w(TAG, "Device not found, cannot connect to it");
            finish();
        }

        addPreferencesFromResource(R.xml.bluetooth_device_advanced);
        mProfileContainer = (PreferenceGroup) findPreference(KEY_PROFILE_CONTAINER);
        
        // Set the title of the screen
        findPreference(KEY_TITLE).setTitle(
                getString(R.string.bluetooth_device_advanced_title, mDevice.getName()));

        // Listen for check/uncheck of the online mode checkbox
        mOnlineModePreference = (CheckBoxPreference) findPreference(KEY_ONLINE_MODE);
        mOnlineModePreference.setOnPreferenceChangeListener(this);
        
        // Add a preference for each profile
        addPreferencesForProfiles();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putString(EXTRA_ADDRESS, mDevice.getAddress());
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        mManager.setForegroundActivity(this);
        mDevice.registerCallback(this);

        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        mDevice.unregisterCallback(this);
        mManager.setForegroundActivity(null);
    }

    private void addPreferencesForProfiles() {
        for (Profile profile : mDevice.getProfiles()) {
            Preference pref = createProfilePreference(profile);
            mProfileContainer.addPreference(pref);
        }
    }

    /**
     * Creates a checkbox preference for the particular profile. The key will be
     * the profile's name.
     * 
     * @param profile The profile for which the preference controls.
     * @return A preference that allows the user to choose whether this profile
     *         will be connected to.
     */
    private CheckBoxPreference createProfilePreference(Profile profile) {
        CheckBoxPreference pref = new CheckBoxPreference(this);
        pref.setKey(profile.toString());
        pref.setTitle(profile.localizedString);
        pref.setPersistent(false);
        pref.setOnPreferenceChangeListener(this);

        refreshProfilePreference(pref, profile);
        
        return pref;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (TextUtils.isEmpty(key) || newValue == null) return true;
        
        if (key.equals(KEY_ONLINE_MODE)) {
            onOnlineModeCheckedStateChanged((Boolean) newValue);
            
        } else {
            Profile profile = getProfileOf(preference);
            if (profile == null) return false;
            onProfileCheckedStateChanged(profile, (Boolean) newValue);
        }

        return true;
    }

    private void onOnlineModeCheckedStateChanged(boolean checked) {
        setOnlineMode(checked, true);
    }
    
    private void onProfileCheckedStateChanged(Profile profile, boolean checked) {
        if (mOnlineMode) {
            if (checked) {
                mDevice.connect(profile);
            } else {
                mDevice.disconnect(profile);
            }
        }
        
        LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                .getProfileManager(mManager, profile);
        profileManager.setPreferred(mDevice.getAddress(), checked);
    }
    
    public void onDeviceAttributesChanged(LocalBluetoothDevice device) {
        refresh();
    }

    private void refresh() {
        // We are in 'online mode' if we are connected, connecting, or disconnecting
        setOnlineMode(mDevice.isConnected() || mDevice.isBusy(), false);
        refreshProfiles();
    }

    /**
     * Switches between online/offline mode.
     * 
     * @param onlineMode Whether to be in online mode, or offline mode.
     * @param takeAction Whether to take action (i.e., connect or disconnect)
     *            based on the new online mode.
     */
    private void setOnlineMode(boolean onlineMode, boolean takeAction) {
        mOnlineMode = onlineMode;
            
        if (takeAction) {
            if (onlineMode) {
                mDevice.connect();
            } else {
                mDevice.disconnect();
            }
        }
            
        refreshOnlineModePreference();
    }
    
    private void refreshOnlineModePreference() {
        mOnlineModePreference.setChecked(mOnlineMode);

        /* Gray out checkbox while connecting and disconnecting */
        mOnlineModePreference.setEnabled(!mDevice.isBusy());

        /**
         * If the device is online, show status. Otherwise, show a summary that
         * describes what the checkbox does.
         */
        mOnlineModePreference.setSummary(mOnlineMode ? mDevice.getSummary()
                : R.string.bluetooth_device_advanced_online_mode_summary);
    }
    
    private void refreshProfiles() {
        for (Profile profile : mDevice.getProfiles()) {
            CheckBoxPreference profilePref =
                    (CheckBoxPreference) findPreference(profile.toString());
            if (profilePref == null) {
                profilePref = createProfilePreference(profile);
                mProfileContainer.addPreference(profilePref);
            } else {
                refreshProfilePreference(profilePref, profile);
            }
        }
    }
    
    private void refreshProfilePreference(CheckBoxPreference profilePref, Profile profile) {
        String address = mDevice.getAddress();
        LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                .getProfileManager(mManager, profile);
        
        int connectionStatus = profileManager.getConnectionStatus(address);

        /* Gray out checkbox while connecting and disconnecting */
        profilePref.setEnabled(!mDevice.isBusy());

        profilePref.setSummary(getProfileSummary(profileManager, profile, address,
                connectionStatus, mOnlineMode));
        
        profilePref.setChecked(profileManager.isPreferred(address));
    }

    private Profile getProfileOf(Preference pref) {
        if (!(pref instanceof CheckBoxPreference)) return null;
        String key = pref.getKey();
        if (TextUtils.isEmpty(key)) return null;
        
        try {
            return Profile.valueOf(pref.getKey());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int getProfileSummary(LocalBluetoothProfileManager profileManager,
            Profile profile, String address, int connectionStatus, boolean onlineMode) {
        if (!onlineMode || connectionStatus == SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED) {
            return getProfileSummaryForSettingPreference(profile);
        } else {
            return profileManager.getSummary(address);
        }
    }
    
    /**
     * Gets the summary that describes when checked, it will become a preferred profile.
     * 
     * @param profile The profile to get the summary for.
     * @return The summary.
     */
    private static final int getProfileSummaryForSettingPreference(Profile profile) {
        switch (profile) {
            case A2DP:
                return R.string.bluetooth_a2dp_profile_summary_use_for;
            case HEADSET:
                return R.string.bluetooth_headset_profile_summary_use_for;
            default:
                return 0;
        }
    }
    
}
