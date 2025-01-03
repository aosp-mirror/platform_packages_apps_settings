/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static com.android.settings.accessibility.MagnificationFeedbackPreferenceController.FEEDBACK_KEY;
import static com.android.settings.accessibility.MagnificationFeedbackPreferenceController.PREF_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.core.util.Consumer;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MagnificationFeedbackPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class MagnificationFeedbackPreferenceControllerTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private PreferenceScreen mScreen;
    @Mock private PreferenceManager mPreferenceManager;
    @Mock private DashboardFragment mFragment;
    private SurveyFeatureProvider mSurveyFeatureProvider;
    private MagnificationFeedbackPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        FakeFeatureFactory.setupForTest();
        mSurveyFeatureProvider =
                FakeFeatureFactory.getFeatureFactory().getSurveyFeatureProvider(mContext);
        mController = new MagnificationFeedbackPreferenceController(mContext, mFragment, PREF_KEY);
        mPreference = new Preference(mContext);
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mPreferenceManager.findPreference(PREF_KEY)).thenReturn(mPreference);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
    }

    @Test
    public void getAvailabilityStatus_shouldAlwaysBeAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                MagnificationFeedbackPreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_surveyAvailable_preferenceEnabledWithSummary() {
        doAnswer(invocation -> {
            Consumer<Boolean> consumer = invocation.getArgument(2);
            consumer.accept(true);
            return null;
        }).when(mSurveyFeatureProvider).checkSurveyAvailable(any(), eq(FEEDBACK_KEY), any());

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.accessibility_feedback_summary));
    }

    @Test
    public void updateState_surveyUnavailable_preferenceDisabledWithSummary() {
        doAnswer(invocation -> {
            Consumer<Boolean> consumer = invocation.getArgument(2);
            consumer.accept(false);
            return null;
        }).when(mSurveyFeatureProvider).checkSurveyAvailable(any(), eq(FEEDBACK_KEY), any());

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.accessibility_feedback_disabled_summary));
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartSurvey() {
        mController.handlePreferenceTreeClick(mPreference);

        verify(mSurveyFeatureProvider).sendActivityIfAvailable(FEEDBACK_KEY);
    }
}
