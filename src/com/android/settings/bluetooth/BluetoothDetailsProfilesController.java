/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PanProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.List;

/**
 * This class adds switches for toggling the individual profiles that a Bluetooth device
 * supports, such as "Phone audio", "Media audio", "Contact sharing", etc.
 */
public class BluetoothDetailsProfilesController extends BluetoothDetailsController
        implements Preference.OnPreferenceClickListener,
        LocalBluetoothProfileManager.ServiceListener {
    private static final String KEY_PROFILES_GROUP = "bluetooth_profiles";
    private static final String KEY_BOTTOM_PREFERENCE = "bottom_preference";
    private static final int ORDINAL = 99;

    @VisibleForTesting
    static final String HIGH_QUALITY_AUDIO_PREF_TAG = "A2dpProfileHighQualityAudio";

    private LocalBluetoothManager mManager;
    private LocalBluetoothProfileManager mProfileManager;
    private CachedBluetoothDevice mCachedDevice;

    @VisibleForTesting
    PreferenceCategory mProfilesContainer;

    public BluetoothDetailsProfilesController(Context context, PreferenceFragmentCompat fragment,
            LocalBluetoothManager manager, CachedBluetoothDevice device, Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mManager = manager;
        mProfileManager = mManager.getProfileManager();
        mCachedDevice = device;
        lifecycle.addObserver(this);
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mProfilesContainer = (PreferenceCategory)screen.findPreference(getPreferenceKey());
        mProfilesContainer.setLayoutResource(R.layout.preference_bluetooth_profile_category);
        // Call refresh here even though it will get called later in onResume, to avoid the
        // list of switches appearing to "pop" into the page.
        refresh();
    }

    /**
     * Creates a switch preference for the particular profile.
     *
     * @param context The context to use when creating the SwitchPreference
     * @param profile The profile for which the preference controls.
     * @return A preference that allows the user to choose whether this profile
     * will be connected to.
     */
    private SwitchPreference createProfilePreference(Context context,
            LocalBluetoothProfile profile) {
        SwitchPreference pref = new SwitchPreference(context);
        pref.setKey(profile.toString());
        pref.setTitle(profile.getNameResource(mCachedDevice.getDevice()));
        pref.setOnPreferenceClickListener(this);
        pref.setOrder(profile.getOrdinal());
        return pref;
    }

    /**
     * Refreshes the state for an existing SwitchPreference for a profile.
     */
    private void refreshProfilePreference(SwitchPreference profilePref,
            LocalBluetoothProfile profile) {
        BluetoothDevice device = mCachedDevice.getDevice();
        profilePref.setEnabled(!mCachedDevice.isBusy());
        if (profile instanceof MapProfile) {
            profilePref.setChecked(device.getMessageAccessPermission()
                    == BluetoothDevice.ACCESS_ALLOWED);
        } else if (profile instanceof PbapServerProfile) {
            profilePref.setChecked(device.getPhonebookAccessPermission()
                    == BluetoothDevice.ACCESS_ALLOWED);
        } else if (profile instanceof PanProfile) {
            profilePref.setChecked(profile.getConnectionStatus(device) ==
                    BluetoothProfile.STATE_CONNECTED);
        } else {
            profilePref.setChecked(profile.isEnabled(device));
        }

        if (profile instanceof A2dpProfile) {
            A2dpProfile a2dp = (A2dpProfile) profile;
            SwitchPreference highQualityPref = (SwitchPreference) mProfilesContainer.findPreference(
                    HIGH_QUALITY_AUDIO_PREF_TAG);
            if (highQualityPref != null) {
                if (a2dp.isEnabled(device) && a2dp.supportsHighQualityAudio(device)) {
                    highQualityPref.setVisible(true);
                    highQualityPref.setTitle(a2dp.getHighQualityAudioOptionLabel(device));
                    highQualityPref.setChecked(a2dp.isHighQualityAudioEnabled(device));
                    highQualityPref.setEnabled(!mCachedDevice.isBusy());
                } else {
                    highQualityPref.setVisible(false);
                }
            }
        }
    }

    /**
     * Helper method to enable a profile for a device.
     */
    private void enableProfile(LocalBluetoothProfile profile) {
        final BluetoothDevice bluetoothDevice = mCachedDevice.getDevice();
        if (profile instanceof PbapServerProfile) {
            bluetoothDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
            // We don't need to do the additional steps below for this profile.
            return;
        }
        if (profile instanceof MapProfile) {
            bluetoothDevice.setMessageAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
        }
        profile.setEnabled(bluetoothDevice, true);
    }

    /**
     * Helper method to disable a profile for a device
     */
    private void disableProfile(LocalBluetoothProfile profile) {
        final BluetoothDevice bluetoothDevice = mCachedDevice.getDevice();
        profile.setEnabled(bluetoothDevice, false);
        if (profile instanceof MapProfile) {
            bluetoothDevice.setMessageAccessPermission(BluetoothDevice.ACCESS_REJECTED);
        } else if (profile instanceof PbapServerProfile) {
            bluetoothDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
        }
    }

    /**
     * When the pref for a bluetooth profile is clicked on, we want to toggle the enabled/disabled
     * state for that profile.
     */
    @Override
    public boolean onPreferenceClick(Preference preference) {
        LocalBluetoothProfile profile = mProfileManager.getProfileByName(preference.getKey());
        if (profile == null) {
            // It might be the PbapServerProfile, which is not stored by name.
            PbapServerProfile psp = mManager.getProfileManager().getPbapProfile();
            if (TextUtils.equals(preference.getKey(), psp.toString())) {
                profile = psp;
            } else {
                return false;
            }
        }
        SwitchPreference profilePref = (SwitchPreference) preference;
        if (profilePref.isChecked()) {
            enableProfile(profile);
        } else {
            disableProfile(profile);
        }
        refreshProfilePreference(profilePref, profile);
        return true;
    }


    /**
     * Helper to get the list of connectable and special profiles.
     */
    private List<LocalBluetoothProfile> getProfiles() {
        List<LocalBluetoothProfile> result = mCachedDevice.getConnectableProfiles();
        final BluetoothDevice device = mCachedDevice.getDevice();

        final int pbapPermission = device.getPhonebookAccessPermission();
        // Only provide PBAP cabability if the client device has requested PBAP.
        if (pbapPermission != BluetoothDevice.ACCESS_UNKNOWN) {
            final PbapServerProfile psp = mManager.getProfileManager().getPbapProfile();
            result.add(psp);
        }

        final MapProfile mapProfile = mManager.getProfileManager().getMapProfile();
        final int mapPermission = device.getMessageAccessPermission();
        if (mapPermission != BluetoothDevice.ACCESS_UNKNOWN) {
            result.add(mapProfile);
        }

        return result;
    }

    /**
     * This is a helper method to be called after adding a Preference for a profile. If that
     * profile happened to be A2dp and the device supports high quality audio, it will add a
     * separate preference for controlling whether to actually use high quality audio.
     *
     * @param profile the profile just added
     */
    private void maybeAddHighQualityAudioPref(LocalBluetoothProfile profile) {
        if (!(profile instanceof A2dpProfile)) {
            return;
        }
        BluetoothDevice device = mCachedDevice.getDevice();
        A2dpProfile a2dp = (A2dpProfile) profile;
        if (a2dp.isProfileReady() && a2dp.supportsHighQualityAudio(device)) {
            SwitchPreference highQualityAudioPref = new SwitchPreference(
                    mProfilesContainer.getContext());
            highQualityAudioPref.setKey(HIGH_QUALITY_AUDIO_PREF_TAG);
            highQualityAudioPref.setVisible(false);
            highQualityAudioPref.setOnPreferenceClickListener(clickedPref -> {
                boolean enable = ((SwitchPreference) clickedPref).isChecked();
                a2dp.setHighQualityAudioEnabled(mCachedDevice.getDevice(), enable);
                return true;
            });
            mProfilesContainer.addPreference(highQualityAudioPref);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mProfileManager.removeServiceListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mProfileManager.addServiceListener(this);
    }

    @Override
    public void onServiceConnected() {
        refresh();
    }

    @Override
    public void onServiceDisconnected() {
        refresh();
    }

    /**
     * Refreshes the state of the switches for all profiles, possibly adding or removing switches as
     * needed.
     */
    @Override
    protected void refresh() {
        for (LocalBluetoothProfile profile : getProfiles()) {
            if (!profile.isProfileReady()) {
                continue;
            }
            SwitchPreference pref = mProfilesContainer.findPreference(
                    profile.toString());
            if (pref == null) {
                pref = createProfilePreference(mProfilesContainer.getContext(), profile);
                mProfilesContainer.addPreference(pref);
                maybeAddHighQualityAudioPref(profile);
            }
            refreshProfilePreference(pref, profile);
        }
        for (LocalBluetoothProfile removedProfile : mCachedDevice.getRemovedProfiles()) {
            final SwitchPreference pref = mProfilesContainer.findPreference(
                    removedProfile.toString());
            if (pref != null) {
                mProfilesContainer.removePreference(pref);
            }
        }

        Preference preference = mProfilesContainer.findPreference(KEY_BOTTOM_PREFERENCE);
        if (preference == null) {
            preference = new Preference(mContext);
            preference.setLayoutResource(R.layout.preference_bluetooth_profile_category);
            preference.setEnabled(false);
            preference.setKey(KEY_BOTTOM_PREFERENCE);
            preference.setOrder(ORDINAL);
            mProfilesContainer.addPreference(preference);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PROFILES_GROUP;
    }
}