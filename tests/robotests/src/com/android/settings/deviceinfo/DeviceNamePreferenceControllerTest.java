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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
public class DeviceNamePreferenceControllerTest {
    private static final String TESTING_STRING = "Testing";

    @Mock
    private LocalBluetoothAdapter mBluetoothAdapter;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    private ValidatedEditTextPreference mPreference;
    private DeviceNamePreferenceController mController;
    private Context mContext;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.WIFI_SERVICE, mWifiManager);
        mContext = shadowApplication.getApplicationContext();
        mPreference = new ValidatedEditTextPreference(mContext);
        when(mBluetoothManager.getBluetoothAdapter()).thenReturn(mBluetoothAdapter);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        final WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = "test-ap";
        when(mWifiManager.getWifiApConfiguration()).thenReturn(configuration);

        mController = new DeviceNamePreferenceController(mContext);
        mController.setLocalBluetoothManager(mBluetoothManager);
    }

    @Test
    public void constructor_defaultDeviceNameIsModelName() {
        assertThat(mController.getSummary()).isEqualTo(Build.MODEL);
    }

    @Test
    public void constructor_deviceNameLoadedIfSet() {
        Settings.Global.putString(
                mContext.getContentResolver(), Settings.Global.DEVICE_NAME, "Test");
        mController = new DeviceNamePreferenceController(mContext);
        mController.setLocalBluetoothManager(mBluetoothManager);
        assertThat(mController.getSummary()).isEqualTo("Test");
    }

    @Test
    public void isTextValid_nameUnder33CharactersIsValid() {
        assertThat(mController.isTextValid("12345678901234567890123456789012")).isTrue();
    }

    @Test
    public void isTextValid_nameTooLongIsInvalid() {
        assertThat(mController.isTextValid("123456789012345678901234567890123")).isFalse();
    }

    @Test
    public void setDeviceName_preferenceUpdatedWhenDeviceNameUpdated() {
        forceAcceptDeviceName();
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        assertThat(mPreference.getSummary()).isEqualTo(TESTING_STRING);
    }

    @Test
    public void setDeviceName_bluetoothNameUpdatedWhenDeviceNameUpdated() {
        forceAcceptDeviceName();
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        verify(mBluetoothAdapter).setName(eq(TESTING_STRING));
    }

    @Test
    public void setDeviceName_wifiTetherNameUpdatedWhenDeviceNameUpdated() {
        forceAcceptDeviceName();
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        ArgumentCaptor<WifiConfiguration> captor = ArgumentCaptor.forClass(WifiConfiguration.class);
        verify(mWifiManager).setWifiApConfiguration(captor.capture());
        assertThat(captor.getValue().SSID).isEqualTo(TESTING_STRING);
    }

    @Test
    public void displayPreference_defaultDeviceNameIsModelNameOnPreference() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getText()).isEqualTo(Build.MODEL);
    }

    @Test
    public void setDeviceName_ignoresIfCancelPressed() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, TESTING_STRING);

        verify(mBluetoothAdapter, never()).setName(eq(TESTING_STRING));
    }

    private void forceAcceptDeviceName() {
        mController.setHost(
                new DeviceNamePreferenceController.DeviceNamePreferenceHost() {
                    @Override
                    public void showDeviceNameWarningDialog(String deviceName) {
                        mController.confirmDeviceName();
                    }
                });
    }

}
