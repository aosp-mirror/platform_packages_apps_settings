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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AccessibilityShortcutPreferenceControllerTest {

    private Context mContext;
    private SwitchPreference mPreference;
    private AccessibilityShortcutPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = new AccessibilityShortcutPreferenceController(mContext,
                "accessibility_shortcut_preference");
    }

    @Test
    public void isChecked_enabledShortcutOnLockScreen_shouldReturnTrue() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, ON, UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabledShortcutOnLockScreen_shouldReturnFalse() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, OFF,
                UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_setTrue_shouldEnableShortcutOnLockScreen() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, OFF,
                UserHandle.USER_CURRENT)).isEqualTo(ON);
    }

    @Test
    public void setChecked_setFalse_shouldDisableShortcutOnLockScreen() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, ON,
                UserHandle.USER_CURRENT)).isEqualTo(OFF);
    }
}
