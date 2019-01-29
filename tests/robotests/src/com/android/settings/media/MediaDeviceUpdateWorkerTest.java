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
 */

package com.android.settings.media;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.android.settingslib.media.MediaDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MediaDeviceUpdateWorkerTest {

    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private static final String TEST_DEVICE_1_ID = "test_device_1_id";
    private static final String TEST_DEVICE_2_ID = "test_device_2_id";
    private static final String TEST_DEVICE_3_ID = "test_device_3_id";

    private final List<MediaDevice> mMediaDevices = new ArrayList<>();

    private MediaDeviceUpdateWorker mMediaDeviceUpdateWorker;
    private ContentResolver mResolver;
    private Context mContext;
    private MediaDevice mMediaDevice1;
    private MediaDevice mMediaDevice2;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mMediaDeviceUpdateWorker = new MediaDeviceUpdateWorker(mContext, URI);
        mResolver = mock(ContentResolver.class);

        mMediaDevice1 = mock(MediaDevice.class);
        when(mMediaDevice1.getId()).thenReturn(TEST_DEVICE_1_ID);
        mMediaDevice2 = mock(MediaDevice.class);
        when(mMediaDevice2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mMediaDevices.add(mMediaDevice1);
        mMediaDevices.add(mMediaDevice2);

        doReturn(mResolver).when(mContext).getContentResolver();
    }

    @Test
    public void onDeviceListUpdate_shouldNotifyChange() {
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mMediaDevices);

        verify(mResolver).notifyChange(URI, null);
    }

    @Test
    public void onSelectedDeviceStateChanged_shouldNotifyChange() {
        mMediaDeviceUpdateWorker.onSelectedDeviceStateChanged(null, 0);

        verify(mResolver).notifyChange(URI, null);
    }

    @Test
    public void onDeviceListUpdate_sameDeviceList_shouldBeEqual() {
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mMediaDevices);

        final List<MediaDevice> newDevices = new ArrayList<>();
        newDevices.add(mMediaDevice1);
        newDevices.add(mMediaDevice2);

        mMediaDeviceUpdateWorker.onDeviceListUpdate(newDevices);
        final List<MediaDevice> devices = mMediaDeviceUpdateWorker.getMediaDevices();

        assertThat(devices.get(0).getId()).isEqualTo(newDevices.get(0).getId());
        assertThat(devices.get(1).getId()).isEqualTo(newDevices.get(1).getId());
    }

    @Test
    public void onDeviceListUpdate_add1DeviceToDeviceList_shouldBeEqual() {
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mMediaDevices);

        final List<MediaDevice> newDevices = new ArrayList<>();
        final MediaDevice device3 = mock(MediaDevice.class);
        when(mMediaDevice2.getId()).thenReturn(TEST_DEVICE_3_ID);
        newDevices.add(mMediaDevice1);
        newDevices.add(mMediaDevice2);
        newDevices.add(device3);

        mMediaDeviceUpdateWorker.onDeviceListUpdate(newDevices);
        final List<MediaDevice> devices = mMediaDeviceUpdateWorker.getMediaDevices();

        assertThat(devices.size()).isEqualTo(newDevices.size());
    }

    @Test
    public void onDeviceListUpdate_less1DeviceToDeviceList_shouldBeEqual() {
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mMediaDevices);

        final List<MediaDevice> newDevices = new ArrayList<>();
        newDevices.add(mMediaDevice1);

        mMediaDeviceUpdateWorker.onDeviceListUpdate(newDevices);
        final List<MediaDevice> devices = mMediaDeviceUpdateWorker.getMediaDevices();

        assertThat(devices.size()).isEqualTo(newDevices.size());
    }
}
