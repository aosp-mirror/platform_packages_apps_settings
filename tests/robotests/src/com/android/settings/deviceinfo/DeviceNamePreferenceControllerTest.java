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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.widget.ValidatedEditTextPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class DeviceNamePreferenceControllerTest {
    private static final String TESTING_STRING = "Testing";

    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    private ValidatedEditTextPreference mPreference;
    private DeviceNamePreferenceController mController;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.WIFI_SERVICE, mWifiManager);
        mContext = RuntimeEnvironment.application;
        mPreference = new ValidatedEditTextPreference(mContext);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        final SoftApConfiguration configuration =
                new SoftApConfiguration.Builder().setSsid("test-ap").build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(configuration);

        mController = new DeviceNamePreferenceController(mContext, "test_key");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Test
    public void getAvailibilityStatus_availableByDefault() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_unsupportedWhenSet() {
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
    public void setDeviceName_ignoresIfCancelPressed() {
        acceptDeviceName(true);
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        assertThat(mBluetoothAdapter.getName()).isEqualTo(TESTING_STRING);
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
