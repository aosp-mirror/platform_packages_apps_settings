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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DaltonizerPreferenceControllerTest {
    private static final String PREF_KEY = "daltonizer_preference";
    private static final int ON = 1;
    private static final int OFF = 0;

    private Context mContext;
    private DaltonizerPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new DaltonizerPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void getSummary_enabledColorCorrection_shouldReturnOnSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, ON);

        assertThat(mController.getSummary().toString().contains(
                mContext.getText(R.string.accessibility_feature_state_on))).isTrue();
    }

    @Test
    public void getSummary_disabledColorCorrection_shouldReturnOffSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, OFF);

        assertThat(mController.getSummary().toString().contains(
                mContext.getText(R.string.accessibility_feature_state_off))).isTrue();
    }
}
