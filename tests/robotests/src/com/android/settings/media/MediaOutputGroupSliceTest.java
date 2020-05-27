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

import static com.android.settings.media.MediaOutputGroupSlice.ACTION_MEDIA_SESSION_OPERATION;
import static com.android.settings.media.MediaOutputGroupSlice.ACTION_VOLUME_ADJUSTMENT;
import static com.android.settings.media.MediaOutputGroupSlice.CUSTOMIZED_ACTION;
import static com.android.settings.media.MediaOutputGroupSlice.GROUP_DEVICES;
import static com.android.settings.media.MediaOutputGroupSlice.MEDIA_DEVICE_ID;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_GROUP_SLICE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
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
@Config(shadows = MediaOutputGroupSliceTest.ShadowSliceBackgroundWorker.class)
public class MediaOutputGroupSliceTest {

    private static final String TEST_PACKAGE_NAME = "com.test.music";
    private static final String TEST_PACKAGE_NAME2 = "com.test.music2";
    private static final String TEST_DEVICE_1_ID = "test_device_1_id";
    private static final String TEST_DEVICE_1_NAME = "test_device_1_name";
    private static final String TEST_DEVICE_2_ID = "test_device_2_id";
    private static final String TEST_DEVICE_2_NAME = "test_device_2_name";
    private static final int TEST_VOLUME = 3;

    private static MediaDeviceUpdateWorker sMediaDeviceUpdateWorker;

    @Mock
    private LocalMediaManager mLocalMediaManager;
    @Mock
    private MediaDevice mDevice1;
    @Mock
    private MediaDevice mDevice2;

    private final List<MediaDevice> mSelectableDevices = new ArrayList<>();
    private final List<MediaDevice> mSelectedDevices = new ArrayList<>();
    private final List<MediaDevice> mDeselectableDevices = new ArrayList<>();

    private Context mContext;
    private MediaOutputGroupSlice mMediaOutputGroupSlice;
    private Drawable mDrawable;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mMediaOutputGroupSlice = new MediaOutputGroupSlice(mContext);
        sMediaDeviceUpdateWorker = spy(new MediaDeviceUpdateWorker(mContext,
                MEDIA_OUTPUT_GROUP_SLICE_URI));
        sMediaDeviceUpdateWorker.mLocalMediaManager = mLocalMediaManager;
        when(sMediaDeviceUpdateWorker.getPackageName()).thenReturn(TEST_PACKAGE_NAME);
        mDrawable = mContext.getDrawable(R.drawable.ic_check_box_blue_24dp);
        when(sMediaDeviceUpdateWorker.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        doReturn(false).when(sMediaDeviceUpdateWorker).hasAdjustVolumeUserRestriction();
        when(mDevice1.getId()).thenReturn(TEST_DEVICE_1_ID);
        when(mDevice1.getIcon()).thenReturn(mDrawable);
        when(mDevice1.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(mDevice1.getMaxVolume()).thenReturn(100);
        when(mDevice1.getCurrentVolume()).thenReturn(10);
        when(mDevice1.getClientPackageName()).thenReturn(TEST_PACKAGE_NAME);
        when(mDevice2.getId()).thenReturn(TEST_DEVICE_2_ID);
        when(mDevice2.getIcon()).thenReturn(mDrawable);
        when(mDevice2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(mDevice2.getMaxVolume()).thenReturn(100);
        when(mDevice2.getCurrentVolume()).thenReturn(20);
    }

    @Test
    public void getSlice_noMatchedDevice_doNothing() {
        mSelectableDevices.add(mDevice1);
        mSelectedDevices.add(mDevice1);
        when(mLocalMediaManager.getMediaDeviceById(mSelectableDevices, TEST_DEVICE_1_ID))
                .thenReturn(mDevice1);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mSelectableDevices);
        when(sMediaDeviceUpdateWorker.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_RANGE_VALUE, TEST_VOLUME);
        intent.putExtra(MEDIA_DEVICE_ID, TEST_DEVICE_2_ID);
        intent.putExtra(CUSTOMIZED_ACTION, ACTION_VOLUME_ADJUSTMENT);

        mMediaOutputGroupSlice.onNotifyChange(intent);

        verify(sMediaDeviceUpdateWorker, never()).adjustSessionVolume(anyInt());
        verify(mDevice1, never()).requestSetVolume(TEST_VOLUME);
    }

    @Test
    public void getSlice_withOneSelectableDevice_checkRowNumber() {
        mSelectableDevices.add(mDevice1);
        mSelectedDevices.add(mDevice2);
        when(sMediaDeviceUpdateWorker.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(sMediaDeviceUpdateWorker.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        final Slice slice = mMediaOutputGroupSlice.getSlice();
        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM, null).size();

        // Group item and 2 * InputRange
        assertThat(rows).isEqualTo(3);
    }

    @Test
    public void getSlice_nullWorker_noException() {
        sMediaDeviceUpdateWorker = null;
        mMediaOutputGroupSlice.getSlice();
    }

    @Test
    public void getSlice_withOneSelectableDevice_checkTitle() {
        mSelectableDevices.add(mDevice1);
        mSelectedDevices.add(mDevice1);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mSelectableDevices);
        when(sMediaDeviceUpdateWorker.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        final Slice slice = mMediaOutputGroupSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();

        assertThat(primaryAction.getTitle().toString()).isEqualTo(GROUP_DEVICES);
    }

    @Test
    public void onNotifyChange_verifyAdjustDeviceVolume() {
        mSelectableDevices.add(mDevice1);
        mSelectedDevices.add(mDevice1);
        when(mLocalMediaManager.getMediaDeviceById(mSelectableDevices, TEST_DEVICE_1_ID))
                .thenReturn(mDevice1);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mSelectableDevices);
        when(sMediaDeviceUpdateWorker.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_RANGE_VALUE, TEST_VOLUME);
        intent.putExtra(MEDIA_DEVICE_ID, TEST_DEVICE_1_ID);
        intent.putExtra(CUSTOMIZED_ACTION, ACTION_VOLUME_ADJUSTMENT);

        mMediaOutputGroupSlice.onNotifyChange(intent);

        verify(mDevice1).requestSetVolume(TEST_VOLUME);
    }

    @Test
    public void onNotifyChange_verifyAdjustGroupVolume() {
        mSelectableDevices.add(mDevice1);
        mSelectedDevices.add(mDevice1);
        when(mLocalMediaManager.getMediaDeviceById(mSelectableDevices, TEST_DEVICE_1_ID))
                .thenReturn(mDevice1);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mSelectableDevices);
        when(sMediaDeviceUpdateWorker.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_RANGE_VALUE, TEST_VOLUME);
        intent.putExtra(MEDIA_DEVICE_ID, GROUP_DEVICES);
        intent.putExtra(CUSTOMIZED_ACTION, ACTION_VOLUME_ADJUSTMENT);

        mMediaOutputGroupSlice.onNotifyChange(intent);

        verify(sMediaDeviceUpdateWorker).adjustSessionVolume(TEST_VOLUME);
    }

