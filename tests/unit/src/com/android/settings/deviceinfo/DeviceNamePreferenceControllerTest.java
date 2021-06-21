/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.deviceinfo.DeviceNamePreferenceController.RES_SHOW_DEVICE_NAME_BOOL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.widget.ValidatedEditTextPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class DeviceNamePreferenceControllerTest {
    private static final String TESTING_STRING = "Testing";
    private static final String TEST_PREFERENCE_KEY = "test_key";

    @Mock
    private WifiManager mWifiManager;
    private PreferenceScreen mScreen;
    private ValidatedEditTextPreference mPreference;
    private DeviceNamePreferenceController mController;
    private Context mContext;
    private Resources mResources;
    private BluetoothAdapter mBluetoothAdapter;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new ValidatedEditTextPreference(mContext);
        mPreference.setKey(TEST_PREFERENCE_KEY);
        mScreen.addPreference(mPreference);

        final SoftApConfiguration configuration =
                new SoftApConfiguration.Builder().setSsid("test-ap").build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(configuration);

        mController = new DeviceNamePreferenceController(mContext, TEST_PREFERENCE_KEY);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @After
    public void tearDown() {
        Settings.Global.putString(
                mContext.getContentResolver(), Settings.Global.DEVICE_NAME, null);
    }

    @Test
    public void getAvailibilityStatus_availableByDefault() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_unsupportedWhenSet() {
        doReturn(false).when(mResources).getBoolean(RES_SHOW_DEVICE_NAME_BOOL);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void constructor_defaultDeviceNameIsModelName() {
        assertThat(mController.getSummary()).isEqualTo(Build.MODEL);
    }

    @Test
    public void constructor_deviceNameLoadedIfSet() {
        Settings.Global.putString(
                mContext.getContentResolver(), Settings.Global.DEVICE_NAME, "Test");
        mController = new DeviceNamePreferenceController(mContext, "test_key");
        assertThat(mController.getSummary()).isEqualTo("Test");
    }

    @Test
    public void isTextValid_nameUnder33Characters_isValid() {
        assertThat(mController.isTextValid("12345678901234567890123456789012")).isTrue();
    }

    @Test
    public void isTextValid_nameTooLong_isInvalid() {
        assertThat(mController.isTextValid("123456789012345678901234567890123")).isFalse();
    }

    @Test
    public void setDeviceName_preferenceUpdatedWhenDeviceNameUpdated() {
        acceptDeviceName(true);
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        assertThat(mPreference.getSummary()).isEqualTo(TESTING_STRING);
    }

    // TODO(b/175389659): Determine why this test case fails for virtual but not local devices.
    @Ignore
    @Test
    public void setDeviceName_bluetoothNameUpdatedWhenDeviceNameUpdated() {
        acceptDeviceName(true);
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        assertThat(mBluetoothAdapter.getName()).isEqualTo(TESTING_STRING);
    }

    @Test
    public void setDeviceName_wifiTetherNameUpdatedWhenDeviceNameUpdated() {
        acceptDeviceName(true);
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        ArgumentCaptor<SoftApConfiguration> captor =
                ArgumentCaptor.forClass(SoftApConfiguration.class);
        verify(mWifiManager).setSoftApConfiguration(captor.capture());
        assertThat(captor.getValue().getSsid()).isEqualTo(TESTING_STRING);
    }

    @Test
    public void displayPreference_defaultDeviceNameIsModelNameOnPreference() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getText()).isEqualTo(Build.MODEL);
    }

    @Test
    public void setDeviceName_okInDeviceNameWarningDialog_shouldChangePreferenceText() {
        acceptDeviceName(true);
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        assertThat(mPreference.getSummary()).isEqualTo(TESTING_STRING);
    }

    @Test
    public void setDeviceName_cancelInDeviceNameWarningDialog_shouldNotChangePreferenceText() {
        acceptDeviceName(false);
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        assertThat(mPreference.getSummary()).isNotEqualTo(TESTING_STRING);
        assertThat(mPreference.getText()).isEqualTo(mPreference.getSummary());
    }

    private void acceptDeviceName(boolean accept) {
        mController.setHost(deviceName -> mController.updateDeviceName(accept));
    }
}
