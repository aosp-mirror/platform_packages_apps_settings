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

package com.android.settings.applications;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import android.app.usage.UsageStats;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AllAppsInfoPreferenceControllerTest {

    private AllAppsInfoPreferenceController mController;

    @Before
    public void setUp() {
        final Context context = RuntimeEnvironment.application;
        mController = new AllAppsInfoPreferenceController(context, "test_key");
    }

    @Test
    public void getAvailabilityStatus_hasRecentApps_shouldReturnConditionallyUnavailable() {
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);
        mController.setRecentApps(stats);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noRecentApps_shouldReturnAvailable() {
        // No data
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
