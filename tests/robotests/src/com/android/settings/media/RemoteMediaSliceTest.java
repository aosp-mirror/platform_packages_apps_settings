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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRouter2Manager;
import android.media.RoutingSessionInfo;
import android.net.Uri;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.media.LocalMediaManager;

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
    private static final String TEST_SESSION_1_ID = "test_session_1_id";
    private static final String TEST_SESSION_1_NAME = "test_session_1_name";
    private static final int TEST_VOLUME = 3;

    private static MediaDeviceUpdateWorker sMediaDeviceUpdateWorker;

    @Mock
    private LocalMediaManager mLocalMediaManager;

    private final List<RoutingSessionInfo> mRoutingSessionInfos = new ArrayList<>();

    private Context mContext;
    private RemoteMediaSlice mRemoteMediaSlice;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mRemoteMediaSlice = new RemoteMediaSlice(mContext);
        mRemoteMediaSlice.mRouterManager = mock(MediaRouter2Manager.class);
        sMediaDeviceUpdateWorker = spy(new MediaDeviceUpdateWorker(mContext,
                REMOTE_MEDIA_SLICE_URI));
        sMediaDeviceUpdateWorker.mLocalMediaManager = mLocalMediaManager;
        final RoutingSessionInfo remoteSessionInfo = mock(RoutingSessionInfo.class);
        when(remoteSessionInfo.getId()).thenReturn(TEST_SESSION_1_ID);
        when(remoteSessionInfo.getName()).thenReturn(TEST_SESSION_1_NAME);
        when(remoteSessionInfo.getVolumeMax()).thenReturn(100);
        when(remoteSessionInfo.getVolume()).thenReturn(10);
        when(remoteSessionInfo.isSystemSession()).thenReturn(false);
        mRoutingSessionInfos.add(remoteSessionInfo);
        when(sMediaDeviceUpdateWorker.getActiveRemoteMediaDevice()).thenReturn(
                mRoutingSessionInfos);
    }

    @Test
    public void onNotifyChange_noId_doNothing() {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_RANGE_VALUE, TEST_VOLUME);

        mRemoteMediaSlice.onNotifyChange(intent);

        verify(sMediaDeviceUpdateWorker, never())
                .adjustSessionVolume(TEST_SESSION_1_ID, TEST_VOLUME);
    }

    @Test
    public void onNotifyChange_verifyAdjustVolume() {
        final Intent intent = new Intent();
        intent.putExtra(MEDIA_ID, TEST_SESSION_1_ID);
        intent.putExtra(EXTRA_RANGE_VALUE, TEST_VOLUME);

        mRemoteMediaSlice.onNotifyChange(intent);

        verify(sMediaDeviceUpdateWorker).adjustSessionVolume(TEST_SESSION_1_ID, TEST_VOLUME);
    }

    @Test
    public void getSlice_noActiveSession_checkRowNumber() {
        mRoutingSessionInfos.clear();
        final Slice slice = mRemoteMediaSlice.getSlice();
        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM, null).size();

        assertThat(rows).isEqualTo(0);
    }

    @Test
    public void getSlice_withActiveSession_checkRowNumber() {
        final Slice slice = mRemoteMediaSlice.getSlice();
        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM, null).size();

        // InputRange and Row
        assertThat(rows).isEqualTo(2);
    }

    @Test
    public void getSlice_withActiveSession_checkTitle() {
        final Slice slice = mRemoteMediaSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();

        assertThat(primaryAction.getTitle().toString()).isEqualTo(mContext.getText(
                com.android.settings.R.string.remote_media_volume_option_title));
    }

    @Implements(SliceBackgroundWorker.class)
    public static class ShadowSliceBackgroundWorker {

        @Implementation
        public static SliceBackgroundWorker getInstance(Uri uri) {
            return sMediaDeviceUpdateWorker;
        }
    }
}
