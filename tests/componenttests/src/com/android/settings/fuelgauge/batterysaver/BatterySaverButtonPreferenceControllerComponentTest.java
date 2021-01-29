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

package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.R;
import com.android.settings.Settings.BatterySaverSettingsActivity;
import com.android.settings.testutils.AdbUtils;
import com.android.settings.testutils.UiUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BatterySaverButtonPreferenceControllerComponentTest {
    private static final String TAG =
            BatterySaverButtonPreferenceControllerComponentTest.class.getSimpleName();
    private Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private PowerManager mManager =
            (PowerManager) mInstrumentation.getTargetContext().getSystemService(
                    Context.POWER_SERVICE);

    @Rule
    public ActivityScenarioRule<BatterySaverSettingsActivity> rule = new ActivityScenarioRule<>(
            new Intent(
                    Settings.ACTION_BATTERY_SAVER_SETTINGS).setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK));

    @Before
    public void setUp() throws Exception {
        mInstrumentation.getUiAutomation().executeShellCommand("dumpsys battery unplug");
        mInstrumentation.getUiAutomation().executeShellCommand("settings get global low_power 0");
    }

    @Test
    public void test_check_battery_saver_button() throws Exception {
        ActivityScenario scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final Button button = activity.findViewById(R.id.state_on_button);
            UiUtils.waitUntilCondition(3000, () -> button.isEnabled());
            button.callOnClick();
            checkPowerSaverMode(true);

            Button offButton = activity.findViewById(R.id.state_off_button);
            offButton.callOnClick();
            checkPowerSaverMode(false);
        });

        //Ideally, we should be able to also create BatteryTipPreferenceController and verify that
        //it is showing battery saver on. Unfortunately, that part of code is tightly coupled with
        //UI, and it's not possible to retrieve that string without reaching very deep into the
        //codes and become very tightly coupled with any future changes. That is not what component
        //tests should do, so either we'll need to do this through UI with another ActivityScenario,
        //or the code needs to be refactored to be less coupled with UI.
    }

    @Test
    public void test_battery_saver_button_changes_when_framework_setting_change() throws Exception {
        ActivityScenario scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            Button buttonOn = activity.findViewById(R.id.state_on_button);
            Button buttonOff = activity.findViewById(R.id.state_off_button);
            assertThat(buttonOn.isVisibleToUser()).isEqualTo(true);
            assertThat(buttonOff.isVisibleToUser()).isEqualTo(false);
        });

        mManager.setPowerSaveModeEnabled(true);
        scenario.recreate();
        scenario.onActivity(activity -> {
            Button buttonOn = activity.findViewById(R.id.state_on_button);
            Button buttonOff = activity.findViewById(R.id.state_off_button);
            assertThat(buttonOn.isVisibleToUser()).isEqualTo(false);
            assertThat(buttonOff.isVisibleToUser()).isEqualTo(true);
        });

        mManager.setPowerSaveModeEnabled(false);
        scenario.recreate();
        scenario.onActivity(activity -> {
            Button buttonOn = activity.findViewById(R.id.state_on_button);
            Button buttonOff = activity.findViewById(R.id.state_off_button);
            assertThat(buttonOn.isVisibleToUser()).isEqualTo(true);
            assertThat(buttonOff.isVisibleToUser()).isEqualTo(false);
        });
    }

    @After
    public void tearDown() {
        mInstrumentation.getUiAutomation().executeShellCommand("settings get global low_power 0");
        mInstrumentation.getUiAutomation().executeShellCommand("dumpsys battery reset");
    }

    private void checkPowerSaverMode(boolean enabled) {
        //Check through adb. Note that this needs to be done first, or a wait and poll needs to be
        //done to the manager.isPowerSaveMode(), because calling isPowerSaveMode immediately after
        //setting it does not return true. It takes a while for isPowerSaveMode() to return the
        //up-to-date value.
        try {
            assertThat(
                    AdbUtils.checkStringInAdbCommandOutput(TAG, "settings get global low_power",
                            null, enabled ? "1" : "0", 1000)).isTrue();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            assert_().fail();
        }

        //Check through manager
        assertThat(mManager.isPowerSaveMode() == enabled).isTrue();
    }
}
