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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class BluetoothPairingDetailTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDeviceManager mDeviceManager;
    private BluetoothPairingDetail mFragment;
    private BluetoothProgressCategory mAvailableDevicesCategory;
    private FooterPreference mFooterPreference;
    private BluetoothAdapter mBluetoothAdapter;

    @Before
    public void setUp() {
        mFragment = spy(new BluetoothPairingDetail());
        doReturn(mContext).when(mFragment).getContext();
        mAvailableDevicesCategory = spy(new BluetoothProgressCategory(mContext));
        mFooterPreference = new FooterPreference(mContext);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        doReturn(mAvailableDevicesCategory).when(mFragment)
                .findPreference(BluetoothPairingDetail.KEY_AVAIL_DEVICES);
        doReturn(mFooterPreference).when(mFragment)
                .findPreference(BluetoothPairingDetail.KEY_FOOTER_PREF);
        doReturn(Collections.emptyList()).when(mDeviceManager).getCachedDevicesCopy();

        mFragment.mBluetoothAdapter = mBluetoothAdapter;
        mFragment.mLocalManager = mLocalManager;
        mFragment.mCachedDeviceManager = mDeviceManager;
        mFragment.mDeviceListGroup = mAvailableDevicesCategory;
        mFragment.onViewCreated(mFragment.getView(), Bundle.EMPTY);
    }
//
    @Test
    public void initPreferencesFromPreferenceScreen_findPreferences() {
        mFragment.initPreferencesFromPreferenceScreen();

        assertThat(mFragment.mAvailableDevicesCategory).isEqualTo(mAvailableDevicesCategory);
        assertThat(mFragment.mFooterPreference).isEqualTo(mFooterPreference);
    }

    @Test
    public void updateContent_stateOn_addDevices() {
        mFragment.initPreferencesFromPreferenceScreen();

        mFragment.updateContent(BluetoothAdapter.STATE_ON);

        assertThat(mFragment.mAlwaysDiscoverable.mStarted).isEqualTo(true);
        assertThat(mBluetoothAdapter.getScanMode())
                .isEqualTo(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
    }

    @Test
    public void onScanningStateChanged_restartScanAfterInitialScanning() {
        mFragment.initPreferencesFromPreferenceScreen();

        // Initial Bluetooth ON will trigger scan enable, list clear and scan start
        mFragment.updateContent(BluetoothAdapter.STATE_ON);
        verify(mFragment).enableScanning();
        assertThat(mAvailableDevicesCategory.getPreferenceCount()).isEqualTo(0);
        verify(mFragment).startScanning();

        // Subsequent scan started event will not trigger start/stop nor list clear
        mFragment.onScanningStateChanged(true);
        verify(mFragment, times(1)).startScanning();
        verify(mAvailableDevicesCategory, times(1)).setProgress(true);

        // Subsequent scan finished event will trigger scan start without list clean
        mFragment.onScanningStateChanged(false);
        verify(mFragment, times(2)).startScanning();
        verify(mAvailableDevicesCategory, times(2)).setProgress(true);

        // Subsequent scan started event will not trigger any change
        mFragment.onScanningStateChanged(true);
        verify(mFragment, times(2)).startScanning();
        verify(mAvailableDevicesCategory, times(3)).setProgress(true);
        verify(mFragment, never()).stopScanning();

        // Disable scanning will trigger scan stop
        mFragment.disableScanning();
        verify(mFragment, times(1)).stopScanning();

        // Subsequent scan start event will not trigger any change besides progress circle
        mFragment.onScanningStateChanged(true);
        verify(mAvailableDevicesCategory, times(4)).setProgress(true);

        // However, subsequent scan finished event won't trigger new scan start and will stop
        // progress circle from spinning
        mFragment.onScanningStateChanged(false);
        verify(mAvailableDevicesCategory, times(1)).setProgress(false);
        verify(mFragment, times(2)).startScanning();
        verify(mFragment, times(1)).stopScanning();

        // Verify that clean up only happen once at initialization
        verify(mAvailableDevicesCategory, times(1)).removeAll();
    }
}