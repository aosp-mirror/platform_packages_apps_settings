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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.res.Resources;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class DeviceListPreferenceFragmentTest {

    private static final String FOOTAGE_MAC_STRING = "Bluetooth mac: xxxx";

    @Mock
    private Resources mResource;
    @Mock
    private Context mContext;
    @Mock
    private BluetoothLeScanner mBluetoothLeScanner;

    private TestFragment mFragment;
    private Preference mMyDevicePreference;


    private BluetoothAdapter mBluetoothAdapter;
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestFragment());
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mResource).when(mFragment).getResources();
        mBluetoothAdapter = spy(BluetoothAdapter.getDefaultAdapter());
        mFragment.mBluetoothAdapter = mBluetoothAdapter;

        mMyDevicePreference = new Preference(RuntimeEnvironment.application);
    }

    @Test
    public void setUpdateMyDevicePreference_setTitleCorrectly() {
        doReturn(FOOTAGE_MAC_STRING).when(mFragment)
            .getString(eq(R.string.bluetooth_footer_mac_message), any());

        mFragment.updateFooterPreference(mMyDevicePreference);

        assertThat(mMyDevicePreference.getTitle()).isEqualTo(FOOTAGE_MAC_STRING);
    }

    @Test
    public void testEnableDisableScanning_testStateAfterEanbleDisable() {
        mFragment.enableScanning();
        verify(mFragment).startScanning();
        assertThat(mFragment.mScanEnabled).isTrue();

        mFragment.disableScanning();
        verify(mFragment).stopScanning();
        assertThat(mFragment.mScanEnabled).isFalse();
    }

    @Test
    public void testScanningStateChanged_testScanStarted() {
        mFragment.enableScanning();
        assertThat(mFragment.mScanEnabled).isTrue();
        verify(mFragment).startScanning();

        mFragment.onScanningStateChanged(true);
        verify(mFragment, times(1)).startScanning();
    }

    @Test
    public void testScanningStateChanged_testScanFinished() {
        // Could happen when last scanning not done while current scan gets enabled
        mFragment.enableScanning();
        verify(mFragment).startScanning();
        assertThat(mFragment.mScanEnabled).isTrue();

        mFragment.onScanningStateChanged(false);
        verify(mFragment, times(2)).startScanning();
    }

    @Test
    public void testScanningStateChanged_testScanStateMultiple() {
        // Could happen when last scanning not done while current scan gets enabled
        mFragment.enableScanning();
        assertThat(mFragment.mScanEnabled).isTrue();
        verify(mFragment).startScanning();

        mFragment.onScanningStateChanged(true);
        verify(mFragment, times(1)).startScanning();

        mFragment.onScanningStateChanged(false);
        verify(mFragment, times(2)).startScanning();

        mFragment.onScanningStateChanged(true);
        verify(mFragment, times(2)).startScanning();

        mFragment.disableScanning();
        verify(mFragment).stopScanning();

        mFragment.onScanningStateChanged(false);
        verify(mFragment, times(2)).startScanning();

        mFragment.onScanningStateChanged(true);
        verify(mFragment, times(2)).startScanning();
    }

    @Test
    public void testScanningStateChanged_testScanFinishedAfterDisable() {
        mFragment.enableScanning();
        verify(mFragment).startScanning();
        assertThat(mFragment.mScanEnabled).isTrue();

        mFragment.disableScanning();
        verify(mFragment).stopScanning();
        assertThat(mFragment.mScanEnabled).isFalse();

        mFragment.onScanningStateChanged(false);
        verify(mFragment, times(1)).startScanning();
    }

    @Test
    public void testScanningStateChanged_testScanStartedAfterDisable() {
        mFragment.enableScanning();
        verify(mFragment).startScanning();
        assertThat(mFragment.mScanEnabled).isTrue();

        mFragment.disableScanning();
        verify(mFragment).stopScanning();
        assertThat(mFragment.mScanEnabled).isFalse();

        mFragment.onScanningStateChanged(true);
        verify(mFragment, times(1)).startScanning();
    }

    @Test
    public void startScanning_setLeScanFilter_shouldStartLeScan() {
        final ScanFilter leScanFilter = new ScanFilter.Builder()
                .setServiceData(BluetoothUuid.HEARING_AID, new byte[]{0}, new byte[]{0})
                .build();
        doReturn(mBluetoothLeScanner).when(mBluetoothAdapter).getBluetoothLeScanner();

        mFragment.setFilter(Collections.singletonList(leScanFilter));
        mFragment.startScanning();

        verify(mBluetoothLeScanner).startScan(eq(Collections.singletonList(leScanFilter)),
                any(ScanSettings.class), any(ScanCallback.class));
    }

    /**
     * Fragment to test since {@code DeviceListPreferenceFragment} is abstract
     */
    public static class TestFragment extends DeviceListPreferenceFragment {

        public TestFragment() {
            super("");
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {}

        @Override
        protected void initPreferencesFromPreferenceScreen() {}

        @Override
        public String getDeviceListKey() {
            return null;
        }

        @Override
        protected String getLogTag() {
            return null;
        }

        @Override
        protected int getPreferenceScreenResId() {
            return 0;
        }

        @Override
        protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return null;
        }
    }
}
