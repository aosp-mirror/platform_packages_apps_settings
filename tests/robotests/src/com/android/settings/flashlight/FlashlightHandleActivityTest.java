/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.flashlight;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FlashlightHandleActivityTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void getRawDataToIndex_sliceNotSupported_returnEmptyData() {
        List<SearchIndexableRaw> data = FlashlightHandleActivity.SEARCH_INDEX_DATA_PROVIDER
                .getRawDataToIndex(mContext, true /* enabled */);

        assertThat(data).isEmpty();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getRawDataToIndex_sliceSupported_returnData() {
        List<SearchIndexableRaw> data = FlashlightHandleActivity.SEARCH_INDEX_DATA_PROVIDER
                .getRawDataToIndex(mContext, true /* enabled */);

        assertThat(data).isNotEmpty();
    }
}
