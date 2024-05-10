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
package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BluetoothFeatureProviderImplTest {
    private static final String SETTINGS_URI = "content://test.provider/settings_uri";
    private static final String CONTROL_METADATA =
            "<HEARABLE_CONTROL_SLICE_WITH_WIDTH>" + SETTINGS_URI
                    + "</HEARABLE_CONTROL_SLICE_WITH_WIDTH>";
    private static final int METADATA_FAST_PAIR_CUSTOMIZED_FIELDS = 25;

    private BluetoothFeatureProvider mBluetoothFeatureProvider;

    @Mock
    private BluetoothDevice mBluetoothDevice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mBluetoothFeatureProvider = new BluetoothFeatureProviderImpl();
    }

    @Test
    public void getBluetoothDeviceSettingsUri_containCorrectMacAddress() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI)).thenReturn(
                SETTINGS_URI.getBytes());
        final Uri uri = mBluetoothFeatureProvider.getBluetoothDeviceSettingsUri(mBluetoothDevice);
        assertThat(uri.toString()).isEqualTo(SETTINGS_URI);
    }

    @Test
    public void getBluetoothDeviceControlUri_returnsCorrectUri() {
        when(mBluetoothDevice.getMetadata(METADATA_FAST_PAIR_CUSTOMIZED_FIELDS)).thenReturn(
                CONTROL_METADATA.getBytes());
        assertThat(
                mBluetoothFeatureProvider.getBluetoothDeviceControlUri(mBluetoothDevice)).isEqualTo(
                SETTINGS_URI);
    }
}
