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
import static com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ToggleScreenMagnificationPreferenceFragmentTest {

    private static final String DUMMY_PACKAGE_NAME = "com.dummy.example";
    private static final String DUMMY_CLASS_NAME = DUMMY_PACKAGE_NAME + ".dummy_a11y_service";
    private static final ComponentName DUMMY_COMPONENT_NAME = new ComponentName(DUMMY_PACKAGE_NAME,
            DUMMY_CLASS_NAME);
    private static final String SOFTWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    private static final String HARDWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;
    private static final String TRIPLETAP_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED;
    private static final String MAGNIFICATION_CONTROLLER_NAME =
            "com.android.server.accessibility.MagnificationController";

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void hasValueInSettings_putValue_hasValue() {
        setMagnificationTripleTapEnabled(/* enabled= */ true);

        assertThat(ToggleScreenMagnificationPreferenceFragment.hasMagnificationValuesInSettings(
                mContext, UserShortcutType.TRIPLETAP)).isTrue();
    }

    @Test
    public void optInAllValuesToSettings_optInValue_haveMatchString() {
        int shortcutTypes = UserShortcutType.SOFTWARE | UserShortcutType.TRIPLETAP;

        ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(mContext,
                shortcutTypes);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                MAGNIFICATION_CONTROLLER_NAME);
        assertThat(getMagnificationTripleTapStatus()).isTrue();

    }

    @Test
    public void optInAllValuesToSettings_existOtherValue_optInValue_haveMatchString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());

        ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(mContext,
                UserShortcutType.SOFTWARE);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                DUMMY_COMPONENT_NAME.flattenToString() + ":" + MAGNIFICATION_CONTROLLER_NAME);

    }

    @Test
    public void optOutAllValuesToSettings_optOutValue_emptyString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, MAGNIFICATION_CONTROLLER_NAME);
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY, MAGNIFICATION_CONTROLLER_NAME);
        setMagnificationTripleTapEnabled(/* enabled= */ true);
        int shortcutTypes =
                UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE | UserShortcutType.TRIPLETAP;

        ToggleScreenMagnificationPreferenceFragment.optOutAllMagnificationValuesFromSettings(
                mContext, shortcutTypes);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEmpty();
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEmpty();
        assertThat(getMagnificationTripleTapStatus()).isFalse();
    }

    @Test
    public void optOutValueFromSettings_existOtherValue_optOutValue_haveMatchString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY,
                DUMMY_COMPONENT_NAME.flattenToString() + ":" + MAGNIFICATION_CONTROLLER_NAME);
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY,
                DUMMY_COMPONENT_NAME.flattenToString() + ":" + MAGNIFICATION_CONTROLLER_NAME);
        int shortcutTypes = UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE;

        ToggleScreenMagnificationPreferenceFragment.optOutAllMagnificationValuesFromSettings(
                mContext, shortcutTypes);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                DUMMY_COMPONENT_NAME.flattenToString());
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEqualTo(
                DUMMY_COMPONENT_NAME.flattenToString());
    }

    private void putStringIntoSettings(String key, String componentName) {
        Settings.Secure.putString(mContext.getContentResolver(), key, componentName);
    }

    private void setMagnificationTripleTapEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, enabled ? ON : OFF);
    }

    private String getStringFromSettings(String key) {
        return Settings.Secure.getString(mContext.getContentResolver(), key);
    }

    private boolean getMagnificationTripleTapStatus() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF) == ON;
    }
}
