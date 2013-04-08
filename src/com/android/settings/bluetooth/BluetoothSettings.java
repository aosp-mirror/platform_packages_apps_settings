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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public final class BluetoothSettings extends DeviceListPreferenceFragment {
    private static final String TAG = "BluetoothSettings";

    private static final int MENU_ID_SCAN = Menu.FIRST;
    private static final int MENU_ID_RENAME_DEVICE = Menu.FIRST + 1;
    private static final int MENU_ID_VISIBILITY_TIMEOUT = Menu.FIRST + 2;
    private static final int MENU_ID_SHOW_RECEIVED = Menu.FIRST + 3;

    /* Private intent to show the list of received files */
    private static final String BTOPP_ACTION_OPEN_RECEIVED_FILES =
            "android.btopp.intent.action.OPEN_RECEIVED_FILES";

    private BluetoothEnabler mBluetoothEnabler;

    private BluetoothDiscoverableEnabler mDiscoverableEnabler;

    private PreferenceGroup mPairedDevicesCategory;

    private PreferenceGroup mAvailableDevicesCategory;
    private boolean mAvailableDevicesCategoryIsPresent;
    private boolean mActivityStarted;

    private TextView mEmptyView;

    private final IntentFilter mIntentFilter;

    private UserManager mUserManager;

    // accessed from inner class (not private to avoid thunks)
    Preference mMyDevicePreference;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
                updateDeviceName();
            }
        }

        private void updateDeviceName() {
            if (mLocalAdapter.isEnabled() && mMyDevicePreference != null) {
                mMyDevicePreference.setTitle(mLocalAdapter.getName());
            }
        }
    };

    public BluetoothSettings() {
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);
        mActivityStarted = (savedInstanceState == null);    // don't auto start scan after rotation

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        getListView().setEmptyView(mEmptyView);
    }

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
                actionBarSwitch.setPaddingRelative(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));
            }
        }

        mBluetoothEnabler = new BluetoothEnabler(activity, actionBarSwitch);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        // resume BluetoothEnabler before calling super.onResume() so we don't get
        // any onDeviceAdded() callbacks before setting up view in updateContent()
        if (mBluetoothEnabler != null) {
            mBluetoothEnabler.resume();
        }
        super.onResume();

        if (mDiscoverableEnabler != null) {
            mDiscoverableEnabler.resume();
        }
        getActivity().registerReceiver(mReceiver, mIntentFilter);
        if (mLocalAdapter != null) {
            updateContent(mLocalAdapter.getBluetoothState(), mActivityStarted);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBluetoothEnabler != null) {
            mBluetoothEnabler.pause();
        }
        getActivity().unregisterReceiver(mReceiver);
        if (mDiscoverableEnabler != null) {
            mDiscoverableEnabler.pause();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mLocalAdapter == null) return;
        // If the user is not allowed to configure bluetooth, do not show the menu.
        if (mUserManager.hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH)) return;

        boolean bluetoothIsEnabled = mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON;
        boolean isDiscovering = mLocalAdapter.isDiscovering();
        int textId = isDiscovering ? R.string.bluetooth_searching_for_devices :
            R.string.bluetooth_search_for_devices;
        menu.add(Menu.NONE, MENU_ID_SCAN, 0, textId)
                .setEnabled(bluetoothIsEnabled && !isDiscovering)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_RENAME_DEVICE, 0, R.string.bluetooth_rename_device)
                .setEnabled(bluetoothIsEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_VISIBILITY_TIMEOUT, 0, R.string.bluetooth_visibility_timeout)
                .setEnabled(bluetoothIsEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_SHOW_RECEIVED, 0, R.string.bluetooth_show_received_files)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_SCAN:
                if (mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
                    startScanning();
                }
                return true;

            case MENU_ID_RENAME_DEVICE:
                new BluetoothNameDialogFragment().show(
                        getFragmentManager(), "rename device");
                return true;

            case MENU_ID_VISIBILITY_TIMEOUT:
                new BluetoothVisibilityTimeoutFragment().show(
                        getFragmentManager(), "visibility timeout");
                return true;

            case MENU_ID_SHOW_RECEIVED:
                Intent intent = new Intent(BTOPP_ACTION_OPEN_RECEIVED_FILES);
                getActivity().sendBroadcast(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startScanning() {
        if (mUserManager.hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH)) return;
        if (!mAvailableDevicesCategoryIsPresent) {
            getPreferenceScreen().addPreference(mAvailableDevicesCategory);
        }
        mLocalAdapter.startScanning(true);
    }

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        mLocalAdapter.stopScanning();
        super.onDevicePreferenceClick(btPreference);
    }

    private void addDeviceCategory(PreferenceGroup preferenceGroup, int titleId,
            BluetoothDeviceFilter.Filter filter) {
        preferenceGroup.setTitle(titleId);
        getPreferenceScreen().addPreference(preferenceGroup);
        setFilter(filter);
        setDeviceListGroup(preferenceGroup);
        addCachedDevices();
        preferenceGroup.setEnabled(true);
    }

    private void updateContent(int bluetoothState, boolean scanState) {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        int messageId = 0;

        switch (bluetoothState) {
            case BluetoothAdapter.STATE_ON:
                preferenceScreen.removeAll();
                preferenceScreen.setOrderingAsAdded(true);
                mDevicePreferenceMap.clear();

                // This device
                if (mMyDevicePreference == null) {
                    mMyDevicePreference = new Preference(getActivity());
                }
                mMyDevicePreference.setTitle(mLocalAdapter.getName());
                if (getResources().getBoolean(com.android.internal.R.bool.config_voice_capable)) {
                    mMyDevicePreference.setIcon(R.drawable.ic_bt_cellphone);    // for phones
                } else {
                    mMyDevicePreference.setIcon(R.drawable.ic_bt_laptop);   // for tablets, etc.
                }
                mMyDevicePreference.setPersistent(false);
                mMyDevicePreference.setEnabled(true);
                preferenceScreen.addPreference(mMyDevicePreference);

                if (! mUserManager.hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH)) {
                    if (mDiscoverableEnabler == null) {
                        mDiscoverableEnabler = new BluetoothDiscoverableEnabler(getActivity(),
                                mLocalAdapter, mMyDevicePreference);
                        mDiscoverableEnabler.resume();
                        LocalBluetoothManager.getInstance(getActivity()).setDiscoverableEnabler(
                                mDiscoverableEnabler);
                    }
                }

                // Paired devices category
                if (mPairedDevicesCategory == null) {
                    mPairedDevicesCategory = new PreferenceCategory(getActivity());
                } else {
                    mPairedDevicesCategory.removeAll();
                }
                addDeviceCategory(mPairedDevicesCategory,
                        R.string.bluetooth_preference_paired_devices,
                        BluetoothDeviceFilter.BONDED_DEVICE_FILTER);
                int numberOfPairedDevices = mPairedDevicesCategory.getPreferenceCount();

                if (mDiscoverableEnabler != null) {
                    mDiscoverableEnabler.setNumberOfPairedDevices(numberOfPairedDevices);
                }

                // Available devices category
                if (mAvailableDevicesCategory == null) {
                    mAvailableDevicesCategory = new BluetoothProgressCategory(getActivity(), null);
                } else {
                    mAvailableDevicesCategory.removeAll();
                }
                if (! mUserManager.hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH)) {
                    addDeviceCategory(mAvailableDevicesCategory,
                            R.string.bluetooth_preference_found_devices,
                            BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER);
                }
                int numberOfAvailableDevices = mAvailableDevicesCategory.getPreferenceCount();
                mAvailableDevicesCategoryIsPresent = true;

                if (numberOfAvailableDevices == 0) {
                    preferenceScreen.removePreference(mAvailableDevicesCategory);
                    mAvailableDevicesCategoryIsPresent = false;
                }

                if (numberOfPairedDevices == 0) {
                    preferenceScreen.removePreference(mPairedDevicesCategory);
                    if (scanState == true) {
                        mActivityStarted = false;
                        startScanning();
                    } else {
                        if (!mAvailableDevicesCategoryIsPresent) {
                            getPreferenceScreen().addPreference(mAvailableDevicesCategory);
                        }
                    }
                }
                getActivity().invalidateOptionsMenu();
                return; // not break

            case BluetoothAdapter.STATE_TURNING_OFF:
                messageId = R.string.bluetooth_turning_off;
                break;

            case BluetoothAdapter.STATE_OFF:
                messageId = R.string.bluetooth_empty_list_bluetooth_off;
                break;

            case BluetoothAdapter.STATE_TURNING_ON:
                messageId = R.string.bluetooth_turning_on;
                break;
        }

        setDeviceListGroup(preferenceScreen);
        removeAllDevices();
        mEmptyView.setText(messageId);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        updateContent(bluetoothState, true);
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
        updateContent(mLocalAdapter.getBluetoothState(), false);
    }

    private final View.OnClickListener mDeviceProfilesListener = new View.OnClickListener() {
        public void onClick(View v) {
            // User clicked on advanced options icon for a device in the list
            if (v.getTag() instanceof CachedBluetoothDevice) {
                if (mUserManager.hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH)) return;

                CachedBluetoothDevice device = (CachedBluetoothDevice) v.getTag();

                Bundle args = new Bundle(1);
                args.putParcelable(DeviceProfilesSettings.EXTRA_DEVICE, device.getDevice());

                ((PreferenceActivity) getActivity()).startPreferencePanel(
                        DeviceProfilesSettings.class.getName(), args,
                        R.string.bluetooth_device_advanced_title, null, null, 0);
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

    @Override
    protected int getHelpResource() {
        return R.string.help_url_bluetooth;
    }
}
