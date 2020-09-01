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
import android.os.Vibrator;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class VibrationPreferenceControllerTest {

    private static final String VIBRATION_ON = "On";
    private static final String VIBRATION_OFF = "Off";

    private Context mContext;
    private VibrationPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new VibrationPreferenceController(mContext, "vibration_pref");
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_disabledVibration_shouldReturnOffSummary() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.RING_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);
        final String expectedResult = mContext.getString(R.string.switch_off_text);

        assertThat(mController.getSummary()).isEqualTo(expectedResult);
    }

    @Test
    public void getSummary_enabledSomeVibration_shouldReturnVibrationOnSummary() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.RING_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_MEDIUM);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 1);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_INTENSITY, Vibrator.VIBRATION_INTENSITY_MEDIUM);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1);
        final String expectedResult = mContext.getString(R.string.accessibility_vibration_summary,
                VIBRATION_ON /* ring */,
                VIBRATION_OFF /* notification */,
                VIBRATION_ON /* touch */);

        assertThat(mController.getSummary()).isEqualTo(expectedResult);
    }
}
