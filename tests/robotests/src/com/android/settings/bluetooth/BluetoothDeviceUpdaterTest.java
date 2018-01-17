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
 * limitations under the License
 */
package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothDeviceUpdaterTest {
    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private SettingsActivity mSettingsActivity;

    private Context mContext;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private BluetoothDevicePreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        doReturn(mContext).when(mDashboardFragment).getContext();
        doReturn(mBluetoothDevice).when(mCachedBluetoothDevice).getDevice();

        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice, false);
        mBluetoothDeviceUpdater = new BluetoothDeviceUpdater(mDashboardFragment,
                mDevicePreferenceCallback, null) {
            @Override
            public boolean isFilterMatched(CachedBluetoothDevice cachedBluetoothDevice) {
                return true;
            }
        };
        mBluetoothDeviceUpdater.setPrefContext(mContext);
    }

    @Test
    public void testAddPreference_deviceExist_doNothing() {
        mBluetoothDeviceUpdater.mPreferenceMap.put(mBluetoothDevice, mPreference);

        mBluetoothDeviceUpdater.addPreference(mCachedBluetoothDevice);

        verify(mDevicePreferenceCallback, never()).onDeviceAdded(any(Preference.class));
    }

    @Test
    public void testAddPreference_deviceNotExist_addPreference() {
        mBluetoothDeviceUpdater.addPreference(mCachedBluetoothDevice);

        final Preference preference = mBluetoothDeviceUpdater.mPreferenceMap.get(mBluetoothDevice);
        assertThat(preference).isNotNull();
        verify(mDevicePreferenceCallback).onDeviceAdded(preference);
    }

    @Test
    public void testRemovePreference_deviceExist_removePreference() {
        mBluetoothDeviceUpdater.mPreferenceMap.put(mBluetoothDevice, mPreference);

        mBluetoothDeviceUpdater.removePreference(mCachedBluetoothDevice);

        verify(mDevicePreferenceCallback).onDeviceRemoved(mPreference);
        assertThat(mBluetoothDeviceUpdater.mPreferenceMap.containsKey(mBluetoothDevice)).isFalse();
    }

    @Test
    public void testRemovePreference_deviceNotExist_doNothing() {
        mBluetoothDeviceUpdater.removePreference(mCachedBluetoothDevice);

        verify(mDevicePreferenceCallback, never()).onDeviceRemoved(any(Preference.class));
    }

    @Test
    public void testDeviceProfilesListener_click_startBluetoothDeviceDetailPage() {
        doReturn(mSettingsActivity).when(mDashboardFragment).getActivity();

        mBluetoothDeviceUpdater.mDeviceProfilesListener.onGearClick(mPreference);

        verify(mSettingsActivity).startPreferencePanel(eq(mDashboardFragment),
                eq(BluetoothDeviceDetailsFragment.class.getName()), any(),
                eq(R.string.device_details_title), eq(null), eq(null), eq(0));
    }
}
