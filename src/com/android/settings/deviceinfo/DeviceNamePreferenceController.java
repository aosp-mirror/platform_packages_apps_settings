/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.annotation.Nullable;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.SpannedString;

import com.android.internal.annotations.VisibleForTesting;

import com.android.settings.bluetooth.BluetoothLengthDeviceNameFilter;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.tether.WifiDeviceNameTextValidator;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

public class DeviceNamePreferenceController extends BasePreferenceController
        implements ValidatedEditTextPreference.Validator,
                Preference.OnPreferenceChangeListener,
                LifecycleObserver,
                OnSaveInstanceState,
                OnCreate {
    private static final String PREF_KEY = "device_name";
    public static final int DEVICE_NAME_SET_WARNING_ID = 1;
    private static final String KEY_PENDING_DEVICE_NAME = "key_pending_device_name";
    private String mDeviceName;
    protected WifiManager mWifiManager;
    private final WifiDeviceNameTextValidator mWifiDeviceNameTextValidator;
    private ValidatedEditTextPreference mPreference;
    @Nullable
    private LocalBluetoothManager mBluetoothManager;
    private DeviceNamePreferenceHost mHost;
    private String mPendingDeviceName;

    public DeviceNamePreferenceController(Context context) {
        super(context, PREF_KEY);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiDeviceNameTextValidator = new WifiDeviceNameTextValidator();

        initializeDeviceName();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (ValidatedEditTextPreference) screen.findPreference(PREF_KEY);
        final CharSequence deviceName = getSummary();
        mPreference.setSummary(deviceName);
        mPreference.setText(deviceName.toString());
        mPreference.setValidator(this);
    }

    private void initializeDeviceName() {
        mDeviceName = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.DEVICE_NAME);
        if (mDeviceName == null) {
            mDeviceName = Build.MODEL;
        }
    }

    @Override
    public CharSequence getSummary() {
        return mDeviceName;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mPendingDeviceName = (String) newValue;
        if (mHost != null) {
            mHost.showDeviceNameWarningDialog(mPendingDeviceName);
        }
        return true;
    }

    @Override
    public boolean isTextValid(String deviceName) {
        // BluetoothNameDialogFragment describes BT name filter as a 248 bytes long cap.
        // Given the restrictions presented by the SSID name filter (32 char), I don't believe it is
        // possible to construct an SSID that is not a valid Bluetooth name.
        return mWifiDeviceNameTextValidator.isTextValid(deviceName);
    }

    public void setLocalBluetoothManager(LocalBluetoothManager localBluetoothManager) {
        mBluetoothManager = localBluetoothManager;
    }

    public void confirmDeviceName() {
        if (mPendingDeviceName != null) {
            setDeviceName(mPendingDeviceName);
        }
    }

    public void setHost(DeviceNamePreferenceHost host) {
        mHost = host;
    }

    /**
     * This method presumes that security/validity checks have already been passed.
     */
    private void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
        setSettingsGlobalDeviceName(deviceName);
        setBluetoothDeviceName(deviceName);
        setTetherSsidName(deviceName);
        mPreference.setSummary(getSummary());
    }

    private void setSettingsGlobalDeviceName(String deviceName) {
        Settings.Global.putString(mContext.getContentResolver(), Settings.Global.DEVICE_NAME,
                deviceName);
    }

    private void setBluetoothDeviceName(String deviceName) {
        // Bluetooth manager doesn't exist for certain devices.
        if (mBluetoothManager == null) {
            return;
        }

        final LocalBluetoothAdapter localBluetoothAdapter = mBluetoothManager.getBluetoothAdapter();
        if (localBluetoothAdapter != null) {
            localBluetoothAdapter.setName(getFilteredBluetoothString(deviceName));
        }
    }

    /**
     * Using a UTF8ByteLengthFilter, we can filter a string to be compliant with the Bluetooth spec.
     * For more information, see {@link com.android.settings.bluetooth.BluetoothNameDialogFragment}.
     */
    private static final String getFilteredBluetoothString(final String deviceName) {
        CharSequence filteredSequence = new BluetoothLengthDeviceNameFilter().filter(deviceName, 0, deviceName.length(),
                new SpannedString(""),
                0, 0);
        // null -> use the original
        if (filteredSequence == null) {
            return deviceName;
        }
        return filteredSequence.toString();
    }

    private void setTetherSsidName(String deviceName) {
        final WifiConfiguration config = mWifiManager.getWifiApConfiguration();
        config.SSID = deviceName;
        // TODO: If tether is running, turn off the AP and restart it after setting config.
        mWifiManager.setWifiApConfiguration(config);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mPendingDeviceName = savedInstanceState.getString(KEY_PENDING_DEVICE_NAME, null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_PENDING_DEVICE_NAME, mPendingDeviceName);
    }

    public interface DeviceNamePreferenceHost {
        void showDeviceNameWarningDialog(String deviceName);
    }
}