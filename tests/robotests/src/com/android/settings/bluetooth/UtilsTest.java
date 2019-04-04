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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class UtilsTest {

    private static final String STRING_METADATA = "string_metadata";
    private static final String BOOL_METADATA = "true";
    private static final String INT_METADATA = "25";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private BluetoothDevice mBluetoothDevice;

    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mMetricsFeatureProvider = FakeFeatureFactory.setupForTest().getMetricsFeatureProvider();
    }

    @Test
    public void showConnectingError_shouldLogBluetoothConnectError() {
        when(mContext.getString(anyInt(), anyString())).thenReturn("testMessage");
        Utils.showConnectingError(mContext, "testName", mock(LocalBluetoothManager.class));

        verify(mMetricsFeatureProvider).visible(eq(mContext), anyInt(),
                eq(MetricsEvent.ACTION_SETTINGS_BLUETOOTH_CONNECT_ERROR));
    }

    @Test
    public void getStringMetaData_hasMetaData_getCorrectMetaData() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTHETHERED_LEFT_ICON)).thenReturn(STRING_METADATA);

        assertThat(Utils.getStringMetaData(mBluetoothDevice,
                BluetoothDevice.METADATA_UNTHETHERED_LEFT_ICON)).isEqualTo(STRING_METADATA);
    }

    @Test
    public void getIntMetaData_hasMetaData_getCorrectMetaData() {
        when(mBluetoothDevice.getMetadata(
            BluetoothDevice.METADATA_UNTHETHERED_LEFT_BATTERY)).thenReturn(INT_METADATA);

        assertThat(Utils.getIntMetaData(mBluetoothDevice,
            BluetoothDevice.METADATA_UNTHETHERED_LEFT_BATTERY))
            .isEqualTo(Integer.parseInt(INT_METADATA));
    }

    @Test
    public void getIntMetaData_invalidMetaData_getErrorCode() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTHETHERED_LEFT_BATTERY)).thenReturn(STRING_METADATA);

        assertThat(Utils.getIntMetaData(mBluetoothDevice,
                BluetoothDevice.METADATA_UNTHETHERED_LEFT_ICON)).isEqualTo(Utils.META_INT_ERROR);
    }

    @Test
    public void getBooleanMetaData_hasMetaData_getCorrectMetaData() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET)).thenReturn(BOOL_METADATA);

        assertThat(Utils.getBooleanMetaData(mBluetoothDevice,
                BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET)).isEqualTo(true);
    }
}
