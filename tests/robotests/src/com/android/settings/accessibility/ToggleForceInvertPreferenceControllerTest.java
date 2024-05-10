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

import static android.view.accessibility.Flags.FLAG_FORCE_INVERT_COLOR;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ToggleForceInvertPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class ToggleForceInvertPreferenceControllerTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ToggleForceInvertPreferenceController mController;

    @Before
    public void setUp() {
        mController = new ToggleForceInvertPreferenceController(
                mContext,
                ColorAndMotionFragment.TOGGLE_FORCE_INVERT
        );
    }

    @Test
    @RequiresFlagsDisabled(FLAG_FORCE_INVERT_COLOR)
    public void flagOff_getAvailabilityStatus_shouldReturnUnsupported() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_FORCE_INVERT_COLOR)
    public void flagOn_getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void settingOff_reflectsCorrectValue() {
        setEnabled(false);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void settingOn_reflectsCorrectValue() {
        setEnabled(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void onCheck_settingChanges() {
        setEnabled(false);

        mController.setChecked(true);
        assertThat(isEnabled()).isTrue();

        mController.setChecked(false);
        assertThat(isEnabled()).isFalse();
    }

    private boolean isEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, /* def=*/ -1) == ON;
    }

    private void setEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, enabled ? ON : OFF);
    }
}
