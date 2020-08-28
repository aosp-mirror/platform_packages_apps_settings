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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AccessibilityTimeoutPreferenceControllerTest {

    private Context mContext;
    private AccessibilityTimeoutPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new AccessibilityTimeoutPreferenceController(mContext, "control_timeout");
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_byDefault_shouldReturnDefaultSummary() {
        final String[] timeoutSummarys = mContext.getResources().getStringArray(
                R.array.accessibility_timeout_summaries);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, "0");

        assertThat(mController.getSummary()).isEqualTo(timeoutSummarys[0]);
    }

    @Test
    public void getSummary_invalidTimeout_shouldReturnDefaultSummary() {
        final String[] timeoutSummarys = mContext.getResources().getStringArray(
                R.array.accessibility_timeout_summaries);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, "invalid_timeout");

        assertThat(mController.getSummary()).isEqualTo(timeoutSummarys[0]);
    }

    @Test
    public void getSummary_validTimeout_shouldReturnValidSummary() {
        final String[] timeoutSummarys = mContext.getResources().getStringArray(
                R.array.accessibility_timeout_summaries);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, "60000");

        assertThat(mController.getSummary()).isEqualTo(timeoutSummarys[3]);
    }
}
