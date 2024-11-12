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

package com.android.settings.accessibility;

import static android.view.flags.Flags.FLAG_ENABLE_VECTOR_CURSOR_A11Y_SETTINGS;

import static com.android.settings.accessibility.LargePointerIconPreferenceController.OFF;
import static com.android.settings.accessibility.LargePointerIconPreferenceController.ON;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LargePointerIconPreferenceControllerTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final int UNKNOWN = -1;

    private Context mContext;
    private SwitchPreference mPreference;
    private LargePointerIconPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new SwitchPreference(mContext);
        mController = new LargePointerIconPreferenceController(mContext, "large_pointer");
    }

    @Test
    @RequiresFlagsDisabled(FLAG_ENABLE_VECTOR_CURSOR_A11Y_SETTINGS)
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_ENABLE_VECTOR_CURSOR_A11Y_SETTINGS)
    public void getAvailabilityStatus_shouldReturnConditionallyUnavailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isChecked_enabledLargePointer_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON, ON);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabledLargePointer_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON, OFF);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_enabled_shouldEnableLargePointer() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON, UNKNOWN)).isEqualTo(ON);
    }

    @Test
    public void setChecked_disabled_shouldDisableLargePointer() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON, UNKNOWN)).isEqualTo(OFF);
    }
}
