/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class UsageAccessDetailsTest {

    private FakeFeatureFactory mFeatureFactory;
    private UsageAccessDetails mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment = new UsageAccessDetails();
        mFragment.onAttach(RuntimeEnvironment.application);
    }

    @Test
    public void logSpecialPermissionChange() {
        mFragment.logSpecialPermissionChange(true, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_USAGE_VIEW_ALLOW,
                mFragment.getMetricsCategory(),
                "app",
                0);

        mFragment.logSpecialPermissionChange(false, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_USAGE_VIEW_DENY,
                mFragment.getMetricsCategory(),
                "app",
                0);
    }

    @Test
    public void refreshUi_hasNoAppEntry_shouldReturnFalse() {
        mFragment.mState = mock(ApplicationsState.class);
        mFragment.setArguments(new Bundle());

        assertThat(mFragment.refreshUi()).isFalse();
        assertThat(mFragment.mAppEntry).isNull();
        assertThat(mFragment.mPackageInfo).isNull();
    }
}
