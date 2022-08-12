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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.SettingsMainSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link SoftwareCursorTogglePreferenceController}. */
@RunWith(AndroidJUnit4.class)
public class SoftwareCursorTogglePreferenceControllerTest {

    private PreferenceScreen mScreen;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private SoftwareCursorTogglePreferenceController mController;
    private SettingsMainSwitchPreference mSwitchPreference;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mController = new SoftwareCursorTogglePreferenceController(mContext,
                "screen_software_cursor_preference_switch");
        mSwitchPreference = new SettingsMainSwitchPreference(mContext);
        mSwitchPreference.setKey(mController.getPreferenceKey());
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mScreen.addPreference(mSwitchPreference);
    }

    @After
    public void tearDown() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SOFTWARE_CURSOR_ENABLED, OFF);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void performClick_cursorEnabled_shouldSetCursorDisabled() {
        mController.setChecked(true);
        mController.displayPreference(mScreen);

        mSwitchPreference.performClick();

        assertThat(mSwitchPreference.isChecked()).isFalse();
        assertThat(isSoftwareCursorEnabled()).isFalse();
    }

    @Test
    public void performClick_cursorDisabled_shouldSetCursorEnabled() {
        mController.setChecked(false);
        mController.displayPreference(mScreen);

        mSwitchPreference.performClick();

        assertThat(mSwitchPreference.isChecked()).isTrue();
        assertThat(isSoftwareCursorEnabled()).isTrue();
    }

    @Test
    public void setChecked_switchChecked_shouldSetCursorEnabled() {
        mController.displayPreference(mScreen);

        mController.setChecked(/* isChecked= */ true);

        assertThat(isSoftwareCursorEnabled()).isTrue();
    }

    @Test
    public void setChecked_switchUnchecked_shouldSetCursorDisabled() {
        mController.displayPreference(mScreen);

        mController.setChecked(/* isChecked= */ false);

        assertThat(isSoftwareCursorEnabled()).isFalse();
    }

    @Test
    public void onSwitchChanged_switchChecked_shouldSetCursorEnabled() {
        mController.displayPreference(mScreen);

        mController.onSwitchChanged(/* switchView= */ null, /* isChecked= */ true);

        assertThat(isSoftwareCursorEnabled()).isTrue();
    }

    @Test
    public void onSwitchChanged_switchUnchecked_shouldSetCursorDisabled() {
        mController.displayPreference(mScreen);

        mController.onSwitchChanged(/* switchView= */ null, /* isChecked= */ false);


        assertThat(isSoftwareCursorEnabled()).isFalse();
    }

    private boolean isSoftwareCursorEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SOFTWARE_CURSOR_ENABLED, OFF) == ON;
    }
}
