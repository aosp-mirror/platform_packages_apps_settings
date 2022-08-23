/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Looper;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link SoftwareCursorTriggerHintsPreferenceController}. */
@RunWith(AndroidJUnit4.class)
public class SoftwareCursorTriggerHintsPreferenceControllerTest {

    private PreferenceScreen mScreen;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private SoftwareCursorTriggerHintsPreferenceController mController;
    private SwitchPreference mSwitchPreference;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mController = new SoftwareCursorTriggerHintsPreferenceController(mContext,
                "cursor_trigger_hints_enabled");
        mSwitchPreference = new SwitchPreference(mContext);
        mSwitchPreference.setKey(mController.getPreferenceKey());
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mScreen.addPreference(mSwitchPreference);
    }

    @After
    public void tearDown() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SOFTWARE_CURSOR_TRIGGER_HINTS_ENABLED, OFF);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void performClick_cursorEnabled_shouldSetSettingDisabled() {
        mController.setChecked(true);
        mController.displayPreference(mScreen);

        mSwitchPreference.performClick();

        assertThat(mSwitchPreference.isChecked()).isFalse();
        assertThat(areTriggerHintsEnabled()).isFalse();
    }

    @Test
    public void performClick_cursorDisabled_shouldSetSettingEnabled() {
        mController.setChecked(false);
        mController.displayPreference(mScreen);

        mSwitchPreference.performClick();

        assertThat(mSwitchPreference.isChecked()).isTrue();
        assertThat(areTriggerHintsEnabled()).isTrue();
    }

    @Test
    public void setChecked_switchChecked_shouldSetSettingEnabled() {
        mController.displayPreference(mScreen);

        mController.setChecked(/* isChecked= */ true);

        assertThat(areTriggerHintsEnabled()).isTrue();
    }

    @Test
    public void setChecked_switchUnchecked_shouldSetSettingDisabled() {
        mController.displayPreference(mScreen);

        mController.setChecked(/* isChecked= */ false);

        assertThat(areTriggerHintsEnabled()).isFalse();
    }

    private boolean areTriggerHintsEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SOFTWARE_CURSOR_TRIGGER_HINTS_ENABLED, OFF) == ON;
    }
}
