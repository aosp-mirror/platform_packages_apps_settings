/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

/** Tests for {@link BluetoothDevicePairingDetailBase}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowBluetoothAdapter.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class BluetoothDevicePairingDetailBaseTest {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    public static final String KEY_DEVICE_LIST_GROUP = "test_key";

    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private static final String TEST_DEVICE_ADDRESS_B = "00:B1:B1:B1:B1:B1";
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    private Resources mResource;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private Drawable mDrawable;
    private BluetoothAdapter mBluetoothAdapter;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private BluetoothProgressCategory mAvailableDevicesCategory;
    private BluetoothDevice mBluetoothDevice;
    private TestBluetoothDevicePairingDetailBase mFragment;

    @Before
    public void setUp() {
        mAvailableDevicesCategory = spy(new BluetoothProgressCategory(mContext));
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        when(mCachedBluetoothDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        final Pair<Drawable, String> pairs = new Pair<>(mDrawable, "fake_device");
        when(mCachedBluetoothDevice.getDrawableWithDescription()).thenReturn(pairs);
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS);

        mFragment = spy(new TestBluetoothDevicePairingDetailBase());
        when(mFragment.findPreference(KEY_DEVICE_LIST_GROUP)).thenReturn(mAvailableDevicesCategory);
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mResource).when(mFragment).getResources();
        mFragment.mDeviceListGroup = mAvailableDevicesCategory;
        mFragment.mLocalManager = mLocalManager;
        mFragment.mBluetoothAdapter = mBluetoothAdapter;
        mFragment.initPreferencesFromPreferenceScreen();

    }

    @Test
    public void startScanning_startScanAndRemoveDevices() {
        mFragment.enableScanning();

        verify(mFragment).startScanning();
        verify(mAvailableDevicesCategory).removeAll();
    }

    @Test
    public void updateContent_stateOn() {
        mFragment.updateContent(BluetoothAdapter.STATE_ON);

        assertThat(mBluetoothAdapter.isEnabled()).isTrue();
        verify(mFragment).enableScanning();
    }

    @Test
    public void updateContent_stateOff_finish() {
        mFragment.updateContent(BluetoothAdapter.STATE_OFF);

        verify(mFragment).finish();
    }

    @Test
    public void updateBluetooth_bluetoothOff_turnOnBluetooth() {
        mShadowBluetoothAdapter.setEnabled(false);

        mFragment.updateBluetooth();

        assertThat(mBluetoothAdapter.isEnabled()).isTrue();
    }

    @Test
    public void updateBluetooth_bluetoothOn_updateState() {
        mShadowBluetoothAdapter.setEnabled(true);
        doNothing().when(mFragment).updateContent(anyInt());

        mFragment.updateBluetooth();

        verify(mFragment).updateContent(anyInt());
    }

    @Test
    public void onBluetoothStateChanged_whenTurnedOnBTShowToast() {
        doNothing().when(mFragment).updateContent(anyInt());

        mFragment.onBluetoothStateChanged(BluetoothAdapter.STATE_ON);

        verify(mFragment).showBluetoothTurnedOnToast();
    }

    @Test
    public void onProfileConnectionStateChanged_deviceInSelectedListAndConnected_finish() {
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_B);
        mFragment.mSelectedList.add(mBluetoothDevice);
        mFragment.mSelectedList.add(device);

        when(mCachedBluetoothDevice.isConnected()).thenReturn(true);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(device);

        mFragment.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.A2DP, BluetoothAdapter.STATE_CONNECTED);

        verify(mFragment).finish();
    }

    @Test
    public void onProfileConnectionStateChanged_deviceNotInSelectedList_doNothing() {
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_B);
        mFragment.mSelectedList.add(device);

        when(mCachedBluetoothDevice.isConnected()).thenReturn(true);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);

        mFragment.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.A2DP, BluetoothAdapter.STATE_CONNECTED);

        // not crash
    }

    @Test
    public void onProfileConnectionStateChanged_deviceDisconnected_doNothing() {
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_B);
        mFragment.mSelectedList.add(mBluetoothDevice);
        mFragment.mSelectedList.add(device);

        when(mCachedBluetoothDevice.isConnected()).thenReturn(false);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(device);

        mFragment.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.A2DP, BluetoothAdapter.STATE_DISCONNECTED);

        // not crash
    }

    @Test
    public void onProfileConnectionStateChanged_deviceInPreferenceMapAndConnected_removed() {
        final BluetoothDevicePreference preference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        true, BluetoothDevicePreference.SortType.TYPE_FIFO);
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS);
        mFragment.getDevicePreferenceMap().put(mCachedBluetoothDevice, preference);

        when(mCachedBluetoothDevice.isConnected()).thenReturn(true);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(device);

        mFragment.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.A2DP, BluetoothAdapter.STATE_CONNECTED);

        assertThat(mFragment.getDevicePreferenceMap().size()).isEqualTo(0);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceNotInPreferenceMap_doNothing() {
        final CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        final BluetoothDevicePreference preference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        true, BluetoothDevicePreference.SortType.TYPE_FIFO);
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS);
        final BluetoothDevice device2 = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_B);
        mFragment.getDevicePreferenceMap().put(mCachedBluetoothDevice, preference);

        when(mCachedBluetoothDevice.isConnected()).thenReturn(true);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(device);
        when(cachedDevice.isConnected()).thenReturn(true);
        when(cachedDevice.getDevice()).thenReturn(device2);
        when(cachedDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS_B);
        when(cachedDevice.getIdentityAddress()).thenReturn(TEST_DEVICE_ADDRESS_B);

        mFragment.onProfileConnectionStateChanged(cachedDevice, BluetoothProfile.A2DP,
                BluetoothAdapter.STATE_CONNECTED);

        // not crash
    }

    private static class TestBluetoothDevicePairingDetailBase extends
            BluetoothDevicePairingDetailBase {

        TestBluetoothDevicePairingDetailBase() {
            super();
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        public String getDeviceListKey() {
            return KEY_DEVICE_LIST_GROUP;
        }

        @Override
        protected int getPreferenceScreenResId() {
            return 0;
        }

        @Override
        protected String getLogTag() {
            return "test_tag";
        }
    }
}
