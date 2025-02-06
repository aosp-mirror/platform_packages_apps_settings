/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowAccessibilityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.List;

@Config(shadows = {
        SettingsShadowResources.class,
        com.android.settings.testutils.shadow.ShadowAccessibilityManager.class
})
@RunWith(RobolectricTestRunner.class)
public class HardwareShortcutFromLockscreenPreferenceControllerTest {
    @Rule
    public SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext = ApplicationProvider.getApplicationContext();
    private SwitchPreference mPreference;
    private HardwareShortcutFromLockscreenPreferenceController mController;
    private ShadowAccessibilityManager mShadowAccessibilityManager;

    @Before
    public void setUp() {
        mShadowAccessibilityManager = Shadow.extract(
                mContext.getSystemService(AccessibilityManager.class));
        mPreference = new SwitchPreference(mContext);
        mController = new HardwareShortcutFromLockscreenPreferenceController(mContext,
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

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getAvailabilityStatus_settingEmpty_disabled() {
        mShadowAccessibilityManager.setAccessibilityShortcutTargets(HARDWARE, List.of());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getAvailabilityStatus_settingNotEmpty_available() {
        mShadowAccessibilityManager.setAccessibilityShortcutTargets(HARDWARE, List.of("Foo"));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
