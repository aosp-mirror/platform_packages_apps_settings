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

import java.util.HashMap;

/**
 * This preference fragment presents the user with all of the profiles
 * for a particular device, and allows them to be individually connected
 * (or disconnected).
 */
public final class DeviceProfilesSettings extends SettingsPreferenceFragment
        implements CachedBluetoothDevice.Callback, Preference.OnPreferenceChangeListener {
    private static final String TAG = "DeviceProfilesSettings";

    private static final String KEY_RENAME_DEVICE = "rename_device";
    private static final String KEY_PROFILE_CONTAINER = "profile_container";
    private static final String KEY_UNPAIR = "unpair";

    public static final String EXTRA_DEVICE = "device";
    private RenameEditTextPreference mRenameDeviceNamePref;
    private LocalBluetoothManager mManager;
    private CachedBluetoothDevice mCachedDevice;
    private LocalBluetoothProfileManager mProfileManager;

    private static final int OK_BUTTON = -1;

    private PreferenceGroup mProfileContainer;
    private EditTextPreference mDeviceNamePref;

    private final HashMap<LocalBluetoothProfile, CheckBoxPreference> mAutoConnectPrefs
            = new HashMap<LocalBluetoothProfile, CheckBoxPreference>();

    private AlertDialog mDisconnectDialog;
    private boolean mProfileGroupIsRemoved;

    private class RenameEditTextPreference implements TextWatcher{
        public void afterTextChanged(Editable s) {
            Dialog d = mDeviceNamePref.getDialog();
            if (d instanceof AlertDialog) {
                ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(s.length() > 0);
            }
        }

        // TextWatcher interface
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // not used
        }

        // TextWatcher interface
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // not used
        }
    }

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

        addPreferencesFromResource(R.xml.bluetooth_device_advanced);
        getPreferenceScreen().setOrderingAsAdded(false);
        mProfileContainer = (PreferenceGroup) findPreference(KEY_PROFILE_CONTAINER);
        mDeviceNamePref = (EditTextPreference) findPreference(KEY_RENAME_DEVICE);

        if (device == null) {
            Log.w(TAG, "Activity started without a remote Bluetooth device");
            finish();
            return;  // TODO: test this failure path
        }
        mRenameDeviceNamePref = new RenameEditTextPreference();
        mManager = LocalBluetoothManager.getInstance(getActivity());
        CachedBluetoothDeviceManager deviceManager =
                mManager.getCachedDeviceManager();
        mProfileManager = mManager.getProfileManager();
        mCachedDevice = deviceManager.findDevice(device);
        if (mCachedDevice == null) {
            Log.w(TAG, "Device not found, cannot connect to it");
            finish();
            return;  // TODO: test this failure path
        }

        String deviceName = mCachedDevice.getName();
        mDeviceNamePref.setSummary(deviceName);
        mDeviceNamePref.setText(deviceName);
        mDeviceNamePref.setOnPreferenceChangeListener(this);

        // Add a preference for each profile
        addPreferencesForProfiles();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDisconnectDialog != null) {
            mDisconnectDialog.dismiss();
            mDisconnectDialog = null;
        }
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
        if(mCachedDevice.getBondState() == BluetoothDevice.BOND_NONE)
            finish();
        refresh();
        EditText et = mDeviceNamePref.getEditText();
        if (et != null) {
            et.addTextChangedListener(mRenameDeviceNamePref);
            Dialog d = mDeviceNamePref.getDialog();
            if (d instanceof AlertDialog) {
                Button b = ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE);
                b.setEnabled(et.getText().length() > 0);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mCachedDevice.unregisterCallback(this);
        mManager.setForegroundActivity(null);
    }

    private void addPreferencesForProfiles() {
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {
            Preference pref = createProfilePreference(profile);
            mProfileContainer.addPreference(pref);
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
        pref.setKey(profile.toString());
        pref.setTitle(profile.getNameResource(mCachedDevice.getDevice()));
        pref.setPersistent(false);
        pref.setOrder(getProfilePreferenceIndex(profile.getOrdinal()));
        pref.setOnPreferenceChangeListener(this);

        int iconResource = profile.getDrawableResource(mCachedDevice.getBtClass());
        if (iconResource != 0) {
            pref.setIcon(getResources().getDrawable(iconResource));
        }

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
        if (key.equals(KEY_UNPAIR)) {
            unpairDevice();
            finish();
            return true;
        }

        return super.onPreferenceTreeClick(screen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDeviceNamePref) {
            mCachedDevice.setAliasName((String) newValue);
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

        int status = profile.getConnectionStatus(device);
        boolean isConnected =
                status == BluetoothProfile.STATE_CONNECTED;
        if (isConnected) {
            askDisconnect(getActivity(), profile);
        } else {
            if (profile.isPreferred(device)) {
                // profile is preferred but not connected: disable auto-connect
                if (profile instanceof PanProfile) {
                    mCachedDevice.connectProfile(profile);
                } else {
                    profile.setPreferred(device, false);
                    refreshProfilePreference(profilePref, profile);
                }
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
                // Disconnect only when user has selected OK
                if (which == OK_BUTTON) {
                    device.disconnect(profile);
                    profile.setPreferred(device.getDevice(), false);
                }
            }
        };

        mDisconnectDialog = Utils.showDisconnectDialog(context,
                mDisconnectDialog, disconnectListener, title, Html.fromHtml(message));
    }

    public void onDeviceAttributesChanged() {
        refresh();
    }

    private void refresh() {
        String deviceName = mCachedDevice.getName();
        mDeviceNamePref.setSummary(deviceName);
        mDeviceNamePref.setText(deviceName);

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

        /*
         * Gray out checkbox while connecting and disconnecting
         */
        profilePref.setEnabled(!mCachedDevice.isBusy());
        if (profile instanceof PanProfile) {
            profilePref.setChecked(profile.getConnectionStatus(device) ==
                    BluetoothProfile.STATE_CONNECTED);
        }
        else {
            profilePref.setChecked(profile.isPreferred(device));
        }
        profilePref.setSummary(profile.getSummaryResourceForDevice(device));
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

    private void unpairDevice() {
        mCachedDevice.unpair();
    }
}
