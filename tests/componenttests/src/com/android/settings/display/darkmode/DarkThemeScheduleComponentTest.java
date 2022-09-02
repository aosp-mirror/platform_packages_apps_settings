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

package com.android.settings.display.darkmode;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.app.TimePickerDialog;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.provider.Settings;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.android.settings.testutils.CommonUtils;
import com.android.settings.testutils.UiUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalTime;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DarkThemeScheduleComponentTest {
    private static final int DIALOG_START_TIME = 0;
    private static final int DIALOG_END_TIME = 1;
    /** The identifier for the positive button. */
    private static final int BUTTON_POSITIVE = -1;
    public final String TAG = this.getClass().getName();
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    @Rule
    public ActivityScenarioRule<com.android.settings.Settings.DarkThemeSettingsActivity> rule =
            new ActivityScenarioRule<>(
                    new Intent(
                            Settings.ACTION_DARK_THEME_SETTINGS).setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK));
    private UiModeManager mUiModeManager;

    @Before
    public void setUp() {
        mUiModeManager = mInstrumentation.getTargetContext().getSystemService(UiModeManager.class);
        if (mUiModeManager.getNightMode() != UiModeManager.MODE_NIGHT_NO) {
            mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
        }
    }

    private void test_step_for_custom_time(int startTimeDiff, int endTimeDiff) {

        ActivityScenario scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_CUSTOM);
            Fragment f =
                    ((FragmentActivity) activity).getSupportFragmentManager().getFragments().get(0);
            DarkModeSettingsFragment fragment = (DarkModeSettingsFragment) f;

            setCustomTime(fragment, DIALOG_START_TIME, LocalTime.now().plusMinutes(startTimeDiff));
            setCustomTime(fragment, DIALOG_END_TIME, LocalTime.now().plusMinutes(endTimeDiff));

            // The night mode need to reopen the screen to trigger UI change after mode change.
            CommonUtils.reopenScreen();
        });

        // Relaunch the scenario to make sure UI apply new mode.
        scenario.onActivity(activity -> {
            Log.d(TAG, "Activity Recreated!");
            UiUtils.waitForActivitiesInStage(2000, Stage.RESUMED);
        });
    }

    @Test
    public void test_dark_mode_in_custom_time() {
        test_step_for_custom_time(-1, 11);
        assertThat(checkNightMode(true)).isTrue();
    }

    @Test
    public void test_dark_mode_after_custom_time() {
        test_step_for_custom_time(-11, -1);
        assertThat(checkNightMode(false)).isTrue();
    }

    @Test
    public void test_dark_mode_before_custom_time() {
        test_step_for_custom_time(2, 20);
        assertThat(checkNightMode(false)).isTrue();
    }

    /**
     * Sets custom time for night mode.
     *
     * @param fragment The DarkModeSettingsFragment.
     * @param dialogId Dialog id for start time or end time.
     * @param time     The time to be set.
     */
    private void setCustomTime(DarkModeSettingsFragment fragment, int dialogId, LocalTime time) {
        Log.d(TAG, "Start to set custom time " + (dialogId == DIALOG_START_TIME ? "StartTime"
                : "EndTime") + " to " + time.getHour() + ":" + time.getMinute());
        TimePickerDialog startTimeDialog = (TimePickerDialog) fragment.onCreateDialog(dialogId);
        startTimeDialog.updateTime(time.getHour(), time.getMinute());
        startTimeDialog.onClick(startTimeDialog, BUTTON_POSITIVE);
    }

    private boolean checkNightMode(boolean isNightMode) {
        int mask = (isNightMode ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        int mode = mInstrumentation.getTargetContext().getResources().getConfiguration().uiMode;
        return (mode & mask) != 0;
    }

    @After
    public void tearDown() {
        Log.d(TAG, "tearDown.");
        if (mUiModeManager.getNightMode() != UiModeManager.MODE_NIGHT_NO) {
            mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
        }
    }
}
