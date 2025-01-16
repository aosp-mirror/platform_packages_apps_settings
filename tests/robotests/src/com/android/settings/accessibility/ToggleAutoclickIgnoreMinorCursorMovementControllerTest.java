/*
 * Copyright 2025 The Android Open Source Project
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

import static android.view.accessibility.AccessibilityManager.AUTOCLICK_IGNORE_MINOR_CURSOR_MOVEMENT_DEFAULT;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ToggleAutoclickIgnoreMinorCursorMovementController}. */
@RunWith(RobolectricTestRunner.class)
public class ToggleAutoclickIgnoreMinorCursorMovementControllerTest {

    private static final String PREFERENCE_KEY =
            "accessibility_control_autoclick_ignore_minor_cursor_movement";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ToggleAutoclickIgnoreMinorCursorMovementController mController;

    @Before
    public void setUp() {
        mController =
                new ToggleAutoclickIgnoreMinorCursorMovementController(mContext, PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getAvailabilityStatus_availableWhenFlagOn() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getAvailabilityStatus_conditionallyUnavailableWhenFlagOn() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isChecked_matchesSetting() {
        assertThat(mController.isChecked()).isEqualTo(readSetting() == ON);
    }

    @Test
    public void setChecked_true_updatesSetting() {
        mController.setChecked(true);
        assertThat(readSetting()).isEqualTo(ON);
    }

    @Test
    public void setChecked_false_updatesSetting() {
        mController.setChecked(false);
        assertThat(readSetting()).isEqualTo(OFF);
    }

    private int readSetting() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_IGNORE_MINOR_CURSOR_MOVEMENT,
                AUTOCLICK_IGNORE_MINOR_CURSOR_MOVEMENT_DEFAULT ? ON : OFF);
    }
}
