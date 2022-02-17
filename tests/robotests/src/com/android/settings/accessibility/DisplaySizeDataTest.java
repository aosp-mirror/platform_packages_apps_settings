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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link DisplaySizeData}.
 */
@RunWith(RobolectricTestRunner.class)
public class DisplaySizeDataTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private DisplaySizeData mDisplaySizeData;

    @Before
    public void setUp() {
        mDisplaySizeData = new DisplaySizeData(mContext);
    }

    @Ignore("Ignore it since a NPE is happened in ShadowWindowManagerGlobal. (Ref. b/214161063)")
    @Test
    public void commit_success() {
        final int progress = 4;

        mDisplaySizeData.commit(progress);
        final float density = mContext.getResources().getDisplayMetrics().density;

        assertThat(density).isEqualTo(mDisplaySizeData.getValues().get(progress));
    }
}
