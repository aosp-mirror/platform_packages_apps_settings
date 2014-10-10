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

package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.text.TextWatcher;
import android.app.Dialog;
import android.widget.Button;
import android.text.Editable;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.Index;
import com.android.settings.search.SearchIndexableRaw;

import java.util.HashMap;

/**
 * This preference fragment presents the user with all of the profiles
 * for a particular device, and allows them to be individually connected
 * (or disconnected).
 */
public final class DeviceProfilesSettings extends SettingsPreferenceFragment
        implements CachedBluetoothDevice.Callback, Preference.OnPreferenceChangeListener {
    private static final String TAG = "DeviceProfilesSettings";

    private static final String KEY_PROFILE_CONTAINER = "profile_container";
    private static final String KEY_UNPAIR = "unpair";
    private static final String KEY_PBAP_SERVER = "PBAP Server";

    private CachedBluetoothDevice mCachedDevice;
    private LocalBluetoothManager mManager;
    private LocalBluetoothProfileManager mProfileManager;

    private PreferenceGroup mProfileContainer;
    private EditTextPreference mDeviceNamePref;

    private final HashMap<LocalBluetoothProfile, CheckBoxPreference> mAutoConnectPrefs
            = new HashMap<LocalBluetoothProfile, CheckBoxPreference>();

    private AlertDialog mDisconnectDialog;
    private boolean mProfileGroupIsRemoved;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.bluetooth_device_advanced);
        getPreferenceScreen().setOrderingAsAdded(false);
        mProfileContainer = (PreferenceGroup) findPreference(KEY_PROFILE_CONTAINER);
        mProfileContainer.setLayoutResource(R.layout.bluetooth_preference_category);

        mManager = LocalBluetoothManager.getInstance(getActivity());
        CachedBluetoothDeviceManager deviceManager =
                mManager.getCachedDeviceManager();
        mProfileManager = mManager.getProfileManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDisconnectDialog != null) {
            mDisconnectDialog.dismiss();
            mDisconnectDialog = null;
        }
        if (mCachedDevice != null) {
            mCachedDevice.unregisterCallback(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        mManager.setForegroundActivity(getActivity());
        if (mCachedDevice != null) {
            mCachedDevice.registerCallback(this);
            if (mCachedDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                finish();
                return;
            }
            refresh();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCachedDevice != null) {
            mCachedDevice.unregisterCallback(this);
        }

        mManager.setForegroundActivity(null);
    }

    public void setDevice(CachedBluetoothDevice cachedDevice) {
        mCachedDevice = cachedDevice;

        if (isResumed()) {
            mCachedDevice.registerCallback(this);
            addPreferencesForProfiles();
            refresh();
        }
    }

    private void addPreferencesForProfiles() {
        mProfileContainer.removeAll();
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {
            Preference pref = createProfilePreference(profile);
            mProfileContainer.addPreference(pref);
        }

        final int pbapPermission = mCachedDevice.getPhonebookPermissionChoice();
        // Only provide PBAP cabability if the client device has requested PBAP.
        if (pbapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN) {
            final PbapServerProfile psp = mManager.getProfileManager().getPbapProfile();
            CheckBoxPreference pbapPref = createProfilePreference(psp);
            mProfileContainer.addPreference(pbapPref);
        }

        final MapProfile mapProfile = mManager.getProfileManager().getMapProfile();
        final int mapPermission = mCachedDevice.getMessagePermissionChoice();
        if (mapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN) {
            CheckBoxPreference mapPreference = createProfilePreference(mapProfile);
            mProfileContainer.addPreference(mapPreference);
        }

        showOrHideProfileGroup();
    }

    private void showOrHideProfileGroup() {
        int numProfiles = mProfileContainer.getPreferenceCount();
        if (!mProfileGroupIsRemoved && numProfiles == 0) {
            getPreferenceScreen().removePreference(mProfileContainer);
            mProfileGroupIsRemoved = true;
        } else if (mProfileGroupIsRemoved && numProfiles != 0) {
            getPreferenceScreen().addPreference(mProfileContainer);
            mProfileGroupIsRemoved = false;
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
    private CheckBoxPreference createProfilePreference(LocalBluetoothProfile profile) {
        CheckBoxPreference pref = new CheckBoxPreference(getActivity());
        pref.setLayoutResource(R.layout.preference_start_widget);
        pref.setKey(profile.toString());
        pref.setTitle(profile.getNameResource(mCachedDevice.getDevice()));
        pref.setPersistent(false);
        pref.setOrder(getProfilePreferenceIndex(profile.getOrdinal()));
        pref.setOnPreferenceChangeListener(this);

        int iconResource = profile.getDrawableResource(mCachedDevice.getBtClass());
        if (iconResource != 0) {
            pref.setIcon(getActivity().getDrawable(iconResource));
        }

        refreshProfilePreference(pref, profile);

        return pref;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDeviceNamePref) {
            mCachedDevice.setName((String) newValue);
        } else if (preference instanceof CheckBoxPreference) {
            LocalBluetoothProfile prof = getProfileOf(preference);
            onProfileClicked(prof, (CheckBoxPreference) preference);
            return false;   // checkbox will update from onDeviceAttributesChanged() callback
        } else {
            return false;
        }

        return true;
    }

    private void onProfileClicked(LocalBluetoothProfile profile, CheckBoxPreference profilePref) {
        BluetoothDevice device = mCachedDevice.getDevice();

        if (profilePref.getKey().equals(KEY_PBAP_SERVER)) {
            final int newPermission = mCachedDevice.getPhonebookPermissionChoice()
                == CachedBluetoothDevice.ACCESS_ALLOWED ? CachedBluetoothDevice.ACCESS_REJECTED
                : CachedBluetoothDevice.ACCESS_ALLOWED;
            mCachedDevice.setPhonebookPermissionChoice(newPermission);
            profilePref.setChecked(newPermission == CachedBluetoothDevice.ACCESS_ALLOWED);
            return;
        }

        int status = profile.getConnectionStatus(device);
        boolean isConnected =
                status == BluetoothProfile.STATE_CONNECTED;

        if (profilePref.isChecked()) {
            askDisconnect(mManager.getForegroundActivity(), profile);
        } else {
            if (profile instanceof MapProfile) {
                mCachedDevice.setMessagePermissionChoice(BluetoothDevice.ACCESS_ALLOWED);
                refreshProfilePreference(profilePref, profile);
            }
            if (profile.isPreferred(device)) {
                // profile is preferred but not connected: disable auto-connect
                profile.setPreferred(device, false);
                refreshProfilePreference(profilePref, profile);
            } else {
                profile.setPreferred(device, true);
                mCachedDevice.connectProfile(profile);
            }
        }
    }

    private void askDisconnect(Context context,
            final LocalBluetoothProfile profile) {
        // local reference for callback
        final CachedBluetoothDevice device = mCachedDevice;
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.bluetooth_device);
        }

        String profileName = context.getString(profile.getNameResource(device.getDevice()));

        String title = context.getString(R.string.bluetooth_disable_profile_title);
        String message = context.getString(R.string.bluetooth_disable_profile_message,
                profileName, name);

        DialogInterface.OnClickListener disconnectListener =
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                device.disconnect(profile);
                profile.setPreferred(device.getDevice(), false);
                if (profile instanceof MapProfile) {
                    device.setMessagePermissionChoice(BluetoothDevice.ACCESS_REJECTED);
                    refreshProfilePreference(
                            (CheckBoxPreference)findPreference(profile.toString()), profile);
                }
            }
        };

        mDisconnectDialog = Utils.showDisconnectDialog(context,
                mDisconnectDialog, disconnectListener, title, Html.fromHtml(message));
    }

    @Override
    public void onDeviceAttributesChanged() {
        refresh();
    }

    private void refresh() {
        final EditText deviceNameField = (EditText) getView().findViewById(R.id.name);
        if (deviceNameField != null) {
            deviceNameField.setText(mCachedDevice.getName());
        }

        refreshProfiles();
    }

    private void refreshProfiles() {
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {
            CheckBoxPreference profilePref = (CheckBoxPreference)findPreference(profile.toString());
            if (profilePref == null) {
                profilePref = createProfilePreference(profile);
                mProfileContainer.addPreference(profilePref);
            } else {
                refreshProfilePreference(profilePref, profile);
            }
        }
        for (LocalBluetoothProfile profile : mCachedDevice.getRemovedProfiles()) {
            Preference profilePref = findPreference(profile.toString());
            if (profilePref != null) {
                Log.d(TAG, "Removing " + profile.toString() + " from profile list");
                mProfileContainer.removePreference(profilePref);
            }
        }

        showOrHideProfileGroup();
    }

    private void refreshProfilePreference(CheckBoxPreference profilePref,
            LocalBluetoothProfile profile) {
        BluetoothDevice device = mCachedDevice.getDevice();

        // Gray out checkbox while connecting and disconnecting.
        profilePref.setEnabled(!mCachedDevice.isBusy());

        if (profile instanceof MapProfile) {
            profilePref.setChecked(mCachedDevice.getMessagePermissionChoice()
                    == CachedBluetoothDevice.ACCESS_ALLOWED);
        } else if (profile instanceof PbapServerProfile) {
            // Handle PBAP specially.
            profilePref.setChecked(mCachedDevice.getPhonebookPermissionChoice()
                    == CachedBluetoothDevice.ACCESS_ALLOWED);
        } else {
            profilePref.setChecked(profile.isPreferred(device));
        }
    }

    private LocalBluetoothProfile getProfileOf(Preference pref) {
        if (!(pref instanceof CheckBoxPreference)) {
            return null;
        }
        String key = pref.getKey();
        if (TextUtils.isEmpty(key)) return null;

        try {
            return mProfileManager.getProfileByName(pref.getKey());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private int getProfilePreferenceIndex(int profIndex) {
        return mProfileContainer.getOrder() + profIndex * 10;
    }
}
