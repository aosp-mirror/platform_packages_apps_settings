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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplicationPackageManager;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowApplicationPackageManager.class)
public class AddDevicePreferenceControllerTest {
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;

    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    private AddDevicePreferenceController mAddDevicePreferenceController;
    private RestrictedPreference mAddDevicePreference;
    private ShadowApplicationPackageManager mPackageManager;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mPackageManager = (ShadowApplicationPackageManager) shadowOf(
                mContext.getPackageManager());
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true);

        mAddDevicePreferenceController = new AddDevicePreferenceController(mContext,
                "add_bt_devices");
        ReflectionHelpers.setField(mAddDevicePreferenceController,
                "mBluetoothAdapter", mBluetoothAdapter);

        String key = mAddDevicePreferenceController.getPreferenceKey();
        mAddDevicePreference = new RestrictedPreference(mContext);
        mAddDevicePreference.setKey(key);
        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mScreen.findPreference(key)).thenReturn(mAddDevicePreference);
        mAddDevicePreferenceController.displayPreference(mScreen);
    }

    @Test
    public void addDevice_bt_resume_on_then_off() {
        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        mAddDevicePreferenceController.updateState(mAddDevicePreference);
        assertTrue(TextUtils.isEmpty(mAddDevicePreference.getSummary()));

        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        BroadcastReceiver receiver = ReflectionHelpers.getField(
                mAddDevicePreferenceController, "mReceiver");
        when(mBluetoothAdapter.isEnabled()).thenReturn(false);
        receiver.onReceive(mContext, intent);
        assertThat(mAddDevicePreference.getSummary()).isEqualTo(
                mContext.getString(R.string.connected_device_add_device_summary));
    }

    @Test
    public void addDevice_bt_resume_off_then_on() {
        when(mBluetoothAdapter.isEnabled()).thenReturn(false);
        mAddDevicePreferenceController.updateState(mAddDevicePreference);
        assertThat(mAddDevicePreference.getSummary()).isEqualTo(
                mContext.getString(R.string.connected_device_add_device_summary));

        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        BroadcastReceiver receiver = ReflectionHelpers.getField(
                mAddDevicePreferenceController, "mReceiver");
        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        receiver.onReceive(mContext, intent);
        assertTrue(TextUtils.isEmpty(mAddDevicePreference.getSummary()));
    }

    @Test
    public void addDevice_Availability_UnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        assertThat(mAddDevicePreferenceController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void addDevice_Availability_Supported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true);
        assertThat(mAddDevicePreferenceController.getAvailabilityStatus())
                .isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noBluetoothFeature_unSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);

        assertThat(mAddDevicePreferenceController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
