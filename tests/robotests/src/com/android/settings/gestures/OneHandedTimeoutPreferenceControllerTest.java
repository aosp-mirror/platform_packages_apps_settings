/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.preference.ListPreference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OneHandedTimeoutPreferenceControllerTest {

    private static final String KEY = "one_handed_timeout_preference";

    private Context mContext;
    private OneHandedTimeoutPreferenceController mController;
    private ListPreference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new OneHandedTimeoutPreferenceController(mContext, KEY);
        mPreference = new ListPreference(mContext);
        mPreference.setKey(KEY);
    }

    @Test
    public void getAvailabilityStatus_enabledOneHanded_shouldAvailable() {
        OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext, true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_disableOneHanded_shouldUnavailable() {
        OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext, false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void updateState_enableOneHanded_switchShouldEnabled() {
        OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext, true);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_disableOneHanded_switchShouldDisabled() {
        OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext, false);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void getSummary_setTimeoutNever_shouldReturnNeverSummary() {
        final String[] timeoutTitles = mContext.getResources().getStringArray(
                R.array.one_handed_timeout_title);

        OneHandedSettingsUtils.setSettingsOneHandedModeTimeout(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.NEVER.getValue());

        assertThat(mController.getSummary()).isEqualTo(
                timeoutTitles[OneHandedSettingsUtils.OneHandedTimeout.NEVER.ordinal()]);
    }

    @Test
    public void getSummary_setTimeoutShort_shouldReturnShortSummary() {
        final String[] timeoutTitles = mContext.getResources().getStringArray(
                R.array.one_handed_timeout_title);

        OneHandedSettingsUtils.setSettingsOneHandedModeTimeout(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.SHORT.getValue());

        assertThat(mController.getSummary()).isEqualTo(String.format(
                mContext.getResources().getString(R.string.one_handed_timeout_summary),
                timeoutTitles[OneHandedSettingsUtils.OneHandedTimeout.SHORT.ordinal()]));
    }
}
