/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothProfileManager.Profile;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * RequestPermissionActivity asks the user whether to enable discovery. This is
 * usually started by an application wanted to start bluetooth and or discovery
 */
public class DockSettingsActivity extends AlertActivity implements DialogInterface.OnClickListener,
        AlertDialog.OnMultiChoiceClickListener, OnCheckedChangeListener {

    private static final String TAG = "DockSettingsActivity";

    private static final boolean DEBUG = true;

    private static final String SHARED_PREFERENCES_KEY_AUTO_CONNECT_TO_DOCK = "auto_connect_to_dock";

    private BluetoothDevice mDevice;

    private int mState = Intent.EXTRA_DOCK_STATE_UNDOCKED;

    private CachedBluetoothDevice mCachedDevice;

    private LocalBluetoothManager mLocalManager;

    private LocalBluetoothProfileManager mA2dpMgr;

    private LocalBluetoothProfileManager mHeadsetMgr;

    private LocalBluetoothProfileManager[] mProfileManagers;

    private boolean[] mCheckedItems;

    private CheckBox mRememberCheck;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!parseIntent(intent)) {
                finish();
                return;
            }

            if (DEBUG) Log.d(TAG, "Action: " + intent.getAction() + " State: " + mState);
        }
    };

    private Profile[] mProfiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!parseIntent(getIntent())) {
            finish();
            return;
        }

        if (mState == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
            handleUndocked(this, mLocalManager, mDevice);
            dismiss();
            return;
        }
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        createDialog();
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (!parseIntent(getIntent())) {
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
    }

    private void createDialog() {
        // TODO Avoid hardcoding dock and profiles. Read from system properties
        int numOfProfiles;
        switch (mState) {
            case Intent.EXTRA_DOCK_STATE_CAR:
                numOfProfiles = 2;
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
                numOfProfiles = 1;
                break;
            default:
                return;
        }

        CharSequence[] items = new CharSequence[numOfProfiles];
        mCheckedItems = new boolean[numOfProfiles];
        mProfileManagers = new LocalBluetoothProfileManager[numOfProfiles];
        mProfiles = new Profile[numOfProfiles];

        int i = 0;
        switch (mState) {
            case Intent.EXTRA_DOCK_STATE_CAR:
                mProfileManagers[i] = mHeadsetMgr;
                mProfiles[i] = Profile.HEADSET;
                mCheckedItems[i] = mHeadsetMgr.isPreferred(mDevice);
                items[i] = getString(R.string.bluetooth_dock_settings_headset);
                ++i;
                // fall through
            case Intent.EXTRA_DOCK_STATE_DESK:
                mProfileManagers[i] = mA2dpMgr;
                mProfiles[i] = Profile.A2DP;
                mCheckedItems[i] = mA2dpMgr.isPreferred(mDevice);
                items[i] = getString(R.string.bluetooth_dock_settings_a2dp);
                break;
        }

        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.bluetooth_dock_settings_title);

        // Profiles
        p.mIsMultiChoice = true;
        p.mItems = items;
        p.mCheckedItems = mCheckedItems;
        p.mOnCheckboxClickListener = this;

        // Remember this settings
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        p.mView = inflater.inflate(R.layout.remember_dock_setting, null);
        p.mViewSpacingSpecified = true;
        float pixelScaleFactor = getResources().getDisplayMetrics().density;
        p.mViewSpacingLeft = (int) (14 * pixelScaleFactor);
        p.mViewSpacingRight = (int) (14 * pixelScaleFactor);
        mRememberCheck = (CheckBox)p.mView.findViewById(R.id.remember);
        if (DEBUG) Log.d(TAG, "Auto Check? = " + getAutoConnectSetting(mLocalManager));
        mRememberCheck.setChecked(getAutoConnectSetting(mLocalManager));
        mRememberCheck.setOnCheckedChangeListener(this);

        // Ok Button
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;

        setupAlert();
    }

    // Called when the individual items are clicked.
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (DEBUG) Log.d(TAG, "Item " + which + " changed to " + isChecked);
        mCheckedItems[which] = isChecked;
    }

    // Called when the "Remember" Checkbox is clicked
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (DEBUG) Log.d(TAG, "onCheckedChanged: Remember Settings = " + isChecked);
        saveAutoConnectSetting(mLocalManager, isChecked);
    }

    // Called when clicked on the OK button
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            switch (mLocalManager.getBluetoothState()) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    mLocalManager.getBluetoothAdapter().enable();
                    // TODO can I call connect right away? probably not.
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    // TODO wait? probably
                    break;
                case BluetoothAdapter.STATE_ON:
                    break;
            }

            for(int i = 0; i < mProfileManagers.length; i++) {
                mProfileManagers[i].setPreferred(mDevice, mCheckedItems[i]);

                if (DEBUG) Log.d(TAG, mProfileManagers[i].toString() + " = " + mCheckedItems[i]);
                boolean isConnected = mProfileManagers[i].isConnected(mDevice);
                if (mCheckedItems[i] && !isConnected) {
                    if (DEBUG) Log.d(TAG, "Connecting ");
                    mCachedDevice.connect(mProfiles[i]);
                } else if (isConnected){
                    if (DEBUG) Log.d(TAG, "Disconnecting");
                    mProfileManagers[i].disconnect(mDevice);
                }
            }
        }
    }

    private boolean parseIntent(Intent intent) {
        if (intent == null) {
            return false;
        }

        mLocalManager = LocalBluetoothManager.getInstance(this);
        if (mLocalManager == null) {
            if (DEBUG) Log.d(TAG, "Error: there's a problem starting bluetooth");
            return false;
        }

        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (mDevice == null) {
            if (DEBUG) Log.d(TAG, "device == null");
            return false;
        }

        mState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                Intent.EXTRA_DOCK_STATE_UNDOCKED);
        if (mState == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
            handleUndocked(this, mLocalManager, mDevice);
            return false;
        }

        mCachedDevice = getCachedBluetoothDevice(this, mLocalManager, mDevice);
        mA2dpMgr = LocalBluetoothProfileManager.getProfileManager(mLocalManager, Profile.A2DP);
        mHeadsetMgr = LocalBluetoothProfileManager.getProfileManager(mLocalManager,
                Profile.HEADSET);

        return true;
    }

    public static void handleUndocked(Context context, LocalBluetoothManager localManager,
            BluetoothDevice device) {
        CachedBluetoothDevice cachedBluetoothDevice = getCachedBluetoothDevice(context,
                localManager, device);
        cachedBluetoothDevice.disconnect();
    }

    public static void handleDocked(Context context, LocalBluetoothManager localManager,
            BluetoothDevice device, int state) {
        CachedBluetoothDevice cachedBluetoothDevice = getCachedBluetoothDevice(context,
                localManager, device);
        cachedBluetoothDevice.connect();
    }

    private static CachedBluetoothDevice getCachedBluetoothDevice(Context context,
            LocalBluetoothManager localManager, BluetoothDevice device) {
        CachedBluetoothDeviceManager cachedDeviceManager = localManager.getCachedDeviceManager();
        CachedBluetoothDevice cachedBluetoothDevice = cachedDeviceManager.findDevice(device);
        if (cachedBluetoothDevice == null) {
            cachedBluetoothDevice = new CachedBluetoothDevice(context, device);
        }
        return cachedBluetoothDevice;
    }

    public static boolean hasAutoConnectSetting(LocalBluetoothManager localManager) {
        return localManager.getSharedPreferences().contains(
                SHARED_PREFERENCES_KEY_AUTO_CONNECT_TO_DOCK);
    }

    public static boolean getAutoConnectSetting(LocalBluetoothManager localManager) {
        return localManager.getSharedPreferences().getBoolean(
                SHARED_PREFERENCES_KEY_AUTO_CONNECT_TO_DOCK, false);
    }

    public static void saveAutoConnectSetting(LocalBluetoothManager localManager,
            boolean autoConnect) {
        SharedPreferences.Editor editor = localManager.getSharedPreferences().edit();
        editor.putBoolean(SHARED_PREFERENCES_KEY_AUTO_CONNECT_TO_DOCK, autoConnect);
        editor.commit();
    }
}
