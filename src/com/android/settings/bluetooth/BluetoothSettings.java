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
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import com.android.settings.R;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public final class BluetoothSettings extends DeviceListPreferenceFragment {
    private static final String TAG = "BluetoothSettings";

    private static final int MENU_ID_MAKE_DISCOVERABLE = Menu.FIRST;
    private static final int MENU_ID_SCAN = Menu.FIRST + 1;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 2;

    private BluetoothEnabler mBluetoothEnabler;

    /** Initialize the filter to show bonded devices only. */
    //public BluetoothSettings() {
    //    super(BluetoothDeviceFilter.BONDED_DEVICE_FILTER);
    //}

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

        // TODO activity.setTheme(android.R.style.Theme_Holo_SplitActionBarWhenNarrow);

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
        menu.add(Menu.NONE, MENU_ID_MAKE_DISCOVERABLE, 0, R.string.bluetooth_visibility)
                .setEnabled(bluetoothIsEnabled);
        menu.add(Menu.NONE, MENU_ID_SCAN, 0, R.string.bluetooth_preference_find_nearby_title)
                .setIcon(R.drawable.ic_menu_scan_network).setEnabled(bluetoothIsEnabled);
        menu.add(Menu.NONE, MENU_ID_ADVANCED, 0, R.string.bluetooth_menu_advanced)
                .setIcon(android.R.drawable.ic_menu_manage);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_MAKE_DISCOVERABLE:
                // TODO
//                if (mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
//                    onAddNetworkPressed();
//                }
                return true;
            case MENU_ID_SCAN:
                if (mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
                    mLocalAdapter.startScanning(true);
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

    private final View.OnClickListener mListener = new View.OnClickListener() {
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
                Log.w(TAG, "onClick() called for other View: " + v);
            }
        }
    };

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        mLocalAdapter.stopScanning();
        super.onDevicePreferenceClick(btPreference);
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        updateContent(bluetoothState);
    }

    private void updateContent(int bluetoothState) {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        getActivity().invalidateOptionsMenu();
        int messageId = 0;

        switch (bluetoothState) {
            case BluetoothAdapter.STATE_ON:
                preferenceScreen.removeAll();
                // Repopulate (which isn't too bad since it's cached in the settings bluetooth manager)
                addDevices();
                mLocalAdapter.startScanning(false);
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

        removeAllDevices();
        // TODO: from xml, add top padding. Same as in wifi
        Preference emptyListPreference = new Preference(getActivity());
        emptyListPreference.setTitle(messageId);
        preferenceScreen.addPreference(emptyListPreference);
    }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        if (bondState == BluetoothDevice.BOND_BONDED) {
            // add to "Paired devices" list after remote-initiated pairing
            if (mDevicePreferenceMap.get(cachedDevice) == null) {
                createDevicePreference(cachedDevice);
            }
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            // remove unpaired device from paired devices list
            onDeviceDeleted(cachedDevice);
        }
    }

    /**
     * Add a listener, which enables the advanced settings icon.
     * @param preference the newly added preference
     */
    @Override
    void initDevicePreference(BluetoothDevicePreference preference) {
        preference.setOnSettingsClickListener(mListener);
    }
}
