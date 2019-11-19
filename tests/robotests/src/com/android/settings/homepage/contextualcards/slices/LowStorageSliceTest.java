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

import static android.app.slice.Slice.HINT_ERROR;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowPrivateStorageInfo;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
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
    public void getSlice_lowStorage_shouldHaveStorageFreeTitle() {
        ShadowPrivateStorageInfo.setPrivateStorageInfo(new PrivateStorageInfo(10L, 100L));

        final Slice slice = mLowStorageSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.storage_menu_free));
    }

    @Test
    @Config(shadows = ShadowPrivateStorageInfo.class)
    public void getSlice_lowStorage_shouldNotHaveErrorHint() {
        ShadowPrivateStorageInfo.setPrivateStorageInfo(new PrivateStorageInfo(10L, 100L));

        final Slice slice = mLowStorageSlice.getSlice();

        assertThat(slice.hasHint(HINT_ERROR)).isFalse();
    }

    @Test
    @Config(shadows = ShadowPrivateStorageInfo.class)
    public void getSlice_storageFree_shouldHaveStorageSettingsTitle() {
        ShadowPrivateStorageInfo.setPrivateStorageInfo(new PrivateStorageInfo(100L, 100L));

        final Slice slice = mLowStorageSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.storage_settings));
    }

    @Test
    @Config(shadows = ShadowPrivateStorageInfo.class)
    public void getSlice_storageFree_shouldHaveErrorHint() {
        ShadowPrivateStorageInfo.setPrivateStorageInfo(new PrivateStorageInfo(100L, 100L));

        final Slice slice = mLowStorageSlice.getSlice();

        assertThat(slice.hasHint(HINT_ERROR)).isTrue();
    }
}