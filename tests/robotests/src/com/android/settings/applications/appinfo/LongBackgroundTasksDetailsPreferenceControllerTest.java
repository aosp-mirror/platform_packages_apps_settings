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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LongBackgroundTasksDetailsPreferenceControllerTest {

    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private Preference mPreference;
    @Mock
    private ApplicationFeatureProvider mAppFeatureProvider;

    private Context mContext;
    private LongBackgroundTasksDetailsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = spy(new LongBackgroundTasksDetailsPreferenceController(mContext, "test_key",
                mAppFeatureProvider));
        mController.setPackageName("Package1");
        mController.setParentFragment(mFragment);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
        when(mAppFeatureProvider.isLongBackgroundTaskPermissionToggleSupported()).thenReturn(true);
    }

    @Test
    public void getAvailabilityStatus_notCandidate_shouldReturnUnavailable() {
        doReturn(false).when(mController).isCandidate();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_isCandidate_shouldReturnAvailable() {
        doReturn(true).when(mController).isCandidate();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnAlarmsAndRemindersDetails() {
        assertThat(mController.getDetailFragmentClass())
                .isEqualTo(LongBackgroundTasksDetails.class);
    }

    @Test
    public void updateState_shouldSetSummary() {
        final String summary = "test summary";
        doReturn(summary).when(mController).getPreferenceSummary();

        mController.updateState(mPreference);

        verify(mPreference).setSummary(summary);
    }

    @Test
    public void isCandidate_nullPackageInfo_shouldNotCrash() {
        mController.isCandidate();
        // no crash
    }
}
