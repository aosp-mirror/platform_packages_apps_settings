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

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothFeatureProviderImplTest {
    private static final String PARAMETER_KEY = "addr";
    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    private BluetoothFeatureProvider mBluetoothFeatureProvider;

    @Before
    public void setUp() {
        mBluetoothFeatureProvider = new BluetoothFeatureProviderImpl(
                RuntimeEnvironment.application);
    }

    @Test
    public void getBluetoothDeviceSettingsUri_containCorrectMacAddress() {
        final Uri uri = mBluetoothFeatureProvider.getBluetoothDeviceSettingsUri(MAC_ADDRESS);
        assertThat(uri.getQueryParameterNames()).containsExactly(PARAMETER_KEY);
        assertThat(uri.getQueryParameter(PARAMETER_KEY)).isEqualTo(MAC_ADDRESS);
    }
}
