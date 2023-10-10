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
package com.android.settings.core.instrumentation;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ElapsedTimeUtilsTest {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void storeSuwFinishedTimestamp_firstTimeAccess_shouldStoreTimestamp() {
        final long timestamp = 1000000L;

        ElapsedTimeUtils.storeSuwFinishedTimestamp(mContext, timestamp);

        final long result = ElapsedTimeUtils.getSuwFinishedTimestamp(mContext);
        assertThat(result).isEqualTo(timestamp);
    }

    @Test
    public void storeSuwFinishedTimestamp_NotFirstTimeAccess_shouldNotStoreTimestamp() {
        final long timestamp = 1000000L;

        ElapsedTimeUtils.storeSuwFinishedTimestamp(mContext, timestamp);
        ElapsedTimeUtils.storeSuwFinishedTimestamp(mContext, 2000000L);

        final long result = ElapsedTimeUtils.getSuwFinishedTimestamp(mContext);
        assertThat(result).isEqualTo(timestamp);
    }

    @Test
    public void getElapsedTime_valueInPrefs_positiveElapsedTime_returnElapsedTime() {
        final long suwTime = 1000L;
        final long actionTime = 2000L;
        ElapsedTimeUtils.storeSuwFinishedTimestamp(mContext, suwTime);

        final long result = ElapsedTimeUtils.getElapsedTime(actionTime);

        assertThat(result).isEqualTo(actionTime - suwTime);
    }

    @Test
    public void getElapsedTime_valueInPrefs_negativeElapsedTime_returnDefault() {
        final long suwTime = 2000L;
        final long actionTime = 1000L;
        ElapsedTimeUtils.storeSuwFinishedTimestamp(mContext, suwTime);

        final long result = ElapsedTimeUtils.getElapsedTime(actionTime);

        assertThat(result).isEqualTo(ElapsedTimeUtils.DEFAULT_SETUP_TIME);
    }

    @Test
    public void getElapsedTime_noValueInPrefs_returnDefault() {
        final long actionTime = 1000L;

        final long result = ElapsedTimeUtils.getElapsedTime(actionTime);

        assertThat(result).isEqualTo(ElapsedTimeUtils.DEFAULT_SETUP_TIME);
    }
}