    @Test
    public void onNotifyChange_sendSelectableDevice_verifyAddSession() {
        mSelectableDevices.add(mDevice2);
        mSelectedDevices.add(mDevice1);
        when(mLocalMediaManager.getMediaDeviceById(mSelectableDevices, TEST_DEVICE_2_ID))
                .thenReturn(mDevice2);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mSelectableDevices);
        when(sMediaDeviceUpdateWorker.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        final Intent intent = new Intent();
        intent.putExtra(MEDIA_DEVICE_ID, TEST_DEVICE_2_ID);
        intent.putExtra(CUSTOMIZED_ACTION, ACTION_MEDIA_SESSION_OPERATION);

        mMediaOutputGroupSlice.onNotifyChange(intent);

        verify(sMediaDeviceUpdateWorker).addDeviceToPlayMedia(mDevice2);
    }

    @Test
    public void onNotifyChange_sendDeselectableDevice_verifyRemoveSession() {
        mSelectedDevices.add(mDevice1);
        mSelectedDevices.add(mDevice2);
        mDeselectableDevices.add(mDevice1);
        mDeselectableDevices.add(mDevice2);
        when(mLocalMediaManager.getMediaDeviceById(mSelectedDevices, TEST_DEVICE_2_ID))
                .thenReturn(mDevice2);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mSelectedDevices);
        when(sMediaDeviceUpdateWorker.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(sMediaDeviceUpdateWorker.getDeselectableMediaDevice()).thenReturn(
                mDeselectableDevices);
        final Intent intent = new Intent();
        intent.putExtra(MEDIA_DEVICE_ID, TEST_DEVICE_2_ID);
        intent.putExtra(CUSTOMIZED_ACTION, ACTION_MEDIA_SESSION_OPERATION);

        mMediaOutputGroupSlice.onNotifyChange(intent);

        verify(sMediaDeviceUpdateWorker).removeDeviceFromPlayMedia(mDevice2);
    }

    @Test
    public void onNotifyChange_sendNonDeselectableDevice_notRemoveSession() {
        mSelectedDevices.add(mDevice1);
        mSelectedDevices.add(mDevice2);
        mDeselectableDevices.add(mDevice1);
        when(mLocalMediaManager.getMediaDeviceById(mSelectedDevices, TEST_DEVICE_2_ID))
                .thenReturn(mDevice2);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mSelectedDevices);
        when(sMediaDeviceUpdateWorker.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(sMediaDeviceUpdateWorker.getDeselectableMediaDevice()).thenReturn(
                mDeselectableDevices);
        final Intent intent = new Intent();
        intent.putExtra(MEDIA_DEVICE_ID, TEST_DEVICE_2_ID);
        intent.putExtra(CUSTOMIZED_ACTION, ACTION_MEDIA_SESSION_OPERATION);

        mMediaOutputGroupSlice.onNotifyChange(intent);

        verify(sMediaDeviceUpdateWorker, never()).removeDeviceFromPlayMedia(mDevice2);
    }

    @Test
    public void onNotifyChange_noId_doNothing() {
        mSelectableDevices.add(mDevice1);
        mSelectedDevices.add(mDevice1);
        when(mLocalMediaManager.getMediaDeviceById(mSelectableDevices, TEST_DEVICE_1_ID))
                .thenReturn(mDevice1);
        sMediaDeviceUpdateWorker.onDeviceListUpdate(mSelectableDevices);
        when(sMediaDeviceUpdateWorker.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_RANGE_VALUE, TEST_VOLUME);
        intent.putExtra(CUSTOMIZED_ACTION, ACTION_VOLUME_ADJUSTMENT);

        mMediaOutputGroupSlice.onNotifyChange(intent);

        verify(sMediaDeviceUpdateWorker, never()).adjustSessionVolume(anyInt());
        verify(mDevice1, never()).requestSetVolume(TEST_VOLUME);
    }

    @Implements(SliceBackgroundWorker.class)
    public static class ShadowSliceBackgroundWorker {

        @Implementation
        public static SliceBackgroundWorker getInstance(Uri uri) {
            return sMediaDeviceUpdateWorker;
        }
    }
}
