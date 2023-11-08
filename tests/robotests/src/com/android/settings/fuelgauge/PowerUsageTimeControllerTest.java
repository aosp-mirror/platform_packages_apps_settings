/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class PowerUsageTimeControllerTest {
    private static final String SLOT_TIME = "12 am-2 am";
    private static final String KEY_SCREEN_ON_TIME_PREF = "battery_usage_screen_time";
    private static final String KEY_BACKGROUND_TIME_PREF = "battery_usage_background_time";
    private static final String TEST_ANOMALY_HINT_TEXT = "test_anomaly_hint_text";

    private Context mContext;
    private PowerUsageTimeController mPowerUsageTimeController;

    @Mock private PreferenceCategory mPowerUsageTimeCategory;
    @Mock private PowerUsageTimePreference mScreenTimePreference;
    @Mock private PowerUsageTimePreference mBackgroundTimePreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPowerUsageTimeController = new PowerUsageTimeController(mContext);
        mPowerUsageTimeController.mPowerUsageTimeCategory = mPowerUsageTimeCategory;
        mPowerUsageTimeController.mScreenTimePreference = mScreenTimePreference;
        mPowerUsageTimeController.mBackgroundTimePreference = mBackgroundTimePreference;
        doReturn(KEY_SCREEN_ON_TIME_PREF).when(mScreenTimePreference).getKey();
        doReturn(KEY_BACKGROUND_TIME_PREF).when(mBackgroundTimePreference).getKey();
    }

    @Test
    public void handleScreenTimeUpdated_noInfo_prefInvisible() {
        mPowerUsageTimeController.handleScreenTimeUpdated(
                /* slotTime= */ null,
                /* screenOnTimeInMs= */ 0,
                /* backgroundTimeInMs= */ 0,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verifyAllPreferencesVisible(false);
    }

    @Test
    public void handleScreenTimeUpdated_onlySlotTime_prefInvisible() {
        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                /* screenOnTimeInMs= */ 0,
                /* backgroundTimeInMs= */ 0,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verifyAllPreferencesVisible(false);
    }

    @Test
    public void handleScreenTimeUpdated_lackBackgroundTime_onlyScreenOnTime() {
        final long screenOnTimeAMinute = 60000;
        final long backgroundTimeZero = 0;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                screenOnTimeAMinute,
                backgroundTimeZero,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verifyOnePreferenceInvisible(mBackgroundTimePreference);
        verify(mScreenTimePreference).setTimeTitle("Screen time");
        verify(mScreenTimePreference).setTimeSummary("1 min");
        verify(mScreenTimePreference, never()).setAnomalyHint(anyString());
    }

    @Test
    public void handleScreenTimeUpdated_lackScreenOnTime_onlyBackgroundTime() {
        final long screenOnTimeZero = 0;
        final long backgroundTimeTwoMinutes = 120000;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                screenOnTimeZero,
                backgroundTimeTwoMinutes,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verifyOnePreferenceInvisible(mScreenTimePreference);
        verify(mBackgroundTimePreference).setTimeTitle("Background time");
        verify(mBackgroundTimePreference).setTimeSummary("2 min");
        verify(mBackgroundTimePreference, never()).setAnomalyHint(anyString());
    }

    @Test
    public void handleScreenTimeUpdated_categoryTitleWithSlotTime_expectedResult() {
        final long screenOnTimeAMinute = 60000;
        final long backgroundTimeTwoMinutes = 120000;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                screenOnTimeAMinute,
                backgroundTimeTwoMinutes,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verifyAllPreferencesVisible(true);
        verify(mScreenTimePreference).setTimeTitle("Screen time");
        verify(mScreenTimePreference).setTimeSummary("1 min");
        verify(mScreenTimePreference, never()).setAnomalyHint(anyString());
        verify(mBackgroundTimePreference).setTimeTitle("Background time");
        verify(mBackgroundTimePreference).setTimeSummary("2 min");
        verify(mBackgroundTimePreference, never()).setAnomalyHint(anyString());
        verify(mPowerUsageTimeCategory).setTitle("App usage for 12 am-2 am");
    }

    @Test
    public void handleScreenTimeUpdated_categoryTitleWithoutSlotTime_expectedResult() {
        final long backgroundTimeTwoMinutes = 120000;
        final long screenOnTimeAMinute = 60000;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                /* slotTime= */ null,
                screenOnTimeAMinute,
                backgroundTimeTwoMinutes,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verifyAllPreferencesVisible(true);
        verify(mPowerUsageTimeCategory).setTitle("App usage since last full charge");
    }

    @Test
    public void handleScreenTimeUpdated_BackgroundLessThanAMinWithSlotTime_expectedResult() {
        final long screenOnTimeAMinute = 60000;
        final long backgroundTimeLessThanAMinute = 59999;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                screenOnTimeAMinute,
                backgroundTimeLessThanAMinute,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verifyAllPreferencesVisible(true);
        verify(mScreenTimePreference).setTimeSummary("1 min");
        verify(mBackgroundTimePreference).setTimeSummary("Less than a min");
    }

    @Test
    public void handleScreenTimeUpdated_ScreenTimeLessThanAMin_expectedResult() {
        final long screenOnTimeLessThanAMinute = 59999;
        final long backgroundTimeTwoMinutes = 120000;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                screenOnTimeLessThanAMinute,
                backgroundTimeTwoMinutes,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verifyAllPreferencesVisible(true);
        verify(mScreenTimePreference).setTimeSummary("Less than a min");
        verify(mBackgroundTimePreference).setTimeSummary("2 min");
    }

    @Test
    public void handleScreenTimeUpdated_bothLessThanAMin_expectedResult() {
        final long screenOnTimeLessThanAMinute = 59999;
        final long backgroundTimeLessThanAMinute = 59999;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                screenOnTimeLessThanAMinute,
                backgroundTimeLessThanAMinute,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verifyAllPreferencesVisible(true);
        verify(mScreenTimePreference).setTimeSummary("Less than a min");
        verify(mBackgroundTimePreference).setTimeSummary("Less than a min");
    }

    @Test
    public void handleScreenTimeUpdated_anomalyOfScreenOnTime_expectedResult() {
        final long screenOnTimeAMinute = 60000;
        final long backgroundTimeTwoMinutes = 120000;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                screenOnTimeAMinute,
                backgroundTimeTwoMinutes,
                KEY_SCREEN_ON_TIME_PREF,
                TEST_ANOMALY_HINT_TEXT);

        verifyAllPreferencesVisible(true);
        verify(mScreenTimePreference).setAnomalyHint(TEST_ANOMALY_HINT_TEXT);
        verify(mBackgroundTimePreference, never()).setAnomalyHint(anyString());
    }

    @Test
    public void handleScreenTimeUpdated_anomalyOfBackgroundTime_expectedResult() {
        final long screenOnTimeAMinute = 60000;
        final long backgroundTimeTwoMinutes = 120000;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                screenOnTimeAMinute,
                backgroundTimeTwoMinutes,
                KEY_BACKGROUND_TIME_PREF,
                TEST_ANOMALY_HINT_TEXT);

        verifyAllPreferencesVisible(true);
        verify(mScreenTimePreference, never()).setAnomalyHint(anyString());
        verify(mBackgroundTimePreference).setAnomalyHint(TEST_ANOMALY_HINT_TEXT);
    }

    @Test
    public void handleScreenTimeUpdated_anomalyOfScreenOnTimeWithoutTimeInfo_expectedResult() {
        final long screenOnTimeZero = 0;
        final long backgroundTimeTwoMinutes = 120000;

        mPowerUsageTimeController.handleScreenTimeUpdated(
                SLOT_TIME,
                screenOnTimeZero,
                backgroundTimeTwoMinutes,
                KEY_SCREEN_ON_TIME_PREF,
                TEST_ANOMALY_HINT_TEXT);

        verifyAllPreferencesVisible(true);
        verify(mScreenTimePreference).setTimeSummary("Less than a min");
        verify(mScreenTimePreference).setAnomalyHint(TEST_ANOMALY_HINT_TEXT);
        verify(mBackgroundTimePreference, never()).setAnomalyHint(anyString());
    }

    private void verifySetPrefToVisible(Preference pref, boolean isVisible) {
        verify(pref, isVisible ? times(1) : never()).setVisible(true);
    }

    private void verifyAllPreferencesVisible(boolean isVisible) {
        verifySetPrefToVisible(mScreenTimePreference, isVisible);
        verifySetPrefToVisible(mBackgroundTimePreference, isVisible);
        verifySetPrefToVisible(mPowerUsageTimeCategory, isVisible);
    }

    private void verifyOnePreferenceInvisible(Preference pref) {
        verifySetPrefToVisible(mScreenTimePreference, mScreenTimePreference != pref);
        verifySetPrefToVisible(mBackgroundTimePreference, mBackgroundTimePreference != pref);
        verifySetPrefToVisible(mPowerUsageTimeCategory, mPowerUsageTimeCategory != pref);
    }
}
