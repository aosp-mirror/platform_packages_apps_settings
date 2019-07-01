/*
 * Copyright (C) 2019 The Android Open Source Project
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
 *
 */

package com.android.settings.media;

import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.telephony.TelephonyManager;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowTelephonyManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowTelephonyManager.class})
public class MediaOutputSliceTest {

    private static final String TEST_PACKAGE_NAME = "com.fake.android.music";
    private static final String TEST_DEVICE_1_ID = "test_device_1_id";
    private static final String TEST_DEVICE_1_NAME = "test_device_1_name";
    private static final int TEST_DEVICE_1_ICON =
            com.android.internal.R.drawable.ic_bt_headphones_a2dp;

    @Mock
    private LocalMediaManager mLocalMediaManager;
    @Mock
    private Drawable mTestDrawable;

    private final List<MediaDevice> mDevices = new ArrayList<>();

    private Context mContext;
    private MediaOutputSlice mMediaOutputSlice;
    private MediaDeviceUpdateWorker mMediaDeviceUpdateWorker;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private ShadowTelephonyManager mShadowTelephonyManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mShadowTelephonyManager = Shadow.extract(mContext.getSystemService(
                Context.TELEPHONY_SERVICE));

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        // Setup BluetoothAdapter
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowTelephonyManager.setCallState(TelephonyManager.CALL_STATE_IDLE);

        mMediaOutputSlice = new MediaOutputSlice(mContext);
        mMediaDeviceUpdateWorker = new MediaDeviceUpdateWorker(mContext, MEDIA_OUTPUT_SLICE_URI);
        mMediaDeviceUpdateWorker.setPackageName(TEST_PACKAGE_NAME);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);
        mMediaDeviceUpdateWorker.mLocalMediaManager = mLocalMediaManager;
        mMediaOutputSlice.init(TEST_PACKAGE_NAME, mMediaDeviceUpdateWorker);
    }

    @Test
    public void getSlice_workerIsNull_shouldReturnZeroRow() {
        mMediaOutputSlice.init(TEST_PACKAGE_NAME, null);

        final Slice slice = mMediaOutputSlice.getSlice();

        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();

        assertThat(rows).isEqualTo(0);
    }

    @Test
    public void getSlice_bluetoothIsDisable_shouldReturnZeroRow() {
        mShadowBluetoothAdapter.setEnabled(false);

        final Slice slice = mMediaOutputSlice.getSlice();

        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();

        assertThat(rows).isEqualTo(0);
    }

    @Test
    public void getSlice_callStateRinging_shouldReturnZeroRow() {
        mShadowTelephonyManager.setCallState(TelephonyManager.CALL_STATE_RINGING);

        final Slice slice = mMediaOutputSlice.getSlice();

        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();

        assertThat(rows).isEqualTo(0);
    }

    @Test
    public void getSlice_shouldHaveActiveDeviceName() {
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        assertThat(primaryAction.getTitle().toString()).isEqualTo(TEST_DEVICE_1_NAME);
    }

    @Test
    public void onNotifyChange_foundMediaDevice_connect() {
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        when(mLocalMediaManager.getMediaDeviceById(mDevices, TEST_DEVICE_1_ID)).thenReturn(device);
        mDevices.add(device);

        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Intent intent = new Intent();
        intent.putExtra("media_device_id", TEST_DEVICE_1_ID);

        mMediaOutputSlice.onNotifyChange(intent);

        verify(mLocalMediaManager).connectDevice(device);
    }

    @Test
    public void onNotifyChange_notFoundMediaDevice_doNothing() {
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        when(mLocalMediaManager.getMediaDeviceById(mDevices, TEST_DEVICE_1_ID)).thenReturn(device);
        mDevices.add(device);

        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Intent intent = new Intent();
        intent.putExtra("media_device_id", "fake_123");

        mMediaOutputSlice.onNotifyChange(intent);

        verify(mLocalMediaManager, never()).connectDevice(device);
    }
}
