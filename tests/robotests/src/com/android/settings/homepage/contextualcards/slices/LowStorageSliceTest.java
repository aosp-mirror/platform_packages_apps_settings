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

package com.android.settings.homepage.contextualcards.slices;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.SliceTester;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class LowStorageSliceTest {

    private Context mContext;
    private LowStorageSlice mLowStorageSlice;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mLowStorageSlice = new LowStorageSlice(mContext);
    }

    @After
    public void tearDown() {
        ShadowPrivateStorageInfo.reset();
    }

    @Test
    @Config(shadows = ShadowPrivateStorageInfo.class)
    public void getSlice_hasLowStorage_shouldBeCorrectSliceContent() {
        ShadowPrivateStorageInfo.setPrivateStorageInfo(new PrivateStorageInfo(10L, 100L));

        final Slice slice = mLowStorageSlice.getSlice();

        final List<SliceItem> sliceItems = slice.getItems();
        SliceTester.assertTitle(sliceItems, mContext.getString(R.string.storage_menu_free));
    }

    @Test
    @Config(shadows = ShadowPrivateStorageInfo.class)
    public void getSlice_hasNoLowStorage_shouldBeNull() {
        ShadowPrivateStorageInfo.setPrivateStorageInfo(new PrivateStorageInfo(100L, 100L));

        final Slice slice = mLowStorageSlice.getSlice();

        assertThat(slice).isNull();
    }

    @Implements(PrivateStorageInfo.class)
    public static class ShadowPrivateStorageInfo {

        private static PrivateStorageInfo sPrivateStorageInfo = null;

        @Resetter
        public static void reset() {
            sPrivateStorageInfo = null;
        }

        @Implementation
        public static PrivateStorageInfo getPrivateStorageInfo(
                StorageVolumeProvider storageVolumeProvider) {
            return sPrivateStorageInfo;
        }

        public static void setPrivateStorageInfo(
                PrivateStorageInfo privateStorageInfo) {
            sPrivateStorageInfo = privateStorageInfo;
        }
    }
}