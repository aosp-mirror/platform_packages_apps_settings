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

package com.android.settings.media;

import static android.app.slice.Slice.EXTRA_RANGE_VALUE;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import static com.android.settings.slices.CustomSliceRegistry.REMOTE_MEDIA_SLICE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.slices.SliceBackgroundWorker;
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
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = RemoteMediaSliceTest.ShadowSliceBackgroundWorker.class)
public class RemoteMediaSliceTest {

    private static final String MEDIA_ID = "media_id";
    private static final String TEST_PACKAGE_LABEL = "music";
    private static final String TEST_DEVICE_1_ID = "test_device_1_id";
    private static final String TEST_DEVICE_1_NAME = "test_device_1_name";
    private static final int TEST_VOLUME = 3;

    private static MediaDeviceUpdateWorker sMediaDeviceUpdateWorker;

    @Mock
    private LocalMediaManager mLocalMediaManager;
    @Mock
    private MediaDevice mDevice;

    private final List<MediaDevice> mDevices = new ArrayList<>();

    private Context mContext;
    private RemoteMediaSlice mRemoteMediaSlice;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mRemoteMediaSlice = new RemoteMediaSlice(mContext);
        sMediaDeviceUpdateWorker = spy(new MediaDeviceUpdateWorker(mContext,
                REMOTE_MEDIA_SLICE_URI));
        sMediaDeviceUpdateWorker.mLocalMediaManager = mLocalMediaManager;
        when(sMediaDeviceUpdateWorker.getActiveMediaDevice(
                MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE)).thenReturn(mDevices);
        when(mDevice.getId()).thenReturn(TEST_DEVICE_1_ID);
        when(mDevice.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(mDevice.getMaxVolume()).thenReturn(100);
        when(mDevice.getCurrentVolume()).thenReturn(10);
        when(mDevice.getClientAppLabel()).thenReturn(TEST_PACKAGE_LABEL);
    }

    @Test
    public void onNotifyChange_noId_doNothing() {
        mDevices.add(mDevice);
        when(mLocalMediaManager.getMediaDeviceById(mDevices, TEST_DEVICE_1_ID)).thenReturn(mDevice);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_RANGE_VALUE, TEST_VOLUME);

        mRemoteMediaSlice.onNotifyChange(intent);

        verify(mDevice, never()).requestSetVolume(anyInt());
    }

    @Test
    public void onNotifyChange_verifyAdjustVolume() {
        mDevices.add(mDevice);
        when(mLocalMediaManager.getMediaDeviceById(mDevices, TEST_DEVICE_1_ID)).thenReturn(mDevice);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);
        final Intent intent = new Intent();
        intent.putExtra(MEDIA_ID, TEST_DEVICE_1_ID);
        intent.putExtra(EXTRA_RANGE_VALUE, TEST_VOLUME);

        mRemoteMediaSlice.onNotifyChange(intent);

        verify(mDevice).requestSetVolume(TEST_VOLUME);
    }

    @Test
    public void getSlice_noActiveDevice_checkRowNumber() {
        final Slice slice = mRemoteMediaSlice.getSlice();
        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM, null).size();

        assertThat(rows).isEqualTo(0);
    }

    @Test
    public void getSlice_withActiveDevice_checkRowNumber() {
        mDevices.add(mDevice);
        final Slice slice = mRemoteMediaSlice.getSlice();
        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM, null).size();

        // InputRange and Row
        assertThat(rows).isEqualTo(2);
    }

    @Test
    public void getSlice_withActiveDevice_checkTitle() {
        mDevices.add(mDevice);
        final Slice slice = mRemoteMediaSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();

        assertThat(primaryAction.getTitle().toString()).isEqualTo(mContext.getText(
                com.android.settings.R.string.remote_media_volume_option_title)
                + " (" + TEST_PACKAGE_LABEL + ")");
    }

    @Implements(SliceBackgroundWorker.class)
    public static class ShadowSliceBackgroundWorker {

        @Implementation
        public static SliceBackgroundWorker getInstance(Uri uri) {
            return sMediaDeviceUpdateWorker;
        }
    }
}
