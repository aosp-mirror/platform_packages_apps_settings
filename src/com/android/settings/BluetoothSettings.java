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

package com.android.settings;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothIntent;
import android.bluetooth.DeviceClass;
import android.bluetooth.IBluetoothDeviceCallback;
import android.bluetooth.IBluetoothHeadsetCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.util.HashMap;

public class BluetoothSettings
        extends PreferenceActivity
        implements OnSharedPreferenceChangeListener, OnKeyListener,
                View.OnCreateContextMenuListener {

    private static final String TAG = "BluetoothSettings";

    private static final int MENU_SCAN_ID = Menu.FIRST;
    private static final int MENU_CLEAR_ID = Menu.FIRST + 1;
    
    private static final int MENU_CONNECT = ContextMenu.FIRST;
    private static final int MENU_DISCONNECT = ContextMenu.FIRST + 1;
    private static final int MENU_PAIR = ContextMenu.FIRST + 2;
    private static final int MENU_UNPAIR = ContextMenu.FIRST + 3;
    
    private static final String BT_ENABLE = "bt_checkbox";
    private static final String BT_VISIBILITY = "bt_visibility";
    private static final String BT_NAME = "bt_name";
    
    private static final String BT_KEY_PREFIX = "bt_dev_";
    private static final int BT_KEY_LENGTH = BT_KEY_PREFIX.length();
    private static final String FREEZE_ADDRESSES = "addresses";
    private static final String FREEZE_TYPES = "types";
    private static final String FREEZE_PIN = "pinText";
    private static final String FREEZE_PIN_ADDRESS = "pinAddress";
    private static final String FREEZE_RSSI = "rssi";
    private static final String FREEZE_DISCOVERABLE_START = "dstart";
    
    private static final int HANDLE_FAILED_TO_CONNECT = 1;
    private static final int HANDLE_CONNECTING = 2;
    private static final int HANDLE_CONNECTED = 3;
    private static final int HANDLE_DISCONNECTED = 4;
    private static final int HANDLE_PIN_REQUEST = 5;
    private static final int HANDLE_DISCOVERABLE_TIMEOUT = 6;
    private static final int HANDLE_INITIAL_SCAN = 7;
    private static final int HANDLE_PAIRING_FAILED = 8;
    private static final int HANDLE_PAIRING_PASSED = 9;
    private static final int HANDLE_PAUSE_TIMEOUT = 10;
    

    
    private static String STR_CONNECTED;
    private static String STR_PAIRED;
    private static String STR_PAIRED_NOT_NEARBY;
    private static String STR_NOT_CONNECTED;
    private static String STR_CONNECTING;
    private static String STR_PAIRING;
    
    private static final int WEIGHT_CONNECTED = 1;
    private static final int WEIGHT_PAIRED = 0;
    private static final int WEIGHT_UNKNOWN = -1;
    
    private CheckBoxPreference mBTToggle;
    private CheckBoxPreference mBTVisibility;
    private EditTextPreference mBTName;
    private ProgressCategory mBTDeviceList;
    private AlertDialog mPinDialog;
    private String      mPinAddress;
    private EditText    mPinEdit;
    private View        mPinButton1;
    private String      mDisconnectAddress;
    
    private BluetoothDevice mBluetooth;
    private BluetoothHeadset mBluetoothHeadset;
    private boolean mIsEnabled;
    private String mLastConnected;
    private static boolean sIsRunning;
    private static DeviceCallback sDeviceCallback;    
    private IntentFilter mIntentFilter;
    private Resources mRes;
    private long mDiscoverableStartTime;
    private int mDiscoverableTime;
    private static final String DISCOVERABLE_TIME = "debug.bt.discoverable_time";
    private static final int DISCOVERABLE_TIME_DEFAULT = 120;
    private boolean mAutoDiscovery;
    // After a few seconds after a pause, if the user doesn't restart the 
    // BT settings, then we need to cleanup a few things in the message handler
    private static final int PAUSE_TIMEOUT = 3000;
    
    private boolean mStartScan;
    private static final String AUTO_DISCOVERY = "debug.bt.auto_discovery";
    private HashMap<String,Preference> mDeviceMap;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        addPreferencesFromResource(R.xml.bluetooth_settings);
    
        // Deal with restarted activities by passing a static callback object to
        // the Bluetooth service
        if (sDeviceCallback == null) {
            sDeviceCallback = new DeviceCallback();
        }
        sDeviceCallback.setHandler(mHandler);
        
        mDiscoverableTime = SystemProperties.getInt(DISCOVERABLE_TIME, -1);
        if (mDiscoverableTime <= 0) {
            mDiscoverableTime= DISCOVERABLE_TIME_DEFAULT;
        }
        mAutoDiscovery = SystemProperties.getBoolean(AUTO_DISCOVERY, true);

        if (!initBluetoothAPI()) {
            finish();
            return;
        }
        initUI();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
        mDeviceMap = new HashMap<String,Preference>();
        if (icicle != null && icicle.containsKey(FREEZE_ADDRESSES)) {
            addDevices(icicle.getStringArray(FREEZE_ADDRESSES), 
                    icicle.getStringArray(FREEZE_TYPES), 
                    icicle.getIntArray(FREEZE_RSSI));
            if (icicle.containsKey(FREEZE_PIN)) {
                String savedPin = icicle.getString(FREEZE_PIN);
                String pinAddress = icicle.getString(FREEZE_PIN_ADDRESS);
                mPinDialog = showPinDialog(savedPin, pinAddress);
            }
            mDiscoverableStartTime = icicle.getLong(FREEZE_DISCOVERABLE_START);
        } else {
            mStartScan = true;
        }
    }

    private void initUI() {
        mBTToggle = (CheckBoxPreference) findPreference(BT_ENABLE);
        mBTVisibility = (CheckBoxPreference) findPreference(BT_VISIBILITY);
        mBTName = (EditTextPreference) findPreference(BT_NAME);
        mBTDeviceList = (ProgressCategory) findPreference("bt_device_list");
        mBTDeviceList.setOrderingAsAdded(false);
        mRes = getResources();
        if (mIsEnabled) {
            String name = mBluetooth.getName();
            if (name != null) {
                mBTName.setSummary(name);
            }
        }
        mBTVisibility.setEnabled(mIsEnabled);
        mBTName.setEnabled(mIsEnabled);
        STR_CONNECTED = mRes.getString(R.string.bluetooth_connected);
        STR_PAIRED = mRes.getString(R.string.bluetooth_paired);
        STR_PAIRED_NOT_NEARBY = 
            mRes.getString(R.string.bluetooth_paired_not_nearby);
        STR_CONNECTING = mRes.getString(R.string.bluetooth_connecting);
        STR_PAIRING = mRes.getString(R.string.bluetooth_pairing);
        STR_NOT_CONNECTED = mRes.getString(R.string.bluetooth_not_connected);
        getListView().setOnCreateContextMenuListener(this);
    }
    
    private boolean initBluetoothAPI() {
        mIntentFilter =
            new IntentFilter(BluetoothIntent.REMOTE_DEVICE_CONNECTED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.REMOTE_DEVICE_DISCONNECTED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.BONDING_CREATED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.ENABLED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.DISABLED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.REMOTE_DEVICE_FOUND_ACTION);
        mIntentFilter.addAction(BluetoothIntent.REMOTE_DEVICE_DISAPPEARED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.REMOTE_NAME_UPDATED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.PAIRING_REQUEST_ACTION);
        mIntentFilter.addAction(BluetoothIntent.HEADSET_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.DISCOVERY_COMPLETED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.DISCOVERY_STARTED_ACTION);
        mIntentFilter.addAction(BluetoothIntent.MODE_CHANGED_ACTION);
        
        mBluetooth = (BluetoothDevice)getSystemService(BLUETOOTH_SERVICE);
        mBluetoothHeadset = new BluetoothHeadset(this);
        if (mBluetooth == null) { // If the environment doesn't support BT
            return false;
        }
        mIsEnabled = mBluetooth.isEnabled();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        sIsRunning = true;
        mHandler.removeMessages(HANDLE_PAUSE_TIMEOUT);
        registerReceiver(mReceiver, mIntentFilter);

        mIsEnabled = mBluetooth.isEnabled();
        updateStatus();
        final boolean discoverable = mBluetooth.getMode() == 
            BluetoothDevice.MODE_DISCOVERABLE;
        mBTDeviceList.setProgress(mIsEnabled && mBluetooth.isDiscovering());
        mBTVisibility.setChecked(mIsEnabled && discoverable);
        
        if (discoverable) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(HANDLE_DISCOVERABLE_TIMEOUT));            
        }
        
        if (mIsEnabled && mStartScan) {
            // First attempt after 100ms
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(HANDLE_INITIAL_SCAN, 1), 100);
        }
        mStartScan = false;
        
        // Check if headset status changed since we paused
        String connected = mBluetoothHeadset.getHeadsetAddress();
        if (connected != null) {
            updateRemoteDeviceStatus(connected);
        }
        if (mLastConnected != null) {
            updateRemoteDeviceStatus(mLastConnected);
        }
    }
    
    @Override
    protected void onPause() {
        sIsRunning = false;

        unregisterReceiver(mReceiver);

        // Wait for a few seconds and cleanup any pending requests, states
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(HANDLE_PAUSE_TIMEOUT, 
                        new Object[] { mBluetooth, mPinAddress }), 
                PAUSE_TIMEOUT);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mBluetoothHeadset.close();
        sDeviceCallback.setHandler(null);
        
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        // Don't do anything on keyboardHidden/orientation change, as we need
        // to make sure that we don't lose pairing request intents.
    }
    
    public static boolean isRunning() {
        return sIsRunning;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        int deviceCount = mBTDeviceList.getPreferenceCount();
        String [] addresses = new String[deviceCount];
        String [] states = new String[deviceCount];
        int [] weights = new int[deviceCount];
        for (int i = 0; i < deviceCount; i++) {
            BluetoothListItem p = (BluetoothListItem) mBTDeviceList.getPreference(i);
            CharSequence summary = p.getSummary();
            if (summary != null) {
                states[i] = summary.toString();
            } else {
                states[i] = STR_NOT_CONNECTED;
            }
            addresses[i] = getAddressFromKey(p.getKey());
            weights[i] = p.getWeight();
        }
        icicle.putStringArray(FREEZE_ADDRESSES, addresses);
        icicle.putStringArray(FREEZE_TYPES, states);
        icicle.putIntArray(FREEZE_RSSI, weights);
        icicle.putLong(FREEZE_DISCOVERABLE_START, mDiscoverableStartTime);
        if (mPinDialog != null && mPinDialog.isShowing()) {
            icicle.putString(FREEZE_PIN, mPinEdit.getText().toString());
            icicle.putString(FREEZE_PIN_ADDRESS, mPinAddress);
        }
        super.onSaveInstanceState(icicle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SCAN_ID, 0, 
                mRes.getString(R.string.bluetooth_scan_for_devices))
            .setIcon(R.drawable.ic_menu_scan_bluetooth);
        menu.add(0, MENU_CLEAR_ID, 0, 
                mRes.getString(R.string.bluetooth_clear_list))
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SCAN_ID:
            startScanning();
            return true;
        case MENU_CLEAR_ID:
            clearDevices();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        if (!(menuInfo instanceof AdapterContextMenuInfo)) {
            return;
        }
        int position = ((AdapterContextMenuInfo)menuInfo).position;
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(position);
        if (!(pref instanceof BluetoothListItem)) {
            return;
        }
        String address = getAddressFromKey(pref.getKey());
        // Setup the menu header
        String name = mBluetooth.getRemoteName(address);
        menu.setHeaderTitle(name != null? name : address);
        int n = 0;
        if (mBluetoothHeadset.isConnected(address)) {
            menu.add(0, MENU_DISCONNECT, n++, R.string.bluetooth_disconnect);            
        } else {
            menu.add(0, MENU_CONNECT, n++, R.string.bluetooth_connect);
        }
        if (mBluetooth.hasBonding(address)) {
            menu.add(0, MENU_UNPAIR, n++, R.string.bluetooth_unpair);
        } else {
            menu.add(0, MENU_PAIR, n++, R.string.bluetooth_pair);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        if (!(item.getMenuInfo() instanceof AdapterContextMenuInfo)) {
            return false;
        }
        info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().
                getItem(info.position);
        String address = getAddressFromKey(pref.getKey());
        mBluetooth.cancelDiscovery();
        switch (item.getItemId()) {
            case MENU_DISCONNECT:
                if (mBluetoothHeadset.isConnected(address)) {
                    mBluetoothHeadset.disconnectHeadset();
                }
                break;
            case MENU_CONNECT:
                if (!mBluetoothHeadset.isConnected(address)) {
                    updateRemoteDeviceStatus(address, STR_CONNECTING); 
                    connect(pref, address);
                }
                break;
            case MENU_UNPAIR:
                if (mBluetooth.hasBonding(address)) {
                    mBluetooth.removeBonding(address);
                    updateRemoteDeviceStatus(address);
                }
                break;
            case MENU_PAIR:
                if (!mBluetooth.hasBonding(address)) {
                    pair(pref, address);
                }
                break;
        }
        return true;
    }
    
    private void startScanning() {
        if (mIsEnabled && mBluetooth.isDiscovering()) {
            return;
        }
        resetDeviceListUI();
        if (mIsEnabled) {
            mBluetooth.startDiscovery();
        }
    }

    private void clearDevices() {
        String [] addresses = mBluetooth.listBondings();
        if (addresses != null) {
            for (int i = 0; i < addresses.length; i++) {
                unbond(addresses[i]);
            }
        }
        resetDeviceListUI();
    }
    
    /* Update the Bluetooth toggle and visibility summary */
    private void updateStatus() {
        boolean started = mIsEnabled;
        mBTToggle.setChecked(started);
    }
    
    private void updateRemoteDeviceStatus(String address) {
        if (address != null) {
            Preference device = mDeviceMap.get(address);
            if (device == null) {
                // This device is not in our discovered list
                // Let's add the device, if BT is not shut down already
                if (mIsEnabled) {
                    addDeviceToUI(address, null, null, WEIGHT_PAIRED);
                }
                return;
            }
            device.setEnabled(true);
            if (address.equals(mBluetoothHeadset.getHeadsetAddress())) {
                int state = mBluetoothHeadset.getState();
                switch (state) {
                    case BluetoothHeadset.STATE_CONNECTED:
                        device.setSummary(STR_CONNECTED);
                        mLastConnected = address;
                        break;
                    case BluetoothHeadset.STATE_CONNECTING:
                        device.setSummary(STR_CONNECTING);
                        break;
                    case BluetoothHeadset.STATE_DISCONNECTED:
                        if (mBluetooth.hasBonding(address)) {
                            device.setSummary(STR_PAIRED);
                        }
                        break;
                }
            } else if (mBluetooth.hasBonding(address)) {
                device.setSummary(STR_PAIRED);
            } else {
                device.setSummary(STR_NOT_CONNECTED);
            }
        }
    }
    
    private void updateRemoteDeviceStatus(String address, String summary) {
        Preference device = mDeviceMap.get(address);
        if (device != null) {
            device.setEnabled(true);
            device.setSummary(summary);
        }
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(BT_NAME)) {
            String name = sharedPreferences.getString(key, null);
            if (name == null) {
                return;
            }
            if (mBluetooth.setName(name)) {
                mBTName.setSummary(name);
            }
        }
    }
        
    private String getAddressFromKey(String key) {
        if (key != null) {
            return key.substring(BT_KEY_LENGTH);
        }
        return "";
    }

    private void sendPin(String pin) {
        byte[] pinBytes = BluetoothDevice.convertPinToBytes(pin);
        if (pinBytes == null) {
            mBluetooth.cancelPin(mPinAddress);
        } else {
            mBluetooth.setPin(mPinAddress, pinBytes);
        }
        mPinAddress = null;
    }
    
    private AlertDialog showPinDialog(String savedPin, String pinAddress) {
        if (mPinDialog != null) {
            return mPinDialog;
        }
        View view = LayoutInflater.from(this).inflate(
                R.layout.bluetooth_pin_entry, null);
        mPinEdit = (EditText) view.findViewById(R.id.text);
        mPinEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mPinEdit.setOnKeyListener(this);
        mPinAddress = pinAddress;
        
        if (savedPin != null) {
            mPinEdit.setText(savedPin);
        }
        
        String remoteName = mBluetooth.getRemoteName(mPinAddress);
        if (remoteName == null) {
            remoteName = mPinAddress;
        }
            
        AlertDialog ad = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.bluetooth_notif_title))
            .setMessage(getString(R.string.bluetooth_enter_pin_msg) + remoteName)
            .setView(view)
            .setPositiveButton(android.R.string.ok, mDisconnectListener)
            .setNegativeButton(android.R.string.cancel, mDisconnectListener)
            .setOnCancelListener(mCancelListener)
            .show();
        ad.setCanceledOnTouchOutside(false);
        // Making an assumption here that the dialog buttons have the ids starting
        // with ...button1 as below
        mPinButton1 = ad.findViewById(com.android.internal.R.id.button1);
        if (mPinButton1 != null) {
            mPinButton1.setEnabled(savedPin != null? savedPin.length() > 0 : false);
        }
        return ad;
    }
    
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mBTToggle) {
            toggleBT();
            return false;
        } else if (preference == mBTVisibility) {
            boolean vis = mBTVisibility.isChecked();
            if (!vis) {
                // Cancel discoverability
                mBluetooth.setMode(BluetoothDevice.MODE_CONNECTABLE);
                mHandler.removeMessages(HANDLE_DISCOVERABLE_TIMEOUT);
            } else {
                mBluetooth.setMode(BluetoothDevice.MODE_DISCOVERABLE);
                mBTVisibility.setSummaryOn(
                        getResources().getString(R.string.bluetooth_is_discoverable,
                                String.valueOf(mDiscoverableTime)));
                mDiscoverableStartTime = SystemClock.elapsedRealtime();
                mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(HANDLE_DISCOVERABLE_TIMEOUT), 1000);
            }
        } else {
            String key = preference.getKey();
            if (key.startsWith(BT_KEY_PREFIX)) {
                // Extract the device address from the key
                String address = getAddressFromKey(key);
                if (mBluetoothHeadset.isConnected(address)) {
                    askDisconnect(address);
                } else if (mBluetooth.hasBonding(address)) {
                    if (mIsEnabled) {
                        mBluetooth.cancelDiscovery();
                    }
                    updateRemoteDeviceStatus(address, STR_CONNECTING); 
                    connect(preference, address);
                } else {
                    if (mIsEnabled) {
                        mBluetooth.cancelDiscovery();
                    }
                    pair(preference, address);
                }
            }
        }
        return false;
    }

    /* Handle the key input to the PIN entry dialog */
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER 
                || keyCode == KeyEvent.KEYCODE_ENTER) {
            String pin = ((EditText)v).getText().toString(); 
            if (pin != null && pin.length() > 0) {
                sendPin(pin);
                mPinDialog.dismiss();
                return true;
            }
        } else if (mPinButton1 != null) {
            boolean valid =
                    BluetoothDevice.convertPinToBytes(((EditText)v).getText().toString()) != null;
            mPinButton1.setEnabled(valid);
        }
        return false;
    }
    
    private void askDisconnect(String address) {
        String name = mBluetooth.getRemoteName(address);
        if (name == null) {
            name = mRes.getString(R.string.bluetooth_device);
        }
        String message = mRes.getString(R.string.bluetooth_disconnect_blank, name);

        mDisconnectAddress = address;
        
        AlertDialog ad = new AlertDialog.Builder(this)
                .setTitle(message)
                .setPositiveButton(android.R.string.ok, mDisconnectListener)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        ad.setCanceledOnTouchOutside(false);

    }

    private void pairingDone(String address, boolean result) {
        Preference pref = mDeviceMap.get(address);
        if (pref != null) {
            pref.setEnabled(true);
            updateRemoteDeviceStatus(address);
        } else if (result) {
            // We've paired to a device that isn't in our list
            addDeviceToUI(address, STR_PAIRED, mBluetooth.getRemoteName(address), 
                    WEIGHT_PAIRED);
        }
    }
    
    private void pair(Preference pref, String address) {
        pref.setEnabled(false);
        pref.setSummary(STR_PAIRING);
        mBluetooth.createBonding(address, sDeviceCallback);
    }
    
    private void connect(Preference pref, String address) {
        pref.setEnabled(false);
        //TODO: Prompt the user to confirm they will disconnect current headset
        disconnect();
        mBluetoothHeadset.connectHeadset(address, mHeadsetCallback);
    }

    private void disconnect() {
        int state = mBluetoothHeadset.getState();
        if (state == BluetoothHeadset.STATE_CONNECTING ||
                state == BluetoothHeadset.STATE_CONNECTED) {
            mBluetoothHeadset.disconnectHeadset();
        }
    }
    
    private void toggleBT() {
        if (mIsEnabled) {
            mBTToggle.setSummaryOn(mRes.getString(R.string.bluetooth_stopping));
            mBTDeviceList.setProgress(false);
            // Force shutdown.
            mBluetooth.cancelDiscovery();
            mBluetooth.disable();
        } else {
            mBTToggle.setSummaryOff(mRes.getString(R.string.bluetooth_enabling));
            mBTToggle.setChecked(false);
            mBTToggle.setEnabled(false);
            if (!mBluetooth.enable()) {
                mBTToggle.setEnabled(true);
            }
        }
    }

    private void addDeviceToUI(String address, String summary, String name, 
            int rssi) {
        
        if (address == null) {
            return;
        }

        BluetoothListItem p;
        if (mDeviceMap.containsKey(address)) {
            p = (BluetoothListItem) mDeviceMap.get(address);
            if (summary != null && summary.equals(STR_NOT_CONNECTED)) {
                if (mBluetooth.hasBonding(address)) {
                    summary = STR_PAIRED;
                }
            }
            CharSequence oldSummary = p.getSummary();
            if (oldSummary != null && oldSummary.equals(STR_CONNECTED)) {
                summary = STR_CONNECTED; // Don't override connected with paired
                mLastConnected = address;
            }
        } else {
            p = new BluetoothListItem(this, null);
        }
        if (name == null) {
            name = mBluetooth.getRemoteName(address);
        }
        if (name == null) {
            name = address;
        }

        p.setTitle(name);
        p.setSummary(summary);
        p.setKey(BT_KEY_PREFIX + address);
        // Enable the headset icon if it is most probably a headset class device
        if (DeviceClass.getMajorClass(mBluetooth.getRemoteClass(address)) == 
                DeviceClass.MAJOR_CLASS_AUDIO_VIDEO) {
            p.setHeadset(true);
        }
        p.setWeight(rssi);
        if (!mDeviceMap.containsKey(address)) {
            mBTDeviceList.addPreference(p);
            mDeviceMap.put(address, p);
        }
    }

    private void addDevices(String [] addresses,
            String[] deviceStatus, int[] rssi) {
        for (int i = 0; i < addresses.length; i++) {
            String status = deviceStatus[i];
            String name = mBluetooth.getRemoteName(addresses[i]);
            String address = addresses[i];
            // Query the status if it's not known
            if (status == null) {
                if (mBluetoothHeadset.isConnected(addresses[i])) {
                    status = STR_CONNECTED;
                    mLastConnected = address;
                } else if (mBluetooth.hasBonding(addresses[i])) {
                    status = STR_PAIRED;
                } else {
                    status = STR_NOT_CONNECTED;
                }
            }
            addDeviceToUI(address, status, name, rssi[i]);
        }
    }
    
    private void removeDeviceFromUI(String address) {
        Preference p = mDeviceMap.get(address);
        if (p == null) {
            return;
        }
        mBTDeviceList.removePreference(p);
        mDeviceMap.remove(address);
    }
    
    private void updateDeviceName(String address, String name) {
        Preference p = mDeviceMap.get(address);
        if (p != null) {
            p.setTitle(name);
        }
    }
        
    private void resetDeviceListUI() {
        mDeviceMap.clear();

        while (mBTDeviceList.getPreferenceCount() > 0) {
            mBTDeviceList.removePreference(mBTDeviceList.getPreference(0));
        }
        if (!mIsEnabled) {
            return;
        }
        
        String connectedDevice = mBluetoothHeadset.getHeadsetAddress();
        if (connectedDevice != null && mBluetoothHeadset.isConnected(connectedDevice)) {
            addDeviceToUI(connectedDevice, STR_CONNECTED, 
                    mBluetooth.getRemoteName(connectedDevice), WEIGHT_CONNECTED);
        }
        String [] bondedDevices = mBluetooth.listBondings();
        if (bondedDevices != null) {
            for (int i = 0; i < bondedDevices.length; i++) {
                addDeviceToUI(bondedDevices[i], STR_PAIRED_NOT_NEARBY, 
                        mBluetooth.getRemoteName(bondedDevices[i]), WEIGHT_PAIRED);
            }
        }
    }
    
    private void unbond(String address) {
        mBluetooth.removeBonding(address);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String address = intent.getStringExtra(BluetoothIntent.ADDRESS);
            if (action.equals(BluetoothIntent.ENABLED_ACTION)) {
                mIsEnabled = true;
                mBTToggle.setChecked(true);
                mBTToggle.setSummaryOn(mRes.getString(R.string.bluetooth_enabled));
                mBTToggle.setEnabled(true);
                String name = mBluetooth.getName();
                if (name != null) {
                    mBTName.setSummary(name);
                }
                // save the "enabled" setting to database, so we can
                // remember it on startup.
                Settings.System.putInt(getContentResolver(),
                                       Settings.System.BLUETOOTH_ON, 1);
                resetDeviceListUI();
                if (mAutoDiscovery) {
                    mBluetooth.startDiscovery();
                }
            } else if (action.equals(BluetoothIntent.DISABLED_ACTION)) {
                mIsEnabled = false;
                mBTToggle.setSummaryOff(mRes.getString(R.string.bluetooth_disabled));
                resetDeviceListUI();
                mBTVisibility.setChecked(false);
                // save the "disabled" setting to database
                Settings.System.putInt(getContentResolver(), 
                                       Settings.System.BLUETOOTH_ON, 0);
            } else if (action.equals(BluetoothIntent.REMOTE_DEVICE_FOUND_ACTION)) {
                if (address != null) {
                    int rssi = intent.getShortExtra(BluetoothIntent.RSSI, 
                            (short) WEIGHT_UNKNOWN);
                    addDeviceToUI(address, STR_NOT_CONNECTED, null, rssi);
                }
            } else if (action.equals(BluetoothIntent.REMOTE_NAME_UPDATED_ACTION)) {
                String name = intent.getStringExtra(BluetoothIntent.NAME);
                updateDeviceName(address, name);
            } else if (action.equals(BluetoothIntent.REMOTE_DEVICE_DISAPPEARED_ACTION)) {
                removeDeviceFromUI(address);
            } else if (action.equals(BluetoothIntent.PAIRING_REQUEST_ACTION)) {
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_PIN_REQUEST, address));
            } else if (action.equals(BluetoothIntent.HEADSET_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(BluetoothIntent.HEADSET_STATE,
                                               BluetoothHeadset.STATE_ERROR);
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_CONNECTED, address));
                } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_DISCONNECTED, address));
                } else if (state == BluetoothHeadset.STATE_CONNECTING) {
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_CONNECTING, address));                    
                }
            } else if (action.equals(BluetoothIntent.DISCOVERY_STARTED_ACTION)) {
                mBTDeviceList.setProgress(true);
            } else if (action.equals(BluetoothIntent.DISCOVERY_COMPLETED_ACTION)) {
                mBTDeviceList.setProgress(false);
            } else if (action.equals(BluetoothIntent.MODE_CHANGED_ACTION)) {
                mBTVisibility.setChecked(
                        mBluetooth.getMode() == BluetoothDevice.MODE_DISCOVERABLE);
            } else if (action.equals(BluetoothIntent.BONDING_CREATED_ACTION)) {
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_PAIRING_PASSED, address));
            } else if (action.equals(BluetoothIntent.REMOTE_DEVICE_CONNECTED_ACTION)) { 
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_CONNECTED, address));
            } else if (action.equals(BluetoothIntent.REMOTE_DEVICE_DISCONNECTED_ACTION)) { 
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_DISCONNECTED, address));
            }
        }
    };

    
    static class DeviceCallback extends IBluetoothDeviceCallback.Stub {
        Handler messageHandler;

        public void setHandler(Handler handler) {
            synchronized (this) {
                messageHandler = handler;
            }
        }
        
        public void onCreateBondingResult(String address, int result) {
            synchronized (this) {
                if (messageHandler != null) {
                    if (result == BluetoothDevice.RESULT_FAILURE) {
                        messageHandler.sendMessage(messageHandler.obtainMessage(
                                HANDLE_PAIRING_FAILED, address));
                    } else {
                        messageHandler.sendMessage(messageHandler.obtainMessage(
                                HANDLE_PAIRING_PASSED, address));
                    }
                }
            }
        }
        
        public void onEnableResult(int result) { }
        public void onGetRemoteServiceChannelResult(String address, int channel) { }
    };
    
    private IBluetoothHeadsetCallback mHeadsetCallback = new IBluetoothHeadsetCallback.Stub() {
        public void onConnectHeadsetResult(String address, int resultCode) {
            if (resultCode == BluetoothHeadset.RESULT_SUCCESS) {
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_CONNECTED, address));
            } else {
                // Make toast in UI thread
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_FAILED_TO_CONNECT, resultCode,
                            -1, address));
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_CONNECTED:
                case HANDLE_DISCONNECTED:
                case HANDLE_CONNECTING:
                    updateRemoteDeviceStatus((String) msg.obj);
                    break;
                case HANDLE_FAILED_TO_CONNECT:
                    updateRemoteDeviceStatus((String) msg.obj);
                    String name = mBluetooth.getRemoteName((String) msg.obj);
                    if (name == null) {
                        name = (String) msg.obj;
                    }
                    if (msg.arg1 == BluetoothHeadset.RESULT_FAILURE) {
                        Toast.makeText(BluetoothSettings.this, 
                                mRes.getString(R.string.failed_to_connect, name),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case HANDLE_PIN_REQUEST:
                    mPinDialog = showPinDialog(null, (String) msg.obj);
                    break;
                case HANDLE_DISCOVERABLE_TIMEOUT:
                    long nowTime = SystemClock.elapsedRealtime();
                    int secondsLeft = mDiscoverableTime
                            - (int) (nowTime - mDiscoverableStartTime) / 1000;
                    if (secondsLeft > 0) {
                        mBTVisibility.setSummaryOn(
                                getResources().getString(R.string.bluetooth_is_discoverable,
                                        String.valueOf(secondsLeft)));
                        sendMessageDelayed(obtainMessage(HANDLE_DISCOVERABLE_TIMEOUT), 1000);
                    } else {
                        mBluetooth.setMode(BluetoothDevice.MODE_CONNECTABLE);
                        mBTVisibility.setChecked(false);
                    }
                    break;
                case HANDLE_INITIAL_SCAN:
                    if (mBluetoothHeadset.getState() == BluetoothHeadset.STATE_ERROR &&
                            ((Integer)msg.obj).intValue() < 2) {
                        // Second attempt after another 100ms
                        sendMessageDelayed(obtainMessage(HANDLE_INITIAL_SCAN, 2), 100);
                    } else {
                        resetDeviceListUI();
                        if (mAutoDiscovery) {
                            mBluetooth.cancelDiscovery();
                            mBluetooth.startDiscovery();
                        }
                    }
                    break;
                case HANDLE_PAIRING_PASSED:
                    String addr = (String) msg.obj;
                    pairingDone(addr, true);
                    break;
                case HANDLE_PAIRING_FAILED:
                    String address = (String) msg.obj;
                    pairingDone(address, false);
                    String pairName = mBluetooth.getRemoteName(address);
                    if (pairName == null) {
                        pairName = address;
                    }
                    Toast.makeText(BluetoothSettings.this, 
                            mRes.getString(R.string.failed_to_pair, pairName),
                            Toast.LENGTH_SHORT).show();
                    break;
                case HANDLE_PAUSE_TIMEOUT:
                    // Possibility of race condition, but not really harmful
                    if (!sIsRunning) {
                        Object[] params = (Object[]) msg.obj;
                        BluetoothDevice bluetooth = (BluetoothDevice) params[0];
                        if (bluetooth.isEnabled()) {
                            if (bluetooth.isDiscovering()) {
                                bluetooth.cancelDiscovery();
                            }
                            if (params[1] != null) {
                                bluetooth.cancelBondingProcess((String) params[1]);
                            }
                            bluetooth.setMode(BluetoothDevice.MODE_CONNECTABLE);
                        }
                    }
                    break;
            }
        }
    };

    private DialogInterface.OnClickListener mDisconnectListener = 
        new DialogInterface.OnClickListener() {
        
            public void onClick(DialogInterface dialog, int which) {
                if (dialog == mPinDialog) {
                    if (which == DialogInterface.BUTTON1) {
                        String pin = mPinEdit.getText().toString();
                        if (pin != null && pin.length() > 0) {
                            sendPin(pin);
                        } else {
                            sendPin(null);
                        }
                    } else {
                        sendPin(null);
                    }
                    mPinDialog = null;
                    mPinEdit = null;
                } else {
                    if (which == DialogInterface.BUTTON1) {
                        disconnect();
                    }
                }
            }
    };

    private DialogInterface.OnCancelListener mCancelListener =
        new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (dialog == mPinDialog) {
                    sendPin(null);
                }
                mPinDialog = null;
                mPinEdit = null;
            }
    };
}

