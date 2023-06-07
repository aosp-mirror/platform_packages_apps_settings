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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public abstract class BluetoothDetailsControllerTestBase {

    protected Context mContext;
    private LifecycleOwner mLifecycleOwner;
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
    protected FragmentActivity mActivity;
    @Mock
    protected BluetoothClass mBluetoothDeviceClass;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mDeviceConfig = makeDefaultDeviceConfig();
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getApplicationContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = spy(new Lifecycle(mLifecycleOwner));
        mBluetoothManager = mContext.getSystemService(BluetoothManager.class);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    protected static class DeviceConfig {

        private String name;
        private String address;
        private int majorDeviceClass;
        private boolean connected;
        private String connectionSummary;

        DeviceConfig setName(String newValue) {
            this.name = newValue;
            return this;
        }

        DeviceConfig setAddress(String newValue) {
            this.address = newValue;
            return this;
        }

        DeviceConfig setMajorDeviceClass(int newValue) {
            this.majorDeviceClass = newValue;
            return this;
        }

        DeviceConfig setConnected(boolean newValue) {
            this.connected = newValue;
            return this;
        }

        DeviceConfig setConnectionSummary(String connectionSummary) {
            this.connectionSummary = connectionSummary;
            return this;
        }

        String getName() {
            return name;
        }

        String getAddress() {
            return address;
        }

        int getMajorDeviceClass() {
            return majorDeviceClass;
        }

        boolean isConnected() {
            return connected;
        }

        String getConnectionSummary() {
            return connectionSummary;
        }
    }

    DeviceConfig makeDefaultDeviceConfig() {
        return new DeviceConfig()
                .setName("Mock Device")
                .setAddress("B4:B0:34:B5:3B:1B")
                .setMajorDeviceClass(BluetoothClass.Device.Major.AUDIO_VIDEO)
                .setConnected(true)
                .setConnectionSummary(
                        mContext.getString(com.android.settingslib.R.string.bluetooth_connected));
    }

    /**
     * Sets up the device mock to return various state based on a test config.
     */
    void setupDevice(DeviceConfig config) {
        when(mCachedDevice.getName()).thenReturn(config.getName());
        when(mBluetoothDeviceClass.getMajorDeviceClass()).thenReturn(config.getMajorDeviceClass());
        when(mCachedDevice.isConnected()).thenReturn(config.isConnected());
        when(mCachedDevice.getConnectionSummary()).thenReturn(config.getConnectionSummary());

        mDevice = mBluetoothAdapter.getRemoteDevice(config.getAddress());
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getAddress()).thenReturn(config.getAddress());
        when(mCachedDevice.getIdentityAddress()).thenReturn(config.getAddress());
    }

    /**
     * Convenience method to call displayPreference and onResume.
     */
    void showScreen(BluetoothDetailsController controller) {
        controller.displayPreference(mScreen);
        controller.onResume();
    }
}

