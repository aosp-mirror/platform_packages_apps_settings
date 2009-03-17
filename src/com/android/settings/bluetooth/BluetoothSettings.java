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

import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothManager.ExtendedBluetoothState;

import java.util.List;
import java.util.WeakHashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public class BluetoothSettings extends PreferenceActivity
        implements LocalBluetoothManager.Callback {

    private static final String TAG = "BluetoothSettings";

    private static final int MENU_SCAN = Menu.FIRST;
    
    private static final String KEY_BT_CHECKBOX = "bt_checkbox";
    private static final String KEY_BT_DISCOVERABLE = "bt_discoverable";
    private static final String KEY_BT_DEVICE_LIST = "bt_device_list";
    private static final String KEY_BT_NAME = "bt_name";
    private static final String KEY_BT_SCAN = "bt_scan";
    
    private LocalBluetoothManager mLocalManager;
    
    private BluetoothEnabler mEnabler;
    private BluetoothDiscoverableEnabler mDiscoverableEnabler;
    
    private BluetoothNamePreference mNamePreference;
    
    private ProgressCategory mDeviceList;
    
    private WeakHashMap<LocalBluetoothDevice, BluetoothDevicePreference> mDevicePreferenceMap =
            new WeakHashMap<LocalBluetoothDevice, BluetoothDevicePreference>();
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: put this in callback instead of receiving
            onBluetoothStateChanged(mLocalManager.getBluetoothState());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalManager = LocalBluetoothManager.getInstance(this);
        if (mLocalManager == null) finish();        
        
        addPreferencesFromResource(R.xml.bluetooth_settings);
        
        mEnabler = new BluetoothEnabler(
                this,
                (CheckBoxPreference) findPreference(KEY_BT_CHECKBOX));
        
        mDiscoverableEnabler = new BluetoothDiscoverableEnabler(
                this,
                (CheckBoxPreference) findPreference(KEY_BT_DISCOVERABLE));
    
        mNamePreference = (BluetoothNamePreference) findPreference(KEY_BT_NAME);
        
        mDeviceList = (ProgressCategory) findPreference(KEY_BT_DEVICE_LIST);
        
        registerForContextMenu(getListView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Repopulate (which isn't too bad since it's cached in the settings
        // bluetooth manager
        mDevicePreferenceMap.clear();
        mDeviceList.removeAll();
        addDevices();

        mEnabler.resume();
        mDiscoverableEnabler.resume();
        mNamePreference.resume();
        mLocalManager.registerCallback(this);
        
        mLocalManager.startScanning(false);

        registerReceiver(mReceiver, 
                new IntentFilter(LocalBluetoothManager.EXTENDED_BLUETOOTH_STATE_CHANGED_ACTION));
        
        mLocalManager.setForegroundActivity(this);
    }
    
    @Override
    protected void onPause() {
        super.onPause();

        mLocalManager.setForegroundActivity(null);
        
        unregisterReceiver(mReceiver);
        
        mLocalManager.unregisterCallback(this);
        mNamePreference.pause();
        mDiscoverableEnabler.pause();
        mEnabler.pause();
    }

    private void addDevices() {
        List<LocalBluetoothDevice> devices = mLocalManager.getLocalDeviceManager().getDevicesCopy();
        for (LocalBluetoothDevice device : devices) {
            onDeviceAdded(device);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SCAN, 0, R.string.bluetooth_scan_for_devices)
                .setIcon(com.android.internal.R.drawable.ic_menu_refresh)
                .setAlphabeticShortcut('r');
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(MENU_SCAN).setEnabled(mLocalManager.getBluetoothManager().isEnabled());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
            case MENU_SCAN:
                mLocalManager.startScanning(true);
                return true;
                
            default:
                return false;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        if (KEY_BT_SCAN.equals(preference.getKey())) {
            mLocalManager.startScanning(true);
            return true;
        }
        
        if (preference instanceof BluetoothDevicePreference) {
            BluetoothDevicePreference btPreference = (BluetoothDevicePreference) preference;
            btPreference.getDevice().onClicked();
            return true;
        }
        
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        LocalBluetoothDevice device = getDeviceFromMenuInfo(menuInfo);
        if (device == null) return;
        
        device.onCreateContextMenu(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        LocalBluetoothDevice device = getDeviceFromMenuInfo(item.getMenuInfo());
        if (device == null) return false;
        
        device.onContextItemSelected(item);
        return true;
    }

    private LocalBluetoothDevice getDeviceFromMenuInfo(ContextMenuInfo menuInfo) {
        if ((menuInfo == null) || !(menuInfo instanceof AdapterContextMenuInfo)) {
            return null;
        }
        
        AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(
                adapterMenuInfo.position);
        if (pref == null || !(pref instanceof BluetoothDevicePreference)) {
            return null;
        }

        return ((BluetoothDevicePreference) pref).getDevice();
    }
    
    public void onDeviceAdded(LocalBluetoothDevice device) {

        if (mDevicePreferenceMap.get(device) != null) {
            throw new IllegalStateException("Got onDeviceAdded, but device already exists");
        }
        
        createDevicePreference(device);            
    }

    private void createDevicePreference(LocalBluetoothDevice device) {
        BluetoothDevicePreference preference = new BluetoothDevicePreference(this, device);
        mDeviceList.addPreference(preference);
        mDevicePreferenceMap.put(device, preference);
    }
    
    public void onDeviceDeleted(LocalBluetoothDevice device) {
        BluetoothDevicePreference preference = mDevicePreferenceMap.remove(device);
        if (preference != null) {
            mDeviceList.removePreference(preference);
        }
    }

    public void onScanningStateChanged(boolean started) {
        mDeviceList.setProgress(started);
    }
    
    private void onBluetoothStateChanged(ExtendedBluetoothState bluetoothState) {
        // When bluetooth is enabled (and we are in the activity, which we are),
        // we should start a scan
        if (bluetoothState == ExtendedBluetoothState.ENABLED) {
            mLocalManager.startScanning(false);
        } else if (bluetoothState == ExtendedBluetoothState.DISABLED) {
            mDeviceList.setProgress(false);
        }
    }
}
