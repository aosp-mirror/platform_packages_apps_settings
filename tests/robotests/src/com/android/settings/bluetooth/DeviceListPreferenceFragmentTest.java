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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DeviceListPreferenceFragmentTest {
    private static final String FOOTAGE_MAC_STRING = "Bluetooth mac: xxxx";

    @Mock
    private UserManager mUserManager;
    @Mock
    private Resources mResource;
    @Mock
    private Context mContext;
    @Mock
    private LocalBluetoothAdapter mLocalAdapter;
    private TestFragment mFragment;
    private Preference mMyDevicePreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestFragment());
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mResource).when(mFragment).getResources();
        mFragment.mLocalAdapter = mLocalAdapter;

        mMyDevicePreference = new Preference(RuntimeEnvironment.application);
    }

    @Test
    public void setUpdateMyDevicePreference_setTitleCorrectly() {
        doReturn(FOOTAGE_MAC_STRING).when(mFragment).getString(
                eq(R.string.bluetooth_footer_mac_message), any());

        mFragment.updateFooterPreference(mMyDevicePreference);

        assertThat(mMyDevicePreference.getTitle()).isEqualTo(FOOTAGE_MAC_STRING);
    }

    @Test
    public void testEnableDisableScanning_testStateAfterEanbleDisable() {
        mFragment.enableScanning();
        verify(mLocalAdapter).startScanning(true);
        assertThat(mFragment.mScanEnabled).isTrue();

        mFragment.disableScanning();
        verify(mLocalAdapter).stopScanning();
        assertThat(mFragment.mScanEnabled).isFalse();
    }

    @Test
    public void testScanningStateChanged_testScanStarted() {
        mFragment.enableScanning();
        assertThat(mFragment.mScanEnabled).isTrue();
        verify(mLocalAdapter).startScanning(true);

        mFragment.onScanningStateChanged(true);
        verify(mLocalAdapter, times(1)).startScanning(anyBoolean());
    }

    @Test
    public void testScanningStateChanged_testScanFinished() {
        // Could happen when last scanning not done while current scan gets enabled
        mFragment.enableScanning();
        verify(mLocalAdapter).startScanning(true);
        assertThat(mFragment.mScanEnabled).isTrue();

        mFragment.onScanningStateChanged(false);
        verify(mLocalAdapter, times(2)).startScanning(true);
    }

    @Test
    public void testScanningStateChanged_testScanStateMultiple() {
        // Could happen when last scanning not done while current scan gets enabled
        mFragment.enableScanning();
        assertThat(mFragment.mScanEnabled).isTrue();
        verify(mLocalAdapter).startScanning(true);

        mFragment.onScanningStateChanged(true);
        verify(mLocalAdapter, times(1)).startScanning(anyBoolean());

        mFragment.onScanningStateChanged(false);
        verify(mLocalAdapter, times(2)).startScanning(true);

        mFragment.onScanningStateChanged(true);
        verify(mLocalAdapter, times(2)).startScanning(anyBoolean());

        mFragment.disableScanning();
        verify(mLocalAdapter).stopScanning();

        mFragment.onScanningStateChanged(false);
        verify(mLocalAdapter, times(2)).startScanning(anyBoolean());

        mFragment.onScanningStateChanged(true);
        verify(mLocalAdapter, times(2)).startScanning(anyBoolean());
    }

    @Test
    public void testScanningStateChanged_testScanFinishedAfterDisable() {
        mFragment.enableScanning();
        verify(mLocalAdapter).startScanning(true);
        assertThat(mFragment.mScanEnabled).isTrue();

        mFragment.disableScanning();
        verify(mLocalAdapter).stopScanning();
        assertThat(mFragment.mScanEnabled).isFalse();

        mFragment.onScanningStateChanged(false);
        verify(mLocalAdapter, times(1)).startScanning(anyBoolean());
    }

    @Test
    public void testScanningStateChanged_testScanStartedAfterDisable() {
        mFragment.enableScanning();
        verify(mLocalAdapter).startScanning(true);
        assertThat(mFragment.mScanEnabled).isTrue();

        mFragment.disableScanning();
        verify(mLocalAdapter).stopScanning();
        assertThat(mFragment.mScanEnabled).isFalse();

        mFragment.onScanningStateChanged(true);
        verify(mLocalAdapter, times(1)).startScanning(anyBoolean());
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
        void initPreferencesFromPreferenceScreen() {}

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
        protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
            return null;
        }
    }

}
