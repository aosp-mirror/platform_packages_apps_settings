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

package com.android.settings.homepage.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.Resources;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.SliceTester;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class StorageSliceTest {
    private static final String USED_BYTES_TEXT = "test used bytes";
    private static final String SUMMARY_TEXT = "test summary";

    private Context mContext;
    private StorageSlice mStorageSlice;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mStorageSlice = spy(new StorageSlice(mContext));
    }

    @Test
    public void getSlice_shouldBeCorrectSliceContent() {
        final PrivateStorageInfo info = new PrivateStorageInfo(100L, 600L);
        doReturn(info).when(mStorageSlice).getPrivateStorageInfo();
        doReturn(USED_BYTES_TEXT).when(mStorageSlice).getStorageUsedText(any());
        doReturn(SUMMARY_TEXT).when(mStorageSlice).getStorageSummaryText(any());
        final Slice slice = mStorageSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_homepage_storage);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedIcon.toString());

        final List<SliceItem> sliceItems = slice.getItems();
        SliceTester.assertTitle(sliceItems, mContext.getString(R.string.storage_label));
    }
}
