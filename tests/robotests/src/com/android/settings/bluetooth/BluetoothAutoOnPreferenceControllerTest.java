/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.settings.bluetooth.BluetoothAutoOnPreferenceController.PREF_KEY;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BluetoothAutoOnPreferenceControllerTest {
    private Context mContext;
    private ContentResolver mContentResolver;
    private BluetoothAutoOnPreferenceController mController;
    private BluetoothAdapter mBluetoothAdapter;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mContentResolver = mContext.getContentResolver();
        mController = new BluetoothAutoOnPreferenceController(mContext, PREF_KEY);
        mBluetoothAdapter = spy(BluetoothAdapter.getDefaultAdapter());
        mController.mBluetoothAdapter = mBluetoothAdapter;
    }

    @Test
    public void getAvailability_valueUnset_returnUnsupported() {
        doReturn(false).when(mBluetoothAdapter).isAutoOnSupported();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailability_valueSet_returnAvailable() {
        doReturn(true).when(mBluetoothAdapter).isAutoOnSupported();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isChecked_valueEnabled_returnTrue() {
        doReturn(true).when(mBluetoothAdapter).isAutoOnSupported();
        doReturn(true).when(mBluetoothAdapter).isAutoOnEnabled();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isChecked()).isEqualTo(true);
    }
}
