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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.usage.UsageStats;
import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AllAppsInfoPreferenceControllerTest {

    @Mock
    private UserManager mUserManager;
    private AllAppsInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = spy(RuntimeEnvironment.application);
        final Preference preference = new Preference(context);
        doReturn(mUserManager).when(context).getSystemService(Context.USER_SERVICE);
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        mController = new AllAppsInfoPreferenceController(context, "test_key");
        mController.mPreference = preference;
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAVAILABLE() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void onReloadDataCompleted_recentAppsSet_hidePreference() {
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);

        mController.onReloadDataCompleted(stats);

        assertThat(mController.mPreference.isVisible()).isFalse();
    }

    @Test
    public void onReloadDataCompleted_noRecentAppSet_showPreference() {
        final List<UsageStats> stats = new ArrayList<>();

        mController.onReloadDataCompleted(stats);

        assertThat(mController.mPreference.isVisible()).isTrue();
    }
}
