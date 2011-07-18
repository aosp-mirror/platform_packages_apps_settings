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
        implements CachedBluetoothDevice.Callback, Preference.OnPreferenceChangeListener,
                View.OnClickListener {
    private static final String TAG = "DeviceProfilesSettings";

    private static final String KEY_TITLE = "title";
    private static final String KEY_RENAME_DEVICE = "rename_device";
    private static final String KEY_PROFILE_CONTAINER = "profile_container";
    private static final String KEY_UNPAIR = "unpair";
    //private static final String KEY_ALLOW_INCOMING = "allow_incoming";

    public static final String EXTRA_DEVICE = "device";
    private RenameEditTextPreference mRenameDeviceNamePref;
    private LocalBluetoothManager mManager;
    private CachedBluetoothDevice mCachedDevice;
    private LocalBluetoothProfileManager mProfileManager;

    private PreferenceGroup mProfileContainer;
    private EditTextPreference mDeviceNamePref;

    private final HashMap<LocalBluetoothProfile, CheckBoxPreference> mAutoConnectPrefs
            = new HashMap<LocalBluetoothProfile, CheckBoxPreference>();

    private AlertDialog mDisconnectDialog;
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

        // Set the title of the screen
        findPreference(KEY_TITLE).setTitle(
                getString(R.string.bluetooth_device_advanced_title,
                        deviceName));

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
    }

    /**
     * Creates a checkbox preference for the particular profile. The key will be
     * the profile's name.
     *
     * @param profile The profile for which the preference controls.
     * @return A preference that allows the user to choose whether this profile
     *         will be connected to.
     */
    private Preference createProfilePreference(LocalBluetoothProfile profile) {
        BluetoothProfilePreference pref = new BluetoothProfilePreference(getActivity(), profile);
        pref.setKey(profile.toString());
        pref.setTitle(profile.getNameResource());
        pref.setExpanded(false);
        pref.setPersistent(false);
        pref.setOrder(getProfilePreferenceIndex(profile.getOrdinal()));
        pref.setOnExpandClickListener(this);

        int iconResource = profile.getDrawableResource(mCachedDevice.getBtClass());
        if (iconResource != 0) {
            pref.setProfileDrawable(getResources().getDrawable(iconResource));
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
        if (preference instanceof BluetoothProfilePreference) {
            onProfileClicked(mProfileManager.getProfileByName(key));
            return true;
        } else if (key.equals(KEY_UNPAIR)) {
            unpairDevice();
            finish();
            return true;
        }

        return super.onPreferenceTreeClick(screen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDeviceNamePref) {
            mCachedDevice.setName((String) newValue);
        } else if (preference instanceof CheckBoxPreference) {
            boolean autoConnect = (Boolean) newValue;
            LocalBluetoothProfile prof = getProfileOf(preference);
            prof.setPreferred(mCachedDevice.getDevice(),
                            autoConnect);
            return true;
        } else {
            return false;
        }

        return true;
    }

    private void onProfileClicked(LocalBluetoothProfile profile) {
        BluetoothDevice device = mCachedDevice.getDevice();

        int status = profile.getConnectionStatus(device);
        boolean isConnected =
                status == BluetoothProfile.STATE_CONNECTED;

        if (isConnected) {
            askDisconnect(getActivity(), profile);
        } else {
            mCachedDevice.connectProfile(profile);
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
        int disconnectMessage = profile.getDisconnectResource(device.getDevice());
        if (disconnectMessage == 0) {
            Log.w(TAG, "askDisconnect: unexpected profile " + profile);
            disconnectMessage = R.string.bluetooth_disconnect_blank;
        }
        String message = context.getString(disconnectMessage, name);

        DialogInterface.OnClickListener disconnectListener =
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                device.disconnect(profile);
            }
        };

        mDisconnectDialog = Utils.showDisconnectDialog(context,
                mDisconnectDialog, disconnectListener, name, message);
    }

    public void onDeviceAttributesChanged() {
        refresh();
    }

    private void refresh() {
        String deviceName = mCachedDevice.getName();
        // TODO: figure out how to update "bread crumb" title in action bar
//        FragmentTransaction transaction = getFragmentManager().openTransaction();
//        transaction.setBreadCrumbTitle(deviceName);
//        transaction.commit();

        findPreference(KEY_TITLE).setTitle(getString(
                R.string.bluetooth_device_advanced_title,
                deviceName));
        mDeviceNamePref = (EditTextPreference) findPreference(KEY_RENAME_DEVICE);
        mDeviceNamePref.setSummary(deviceName);
        mDeviceNamePref.setText(deviceName);

        refreshProfiles();
    }

    private void refreshProfiles() {
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {
            Preference profilePref = findPreference(profile.toString());
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
    }

    private void refreshProfilePreference(Preference profilePref, LocalBluetoothProfile profile) {
        BluetoothDevice device = mCachedDevice.getDevice();

        /*
         * Gray out checkbox while connecting and disconnecting
         */
        profilePref.setEnabled(!mCachedDevice.isBusy());
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

    public void onClick(View v) {
        if (v.getTag() instanceof LocalBluetoothProfile) {
            LocalBluetoothProfile prof = (LocalBluetoothProfile) v.getTag();
            CheckBoxPreference autoConnectPref = mAutoConnectPrefs.get(prof);
            if (autoConnectPref == null) {
                autoConnectPref = new CheckBoxPreference(getActivity());
                autoConnectPref.setLayoutResource(com.android.internal.R.layout.preference_child);
                autoConnectPref.setKey(prof.toString());
                autoConnectPref.setTitle(R.string.bluetooth_auto_connect);
                autoConnectPref.setOrder(getProfilePreferenceIndex(prof.getOrdinal()) + 1);
                autoConnectPref.setChecked(getAutoConnect(prof));
                autoConnectPref.setOnPreferenceChangeListener(this);
                mAutoConnectPrefs.put(prof, autoConnectPref);
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

    private int getProfilePreferenceIndex(int profIndex) {
        return mProfileContainer.getOrder() + profIndex * 10;
    }

    private void unpairDevice() {
        mCachedDevice.unpair();
    }

    /*
    private void setIncomingFileTransfersAllowed(boolean allow) {
        // TODO: make an IPC call into BluetoothOpp to update
        Log.d(TAG, "Set allow incoming = " + allow);
    }

    private boolean isIncomingFileTransfersAllowed() {
        // TODO: get this value from BluetoothOpp ???
        return true;
    }
    */

    private boolean getAutoConnect(LocalBluetoothProfile prof) {
        return prof.isPreferred(mCachedDevice.getDevice());
    }
}
