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
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PanProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;

import java.util.HashMap;

public final class DeviceProfilesSettings extends DialogFragment implements
        CachedBluetoothDevice.Callback, DialogInterface.OnClickListener, OnClickListener {
    private static final String TAG = "DeviceProfilesSettings";

    public static final String ARG_DEVICE_ADDRESS = "device_address";

    private static final String KEY_PROFILE_CONTAINER = "profile_container";
    private static final String KEY_UNPAIR = "unpair";
    private static final String KEY_PBAP_SERVER = "PBAP Server";

    private CachedBluetoothDevice mCachedDevice;
    private LocalBluetoothManager mManager;
    private LocalBluetoothProfileManager mProfileManager;

    private ViewGroup mProfileContainer;
    private TextView mProfileLabel;
    private EditTextPreference mDeviceNamePref;

    private final HashMap<LocalBluetoothProfile, CheckBoxPreference> mAutoConnectPrefs
            = new HashMap<LocalBluetoothProfile, CheckBoxPreference>();

    private AlertDialog mDisconnectDialog;
    private boolean mProfileGroupIsRemoved;

    private View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mManager = Utils.getLocalBtManager(getActivity());
        CachedBluetoothDeviceManager deviceManager = mManager.getCachedDeviceManager();

        String address = getArguments().getString(ARG_DEVICE_ADDRESS);
        BluetoothDevice remoteDevice = mManager.getBluetoothAdapter().getRemoteDevice(address);

        mCachedDevice = deviceManager.findDevice(remoteDevice);
        if (mCachedDevice == null) {
            mCachedDevice = deviceManager.addDevice(mManager.getBluetoothAdapter(),
                    mManager.getProfileManager(), remoteDevice);
        }
        mProfileManager = mManager.getProfileManager();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mRootView = LayoutInflater.from(getContext()).inflate(R.layout.device_profiles_settings,
                null);
        mProfileContainer = (ViewGroup) mRootView.findViewById(R.id.profiles_section);
        mProfileLabel = (TextView) mRootView.findViewById(R.id.profiles_label);
        final EditText deviceName = (EditText) mRootView.findViewById(R.id.name);
        deviceName.setText(mCachedDevice.getName(), TextView.BufferType.EDITABLE);
        return new AlertDialog.Builder(getContext())
                .setView(mRootView)
                .setNeutralButton(R.string.forget, this)
                .setPositiveButton(R.string.okay, this)
                .setTitle(R.string.bluetooth_preference_paired_devices)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                EditText deviceName = (EditText) mRootView.findViewById(R.id.name);
                mCachedDevice.setName(deviceName.getText().toString());
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                mCachedDevice.unpair();
                com.android.settings.bluetooth.Utils.updateSearchIndex(getContext(),
                        BluetoothSettings.class.getName(), mCachedDevice.getName(),
                        getString(R.string.bluetooth_settings),
                        R.drawable.ic_settings_bluetooth, false);
                break;
        }
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
                dismiss();
                return;
            }
            addPreferencesForProfiles();
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

    private void addPreferencesForProfiles() {
        mProfileContainer.removeAllViews();
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {
            CheckBox pref = createProfilePreference(profile);
            mProfileContainer.addView(pref);
        }

        final int pbapPermission = mCachedDevice.getPhonebookPermissionChoice();
        // Only provide PBAP cabability if the client device has requested PBAP.
        if (pbapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN) {
            final PbapServerProfile psp = mManager.getProfileManager().getPbapProfile();
            CheckBox pbapPref = createProfilePreference(psp);
            mProfileContainer.addView(pbapPref);
        }

        final MapProfile mapProfile = mManager.getProfileManager().getMapProfile();
        final int mapPermission = mCachedDevice.getMessagePermissionChoice();
        if (mapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN) {
            CheckBox mapPreference = createProfilePreference(mapProfile);
            mProfileContainer.addView(mapPreference);
        }

        showOrHideProfileGroup();
    }

    private void showOrHideProfileGroup() {
        int numProfiles = mProfileContainer.getChildCount();
        if (!mProfileGroupIsRemoved && numProfiles == 0) {
            mProfileContainer.setVisibility(View.GONE);
            mProfileLabel.setVisibility(View.GONE);
            mProfileGroupIsRemoved = true;
        } else if (mProfileGroupIsRemoved && numProfiles != 0) {
            mProfileContainer.setVisibility(View.VISIBLE);
            mProfileLabel.setVisibility(View.VISIBLE);
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
    private CheckBox createProfilePreference(LocalBluetoothProfile profile) {
        CheckBox pref = new CheckBox(getActivity());
        pref.setTag(profile.toString());
        pref.setText(profile.getNameResource(mCachedDevice.getDevice()));
        pref.setOnClickListener(this);

        refreshProfilePreference(pref, profile);

        return pref;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof CheckBox) {
            LocalBluetoothProfile prof = getProfileOf(v);
            onProfileClicked(prof, (CheckBox) v);
        }
    }

    private void onProfileClicked(LocalBluetoothProfile profile, CheckBox profilePref) {
        BluetoothDevice device = mCachedDevice.getDevice();

        if (KEY_PBAP_SERVER.equals(profilePref.getTag())) {
            final int newPermission = mCachedDevice.getPhonebookPermissionChoice()
                == CachedBluetoothDevice.ACCESS_ALLOWED ? CachedBluetoothDevice.ACCESS_REJECTED
                : CachedBluetoothDevice.ACCESS_ALLOWED;
            mCachedDevice.setPhonebookPermissionChoice(newPermission);
            profilePref.setChecked(newPermission == CachedBluetoothDevice.ACCESS_ALLOWED);
            return;
        }

        if (!profilePref.isChecked()) {
            // Recheck it, until the dialog is done.
            profilePref.setChecked(true);
            askDisconnect(mManager.getForegroundActivity(), profile);
        } else {
            if (profile instanceof MapProfile) {
                mCachedDevice.setMessagePermissionChoice(BluetoothDevice.ACCESS_ALLOWED);
            }
            if (profile.isPreferred(device)) {
                // profile is preferred but not connected: disable auto-connect
                if (profile instanceof PanProfile) {
                    mCachedDevice.connectProfile(profile);
                } else {
                    profile.setPreferred(device, false);
                }
            } else {
                profile.setPreferred(device, true);
                mCachedDevice.connectProfile(profile);
            }
            refreshProfilePreference(profilePref, profile);
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
                }
                refreshProfilePreference(findProfile(profile.toString()), profile);
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
        final EditText deviceNameField = (EditText) mRootView.findViewById(R.id.name);
        if (deviceNameField != null) {
            deviceNameField.setText(mCachedDevice.getName());
        }

        refreshProfiles();
    }

    private void refreshProfiles() {
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {
            CheckBox profilePref = findProfile(profile.toString());
            if (profilePref == null) {
                profilePref = createProfilePreference(profile);
                mProfileContainer.addView(profilePref);
            } else {
                refreshProfilePreference(profilePref, profile);
            }
        }
        for (LocalBluetoothProfile profile : mCachedDevice.getRemovedProfiles()) {
            CheckBox profilePref = findProfile(profile.toString());
            if (profilePref != null) {
                Log.d(TAG, "Removing " + profile.toString() + " from profile list");
                mProfileContainer.removeView(profilePref);
            }
        }

        showOrHideProfileGroup();
    }

    private CheckBox findProfile(String profile) {
        return (CheckBox) mProfileContainer.findViewWithTag(profile);
    }

    private void refreshProfilePreference(CheckBox profilePref,
            LocalBluetoothProfile profile) {
        BluetoothDevice device = mCachedDevice.getDevice();

        // Gray out checkbox while connecting and disconnecting.
        profilePref.setEnabled(!mCachedDevice.isBusy());

        if (profile instanceof MapProfile) {
            profilePref.setChecked(mCachedDevice.getMessagePermissionChoice()
                    == CachedBluetoothDevice.ACCESS_ALLOWED);

        } else if (profile instanceof PbapServerProfile) {
            profilePref.setChecked(mCachedDevice.getPhonebookPermissionChoice()
                    == CachedBluetoothDevice.ACCESS_ALLOWED);

        } else if (profile instanceof PanProfile) {
            profilePref.setChecked(profile.getConnectionStatus(device) ==
                    BluetoothProfile.STATE_CONNECTED);

        } else {
            profilePref.setChecked(profile.isPreferred(device));
        }
    }

    private LocalBluetoothProfile getProfileOf(View v) {
        if (!(v instanceof CheckBox)) {
            return null;
        }
        String key = (String) v.getTag();
        if (TextUtils.isEmpty(key)) return null;

        try {
            return mProfileManager.getProfileByName(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
