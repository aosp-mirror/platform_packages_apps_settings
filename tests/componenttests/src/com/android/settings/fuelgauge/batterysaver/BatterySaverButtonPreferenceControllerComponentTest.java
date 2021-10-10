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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.Settings.BatterySaverSettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.testutils.AdbUtils;

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
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final PowerManager mManager =
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

    private BatterySaverButtonPreferenceController get_battery_saver_controller(Activity activity) {
        BatterySaverButtonPreferenceController controller =
                new BatterySaverButtonPreferenceController(
                        ApplicationProvider.getApplicationContext(), "battery_saver");
        Fragment f =
                ((FragmentActivity) activity).getSupportFragmentManager().getFragments().get(0);
        controller.displayPreference(((SettingsPreferenceFragment) f).getPreferenceScreen());
        return controller;
    }

    @Test
    public void test_check_battery_saver_button() throws Exception {
        ActivityScenario scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            BatterySaverButtonPreferenceController controller =
                    get_battery_saver_controller(activity);
            controller.setChecked(true);
            checkPowerSaverMode(true);

            controller.setChecked(false);
            checkPowerSaverMode(false);
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
