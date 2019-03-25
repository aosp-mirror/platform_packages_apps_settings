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

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaOutputSliceConstants;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
@Ignore("b/129292771")
public class MediaOutputIndicatorSliceTest {

    private static final String TEST_DEVICE_NAME = "test_device_name";
    private static final int TEST_DEVICE_1_ICON =
            com.android.internal.R.drawable.ic_bt_headphones_a2dp;

    @Mock
    private LocalMediaManager mLocalMediaManager;

    private final List<MediaDevice> mDevices = new ArrayList<>();

    private Context mContext;
    private MediaOutputIndicatorSlice mMediaOutputIndicatorSlice;
    private MediaOutputIndicatorWorker mMediaOutputIndicatorWorker;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mMediaOutputIndicatorSlice = new MediaOutputIndicatorSlice(mContext);
        mMediaOutputIndicatorWorker = spy(new MediaOutputIndicatorWorker(
                mContext, MEDIA_OUTPUT_INDICATOR_SLICE_URI));
        mMediaOutputIndicatorSlice.mWorker = mMediaOutputIndicatorWorker;
    }

    @Test
    public void getSlice_invisible_returnNull() {
        when(mMediaOutputIndicatorWorker.isVisible()).thenReturn(false);

        assertThat(mMediaOutputIndicatorSlice.getSlice()).isNull();
    }

    @Test
    public void getSlice_withActiveDevice_checkContent() {
        when(mMediaOutputIndicatorWorker.isVisible()).thenReturn(true);
        when(mMediaOutputIndicatorWorker.findActiveDeviceName()).thenReturn(TEST_DEVICE_NAME);
        final Slice mediaSlice = mMediaOutputIndicatorSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);
        // Verify slice title and subtitle
        assertThat(metadata.getTitle()).isEqualTo(mContext.getText(R.string.media_output_title));
        assertThat(metadata.getSubtitle()).isEqualTo(TEST_DEVICE_NAME);
    }
}
