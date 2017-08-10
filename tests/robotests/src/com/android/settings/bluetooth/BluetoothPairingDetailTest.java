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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.os.UserManager;
import android.support.v7.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothPairingDetailTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private Resources mResource;
    @Mock
    private LocalBluetoothAdapter mLocalAdapter;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;
    @Mock
    private PreferenceGroup mPreferenceGroup;
    private BluetoothPairingDetail mFragment;
    private Context mContext;
    private BluetoothProgressCategory mAvailableDevicesCategory;
    private FooterPreference mFooterPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mFragment = spy(new BluetoothPairingDetail());
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mResource).when(mFragment).getResources();

        mAvailableDevicesCategory = spy(new BluetoothProgressCategory(mContext));
        mFooterPreference = new FooterPreference(mContext);

        mFragment.mLocalAdapter = mLocalAdapter;
        mFragment.mLocalManager = mLocalManager;
        mFragment.mDeviceListGroup = mPreferenceGroup;
        mFragment.mAlwaysDiscoverable = new AlwaysDiscoverable(mContext, mLocalAdapter);
    }

    @Test
    public void testInitPreferencesFromPreferenceScreen_findPreferences() {
        doReturn(mAvailableDevicesCategory).when(mFragment).findPreference(
                BluetoothPairingDetail.KEY_AVAIL_DEVICES);
        doReturn(mFooterPreference).when(mFragment).findPreference(
                BluetoothPairingDetail.KEY_FOOTER_PREF);

        mFragment.initPreferencesFromPreferenceScreen();

        assertThat(mFragment.mAvailableDevicesCategory).isEqualTo(mAvailableDevicesCategory);
        assertThat(mFragment.mFooterPreference).isEqualTo(mFooterPreference);
    }

    @Test
    public void testStartScanning_startScanAndRemoveDevices() {
        mFragment.mAvailableDevicesCategory = mAvailableDevicesCategory;
        mFragment.mDeviceListGroup = mAvailableDevicesCategory;

        mFragment.enableScanning();

        verify(mLocalAdapter).startScanning(true);
        verify(mAvailableDevicesCategory).removeAll();
    }

    @Test
    public void testUpdateContent_stateOn_addDevices() {
        mFragment.mAvailableDevicesCategory = mAvailableDevicesCategory;
        mFragment.mFooterPreference = mFooterPreference;
        doNothing().when(mFragment).addDeviceCategory(any(), anyInt(), any(), anyBoolean());

        mFragment.updateContent(BluetoothAdapter.STATE_ON);

        verify(mFragment).addDeviceCategory(mAvailableDevicesCategory,
                R.string.bluetooth_preference_found_devices,
                BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER, false);
        verify(mLocalAdapter).setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
    }

    @Test
    public void testUpdateContent_stateOff_finish() {
        mFragment.updateContent(BluetoothAdapter.STATE_OFF);

        verify(mFragment).finish();
    }

    @Test
    public void testOnScanningStateChanged_restartScanAfterInitialScanning() {
        mFragment.mAvailableDevicesCategory = mAvailableDevicesCategory;
        mFragment.mFooterPreference = mFooterPreference;
        mFragment.mDeviceListGroup = mAvailableDevicesCategory;
        doNothing().when(mFragment).addDeviceCategory(any(), anyInt(), any(), anyBoolean());

        // Initial Bluetooth ON will trigger scan enable, list clear and scan start
        mFragment.updateContent(BluetoothAdapter.STATE_ON);
        verify(mFragment).enableScanning();
        assertThat(mAvailableDevicesCategory.getPreferenceCount()).isEqualTo(0);
        verify(mLocalAdapter).startScanning(true);

        // Subsequent scan started event will not trigger start/stop nor list clear
        mFragment.onScanningStateChanged(true);
        verify(mLocalAdapter, times(1)).startScanning(anyBoolean());
        verify(mAvailableDevicesCategory, times(1)).setProgress(true);

        // Subsequent scan finished event will trigger scan start without list clean
        mFragment.onScanningStateChanged(false);
        verify(mLocalAdapter, times(2)).startScanning(true);
        verify(mAvailableDevicesCategory, times(2)).setProgress(true);

        // Subsequent scan started event will not trigger any change
        mFragment.onScanningStateChanged(true);
        verify(mLocalAdapter, times(2)).startScanning(anyBoolean());
        verify(mAvailableDevicesCategory, times(3)).setProgress(true);
        verify(mLocalAdapter, never()).stopScanning();

        // Disable scanning will trigger scan stop
        mFragment.disableScanning();
        verify(mLocalAdapter, times(1)).stopScanning();

        // Subsequent scan start event will not trigger any change besides progress circle
        mFragment.onScanningStateChanged(true);
        verify(mAvailableDevicesCategory, times(4)).setProgress(true);

        // However, subsequent scan finished event won't trigger new scan start and will stop
        // progress circle from spinning
        mFragment.onScanningStateChanged(false);
        verify(mAvailableDevicesCategory, times(1)).setProgress(false);
        verify(mLocalAdapter, times(2)).startScanning(anyBoolean());
        verify(mLocalAdapter, times(1)).stopScanning();

        // Verify that clean up only happen once at initialization
        verify(mAvailableDevicesCategory, times(1)).removeAll();
    }



}
