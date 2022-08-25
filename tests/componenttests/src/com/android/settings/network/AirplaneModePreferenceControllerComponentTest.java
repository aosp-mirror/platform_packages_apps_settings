/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.testutils.UiUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AirplaneModePreferenceControllerComponentTest {
    // Airplane on off status
    private static final int ON = 1;
    private static final int OFF = 0;
    public final String TAG = this.getClass().getName();
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private boolean mOriginAirplaneModeIsOn;

    @Before
    public void setUp() {
        // Make sure origin airplane mode is OFF.
        mOriginAirplaneModeIsOn = is_airplane_mode_on();
        if (mOriginAirplaneModeIsOn) {
            Log.d(TAG, "Origin airplane mode is on, turn it off.");
            Settings.Global.putInt(mInstrumentation.getTargetContext().getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, OFF);
        }
    }

    /**
     * Tests on/off airplane mode repeatedly.
     * Previously, a bug describe that crash issue if user on off airplane mode repeatedly.
     * This case try to switch on & off airplane mode for 10 times to check crash issue.
     */
    @Test
    public void test_on_off_airplane_mode_multiple_times() {
        AirplaneModePreferenceController controller =
                new AirplaneModePreferenceController(mInstrumentation.getTargetContext(),
                        SettingsSlicesContract.KEY_AIRPLANE_MODE);

        for (int i = 0; i < 10; ++i) {
            Log.d(TAG, "Test #" + (i + 1));
            controller.setChecked(true);
            assertThat(UiUtils.waitUntilCondition(1000,
                    () -> is_airplane_mode_on())).isTrue();

            controller.setChecked(false);
            assertThat(UiUtils.waitUntilCondition(1000,
                    () -> !is_airplane_mode_on())).isTrue();
        }

    }

    private boolean is_airplane_mode_on() {
        return Settings.System.getInt(
                mInstrumentation.getTargetContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, OFF) != 0;
    }

    @After
    public void tearDown() {
        if (is_airplane_mode_on() != mOriginAirplaneModeIsOn) {
            Settings.Global.putInt(mInstrumentation.getTargetContext().getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, (
                            mOriginAirplaneModeIsOn ? ON : OFF));
        }
    }
}
