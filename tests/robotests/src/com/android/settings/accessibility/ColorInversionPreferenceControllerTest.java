/*
 * Copyright (C) 2018 The Android Open Source Project
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RunWith(RobolectricTestRunner.class)
public class ColorInversionPreferenceControllerTest {
    private static final String PREF_KEY = "toggle_inversion_preference";
    private static final String DISPLAY_INVERSION_ENABLED =
            Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;
    private Context mContext;
    private ColorInversionPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new ColorInversionPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void getSummary_enabledColorInversion_shouldReturnOnSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                DISPLAY_INVERSION_ENABLED, State.ON);

        assertThat(mController.getSummary().toString().contains(
                mContext.getText(R.string.accessibility_feature_state_on))).isTrue();
    }

    @Test
    public void getSummary_disabledColorInversion_shouldReturnOffSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                DISPLAY_INVERSION_ENABLED, State.OFF);

        assertThat(mController.getSummary().toString().contains(
                mContext.getText(R.string.accessibility_feature_state_off))).isTrue();
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
        int OFF = 0;
        int ON = 1;
    }
}
