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
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.bluetooth.LocalBluetoothProfileManager.Profile;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.HashMap;

/**
 * ConnectSpecificProfilesActivity presents the user with all of the profiles
 * for a particular device, and allows him to choose which should be connected
 * (or disconnected).
 */
public class DeviceProfilesSettings extends SettingsPreferenceFragment
        implements CachedBluetoothDevice.Callback, Preference.OnPreferenceChangeListener,
                View.OnClickListener {
    private static final String TAG = "DeviceProfilesSettings";

    private static final String KEY_TITLE = "title";
    private static final String KEY_RENAME_DEVICE = "rename_device";
    private static final String KEY_PROFILE_CONTAINER = "profile_container";
    private static final String KEY_UNPAIR = "unpair";
    private static final String KEY_ALLOW_INCOMING = "allow_incoming";

    private static final String AUTO_CONNECT_KEY_SUFFIX = "X";

    public static final String EXTRA_DEVICE = "device";

    private LocalBluetoothManager mManager;
    private CachedBluetoothDevice mCachedDevice;

    private PreferenceGroup mProfileContainer;
    private CheckBoxPreference mAllowIncomingPref;
    private EditTextPreference mDeviceNamePref;
    private HashMap<String,CheckBoxPreference> mAutoConnectPrefs
            = new HashMap<String,CheckBoxPreference>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothDevice device;
        if (savedInstanceState != null) {
            device = savedInstanceState.getParcelable(EXTRA_DEVICE);
        } else {
            Bundle args = getArguments();
            device = args.getParcelable(EXTRA_DEVICE);
        }

        if (device == null) {
            Log.w(TAG, "Activity started without a remote Bluetooth device");
            finish();
        }

        mManager = LocalBluetoothManager.getInstance(getActivity());
        mCachedDevice = mManager.getCachedDeviceManager().findDevice(device);
        if (mCachedDevice == null) {
            Log.w(TAG, "Device not found, cannot connect to it");
            finish();
        }

        addPreferencesFromResource(R.xml.bluetooth_device_advanced);
        getPreferenceScreen().setOrderingAsAdded(false);

        mProfileContainer = (PreferenceGroup) findPreference(KEY_PROFILE_CONTAINER);
        mAllowIncomingPref = (CheckBoxPreference) findPreference(KEY_ALLOW_INCOMING);
        mAllowIncomingPref.setChecked(isIncomingFileTransfersAllowed());

        mDeviceNamePref = (EditTextPreference) findPreference(KEY_RENAME_DEVICE);
        mDeviceNamePref.setSummary(mCachedDevice.getName());
        mDeviceNamePref.setText(mCachedDevice.getName());

        // Set the title of the screen
        findPreference(KEY_TITLE).setTitle(getResources()
                .getString(R.string.bluetooth_device_advanced_title, mCachedDevice.getName()));

        // Add a preference for each profile
        addPreferencesForProfiles();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(EXTRA_DEVICE, mCachedDevice.getDevice());
    }

    @Override
    public void onResume() {
        super.onResume();

        mManager.setForegroundActivity(getActivity());
        mCachedDevice.registerCallback(this);

        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();

        mCachedDevice.unregisterCallback(this);
        mManager.setForegroundActivity(null);
    }

    private void addPreferencesForProfiles() {
        for (Profile profile : mCachedDevice.getConnectableProfiles()) {
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
    private Preference createProfilePreference(Profile profile) {
        BluetoothProfilePreference pref = new BluetoothProfilePreference(getActivity(), profile);
        pref.setKey(profile.toString());
        pref.setTitle(profile.localizedString);
        pref.setExpanded(false);
        pref.setPersistent(false);
        pref.setOrder(getProfilePreferenceIndex(profile));
        pref.setOnExpandClickListener(this);

        LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                .getProfileManager(mManager, profile);

        /**
         * Gray out profile while connecting and disconnecting
         */
        pref.setEnabled(!mCachedDevice.isBusy());

        refreshProfilePreference(pref, profile);

        return pref;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        String key = preference.getKey();
        if (preference instanceof BluetoothProfilePreference) {
            onProfileClicked(preference, Profile.valueOf(key));
            return true;
        } else if (key.equals(KEY_UNPAIR)) {
            unpairDevice();
            finish();
            return true;
        }

        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAllowIncomingPref) {
            setIncomingFileTransfersAllowed(mAllowIncomingPref.isChecked());
        } else if (preference == mDeviceNamePref) {
            // TODO: Verify and check for error conditions
            mCachedDevice.setName(mDeviceNamePref.getText());
        } else {
            return false;
        }

        return true;
    }

    private void onProfileClicked(Preference preference, Profile profile) {
        LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                .getProfileManager(mManager, profile);
        // TODO: Get the current state and flip it, updating the summary for the preference

//        profileManager.setPreferred(mCachedDevice.getDevice(), checked);
//
//        if (checked) {
//            mCachedDevice.connect(profile);
//        } else {
//            mCachedDevice.disconnect(profile);
//        }
    }

    public void onDeviceAttributesChanged(CachedBluetoothDevice cachedDevice) {
        refresh();
    }

    private void refresh() {
        refreshProfiles();
    }

    private void refreshProfiles() {
        for (Profile profile : mCachedDevice.getConnectableProfiles()) {
            Preference profilePref = findPreference(profile.toString());
            if (profilePref == null) {
                profilePref = createProfilePreference(profile);
                mProfileContainer.addPreference(profilePref);
            } else {
                refreshProfilePreference(profilePref, profile);
            }
        }
    }

    private void refreshProfilePreference(Preference profilePref, Profile profile) {
        BluetoothDevice device = mCachedDevice.getDevice();
        LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                .getProfileManager(mManager, profile);

        int connectionStatus = profileManager.getConnectionStatus(device);

        /*
         * Gray out checkbox while connecting and disconnecting
         */
        profilePref.setEnabled(!mCachedDevice.isBusy());
        profilePref.setSummary(getProfileSummary(profileManager, profile, device,
                connectionStatus, isDeviceOnline()));
        // TODO:
        //profilePref.setChecked(profileManager.isPreferred(device));
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
            Profile profile, BluetoothDevice device, int connectionStatus, boolean onlineMode) {
        if (!onlineMode || connectionStatus == SettingsBtStatus.CONNECTION_STATUS_DISCONNECTED) {
            return getProfileSummaryForSettingPreference(profile);
        } else {
            return profileManager.getSummary(device);
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
            case HID:
                return R.string.bluetooth_hid_profile_summary_use_for;
            default:
                return 0;
        }
    }

    public void onClick(View v) {
        if (v.getTag() instanceof Profile) {
            Profile prof = (Profile) v.getTag();
            CheckBoxPreference autoConnectPref = mAutoConnectPrefs.get(prof.toString());
            if (autoConnectPref == null) {
                autoConnectPref = new CheckBoxPreference(getActivity());
                autoConnectPref.setKey(prof.toString() + AUTO_CONNECT_KEY_SUFFIX);
                autoConnectPref.setTitle(getCheckBoxTitle(prof));
                autoConnectPref.setOrder(getProfilePreferenceIndex(prof) + 1);
                autoConnectPref.setChecked(getAutoConnect(prof));
                mAutoConnectPrefs.put(prof.name(), autoConnectPref);
            }
            BluetoothProfilePreference profilePref =
                    (BluetoothProfilePreference) findPreference(prof.toString());
            if (profilePref != null) {
                if (profilePref.isExpanded()) {
                    mProfileContainer.addPreference(autoConnectPref);
                } else {
                    mProfileContainer.removePreference(autoConnectPref);
                }
            }
        }
    }

    private int getProfilePreferenceIndex(Profile prof) {
        return mProfileContainer.getOrder() + prof.ordinal() * 10;
    }

    private void unpairDevice() {
        mCachedDevice.unpair();
    }

    private boolean isDeviceOnline() {
        // TODO: Verify
        return mCachedDevice.isConnected() || mCachedDevice.isBusy();
    }

    private void setIncomingFileTransfersAllowed(boolean allow) {
        // TODO:
        Log.d(TAG, "Set allow incoming = " + allow);
    }

    private boolean isIncomingFileTransfersAllowed() {
        // TODO:
        return true;
    }

    private String getCheckBoxTitle(Profile prof) {
        // TODO: Use resources and base it on profile if necessary
        return "Auto connect";
    }

    private boolean getAutoConnect(Profile prof) {
        // TODO: Get the auto connect toggle state for the profile
        return true;
    }
}
