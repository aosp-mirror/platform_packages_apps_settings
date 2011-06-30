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

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import com.android.settings.ProgressCategory;
import com.android.settings.R;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public final class BluetoothSettings extends DeviceListPreferenceFragment {
    private static final String TAG = "BluetoothSettings";

    private static final int MENU_ID_SCAN = Menu.FIRST;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 1;

    private BluetoothEnabler mBluetoothEnabler;

    private PreferenceGroup mFoundDevicesCategory;
    private boolean mFoundDevicesCategoryIsPresent;

    @Override
    void addPreferencesForActivity() {
        addPreferencesFromResource(R.xml.bluetooth_settings);

        Activity activity = getActivity();

        Switch actionBarSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                actionBarSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            }
        }

        mBluetoothEnabler = new BluetoothEnabler(activity, actionBarSwitch);

        if (mLocalAdapter != null && mLocalAdapter.isEnabled()) {
            activity.getActionBar().setSubtitle(mLocalAdapter.getName());
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        mBluetoothEnabler.resume();

        updateContent(mLocalAdapter.getBluetoothState());
    }

    @Override
    public void onPause() {
        super.onPause();

        mBluetoothEnabler.pause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean bluetoothIsEnabled = mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON;
        boolean isDiscovering = mLocalAdapter.isDiscovering();
        int textId = isDiscovering ? R.string.bluetooth_searching_for_devices :
            R.string.bluetooth_search_for_devices;
        menu.add(Menu.NONE, MENU_ID_SCAN, 0, textId)
                //.setIcon(R.drawable.ic_menu_scan_network)
                .setEnabled(bluetoothIsEnabled && !isDiscovering)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_ADVANCED, 0, R.string.bluetooth_menu_advanced)
                //.setIcon(android.R.drawable.ic_menu_manage)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_SCAN:
                if (mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
                    startScanning();
                }
                return true;
            case MENU_ID_ADVANCED:
                if (getActivity() instanceof PreferenceActivity) {
                    ((PreferenceActivity) getActivity()).startPreferencePanel(
                            AdvancedBluetoothSettings.class.getCanonicalName(),
                            null,
                            R.string.bluetooth_advanced_titlebar, null,
                            this, 0);
                } else {
                    startFragment(this, AdvancedBluetoothSettings.class.getCanonicalName(), -1, null);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startScanning() {
        if (!mFoundDevicesCategoryIsPresent) {
            getPreferenceScreen().addPreference(mFoundDevicesCategory);
        }
        mLocalAdapter.startScanning(true);
    }

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        mLocalAdapter.stopScanning();
        super.onDevicePreferenceClick(btPreference);
    }

    private void updateContent(int bluetoothState) {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        getActivity().invalidateOptionsMenu();
        int messageId = 0;

        switch (bluetoothState) {
            case BluetoothAdapter.STATE_ON:
                preferenceScreen.removeAll();

                // Add bonded devices from cache first
                setFilter(BluetoothDeviceFilter.BONDED_DEVICE_FILTER);
                setDeviceListGroup(preferenceScreen);
                preferenceScreen.setOrderingAsAdded(true);

                addCachedDevices();
                int numberOfPairedDevices = preferenceScreen.getPreferenceCount();

                // Found devices category
                mFoundDevicesCategory = new ProgressCategory(getActivity(), null);
                mFoundDevicesCategory.setTitle(R.string.bluetooth_preference_found_devices);
                preferenceScreen.addPreference(mFoundDevicesCategory);
                mFoundDevicesCategoryIsPresent = true;

                // Unbonded found devices from cache
                setFilter(BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER);
                setDeviceListGroup(mFoundDevicesCategory);
                addCachedDevices();

                int numberOfUnpairedDevices = mFoundDevicesCategory.getPreferenceCount();
                if (numberOfUnpairedDevices == 0) {
                    preferenceScreen.removePreference(mFoundDevicesCategory);
                    mFoundDevicesCategoryIsPresent = false;
                }

                if (numberOfPairedDevices == 0) startScanning();

                return;

            case BluetoothAdapter.STATE_TURNING_OFF:
                int preferenceCount = preferenceScreen.getPreferenceCount();
                for (int i = 0; i < preferenceCount; i++) {
                    preferenceScreen.getPreference(i).setEnabled(false);
                }
                return;

            case BluetoothAdapter.STATE_OFF:
                messageId = R.string.bluetooth_empty_list_bluetooth_off;
                break;

            case BluetoothAdapter.STATE_TURNING_ON:
                messageId = R.string.bluetooth_turning_on;
                break;
        }

        setDeviceListGroup(preferenceScreen);
        removeAllDevices();

        // TODO: from xml, add top padding. Same as in wifi
        Preference emptyListPreference = new Preference(getActivity());
        emptyListPreference.setTitle(messageId);
        preferenceScreen.addPreference(emptyListPreference);
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        updateContent(bluetoothState);
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        super.onScanningStateChanged(started);
        // Update options' enabled state
        getActivity().invalidateOptionsMenu();
    }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        setDeviceListGroup(getPreferenceScreen());
        removeAllDevices();
        updateContent(mLocalAdapter.getBluetoothState());
    }

    private final View.OnClickListener mDeviceProfilesListener = new View.OnClickListener() {
        public void onClick(View v) {
            // User clicked on advanced options icon for a device in the list
            if (v.getTag() instanceof CachedBluetoothDevice) {
                CachedBluetoothDevice device = (CachedBluetoothDevice) v.getTag();

                Preference pref = new Preference(getActivity());
                pref.setTitle(device.getName());
                pref.setFragment(DeviceProfilesSettings.class.getName());
                pref.getExtras().putParcelable(DeviceProfilesSettings.EXTRA_DEVICE,
                        device.getDevice());
                ((PreferenceActivity) getActivity()).onPreferenceStartFragment(
                        BluetoothSettings.this, pref);
            } else {
                Log.w(TAG, "onClick() called for other View: " + v); // TODO remove
            }
        }
    };

    /**
     * Add a listener, which enables the advanced settings icon.
     * @param preference the newly added preference
     */
    @Override
    void initDevicePreference(BluetoothDevicePreference preference) {
        CachedBluetoothDevice cachedDevice = preference.getCachedDevice();
        if (cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            // Only paired device have an associated advanced settings screen
            preference.setOnSettingsClickListener(mDeviceProfilesListener);
        }
    }
}
