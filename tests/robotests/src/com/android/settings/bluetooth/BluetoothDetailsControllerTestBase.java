/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

public class BluetoothDetailsControllerTestBase {
    protected Context mContext = RuntimeEnvironment.application;
    protected Lifecycle mLifecycle;
    protected DeviceConfig mDeviceConfig;
    protected BluetoothDevice mDevice;
    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;
    protected PreferenceScreen mScreen;
    protected PreferenceManager mPreferenceManager;

    @Mock
    protected BluetoothDeviceDetailsFragment mFragment;
    @Mock
    protected CachedBluetoothDevice mCachedDevice;
    @Mock
    protected Activity mActivity;
    @Mock
    protected BluetoothClass mBluetoothDeviceClass;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mDeviceConfig = makeDefaultDeviceConfig();
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getApplicationContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        mLifecycle = spy(new Lifecycle());
        mBluetoothManager = new BluetoothManager(mContext);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    protected static class DeviceConfig {
        private String name;
        private String address;
        private int majorDeviceClass;
        private boolean connected;
        private String connectionSummary;

        public DeviceConfig setName(String newValue) {
            this.name = newValue;
            return this;
        }

        public DeviceConfig setAddress(String newValue) {
            this.address = newValue;
            return this;
        }

        public DeviceConfig setMajorDeviceClass(int newValue) {
            this.majorDeviceClass = newValue;
            return this;
        }

        public DeviceConfig setConnected(boolean newValue) {
            this.connected = newValue;
            return this;
        }
        public DeviceConfig setConnectionSummary(String connectionSummary) {
            this.connectionSummary = connectionSummary;
            return this;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public int getMajorDeviceClass() {
            return majorDeviceClass;
        }

        public boolean isConnected() {
            return connected;
        }

        public String getConnectionSummary() {
            return connectionSummary;
        }
    }

    protected DeviceConfig makeDefaultDeviceConfig() {
        return new DeviceConfig()
                .setName("Mock Device")
                .setAddress("B4:B0:34:B5:3B:1B")
                .setMajorDeviceClass(BluetoothClass.Device.Major.AUDIO_VIDEO)
                .setConnected(true)
                .setConnectionSummary(mContext.getString(R.string.bluetooth_connected));
    }

    /**
     * Sets up the device mock to return various state based on a test config.
     * @param config
     */
    protected void setupDevice(DeviceConfig config) {
        when(mCachedDevice.getName()).thenReturn(config.getName());
        when(mBluetoothDeviceClass.getMajorDeviceClass()).thenReturn(config.getMajorDeviceClass());
        when(mCachedDevice.isConnected()).thenReturn(config.isConnected());
        when(mCachedDevice.getConnectionSummary()).thenReturn(config.getConnectionSummary());

        mDevice = mBluetoothAdapter.getRemoteDevice(config.getAddress());
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getAddress()).thenReturn(config.getAddress());
    }

    /**
     * Convenience method to call displayPreference and onResume.
     */
    protected void showScreen(BluetoothDetailsController controller) {
        controller.displayPreference(mScreen);
        controller.onResume();
    }
}

