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

import static com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.util.ShortcutUtils;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

/** Tests for {@link PreferredShortcuts} */
@RunWith(AndroidJUnit4.class)
public class PreferredShortcutsTest {

    private static final String PACKAGE_NAME_1 = "com.test1.example";
    private static final String CLASS_NAME_1 = PACKAGE_NAME_1 + ".test1";
    private static final ComponentName COMPONENT_NAME_1 = new ComponentName(PACKAGE_NAME_1,
            CLASS_NAME_1);
    private static final String PACKAGE_NAME_2 = "com.test2.example";
    private static final String CLASS_NAME_2 = PACKAGE_NAME_2 + ".test2";
    private static final ComponentName COMPONENT_NAME_2 = new ComponentName(PACKAGE_NAME_2,
            CLASS_NAME_2);
    private static final ContentResolver sContentResolver =
            ApplicationProvider.getApplicationContext().getContentResolver();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        clearShortcuts();
    }

    @AfterClass
    public static void cleanUp() {
        clearShortcuts();
    }

    @Test
    public void retrieveUserShortcutType_fromSingleData_matchSavedType() {
        final int type = 1;
        final PreferredShortcut shortcut = new PreferredShortcut(COMPONENT_NAME_1.flattenToString(),
                type);

        PreferredShortcuts.saveUserShortcutType(mContext, shortcut);
        final int retrieveType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                COMPONENT_NAME_1.flattenToString());

        assertThat(retrieveType).isEqualTo(type);
    }

    @Test
    public void retrieveUserShortcutType_fromMultiData_matchSavedType() {
        final int type1 = 1;
        final int type2 = 2;
        final PreferredShortcut shortcut1 = new PreferredShortcut(
                COMPONENT_NAME_1.flattenToString(), type1);
        final PreferredShortcut shortcut2 = new PreferredShortcut(
                COMPONENT_NAME_2.flattenToString(), type2);

        PreferredShortcuts.saveUserShortcutType(mContext, shortcut1);
        PreferredShortcuts.saveUserShortcutType(mContext, shortcut2);
        final int retrieveType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                COMPONENT_NAME_1.flattenToString());

        assertThat(retrieveType).isEqualTo(type1);
    }

    @Test
    public void updatePreferredShortcutsFromSetting_magnificationWithTripleTapAndVolumeKeyShortcuts_preferredShortcutsMatches() {
        ShortcutUtils.optInValueToSettings(mContext, ShortcutConstants.UserShortcutType.HARDWARE,
                MAGNIFICATION_CONTROLLER_NAME);
        Settings.Secure.putInt(
                sContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                AccessibilityUtil.State.ON);

        PreferredShortcuts.updatePreferredShortcutsFromSettings(mContext,
                Set.of(MAGNIFICATION_CONTROLLER_NAME));
        int expectedShortcutTypes = ShortcutConstants.UserShortcutType.HARDWARE
                | ShortcutConstants.UserShortcutType.TRIPLETAP;

        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, MAGNIFICATION_CONTROLLER_NAME
                ))
                .isEqualTo(expectedShortcutTypes);
    }

    @Test
    public void updatePreferredShortcutsFromSetting_magnificationWithNoActiveShortcuts_noChangesOnPreferredShortcutTypes() {
        int expectedShortcutTypes = ShortcutConstants.UserShortcutType.HARDWARE
                | ShortcutConstants.UserShortcutType.SOFTWARE;
        PreferredShortcuts.saveUserShortcutType(mContext,
                new PreferredShortcut(MAGNIFICATION_CONTROLLER_NAME, expectedShortcutTypes));


        PreferredShortcuts.updatePreferredShortcutsFromSettings(mContext,
                Set.of(MAGNIFICATION_CONTROLLER_NAME));


        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, MAGNIFICATION_CONTROLLER_NAME
                ))
                .isEqualTo(expectedShortcutTypes);
    }

    @Test
    public void updatePreferredShortcutsFromSetting_multipleComponents_preferredShortcutsMatches() {
        String target1 = COLOR_INVERSION_COMPONENT_NAME.flattenToString();
        String target2 = DALTONIZER_COMPONENT_NAME.flattenToString();

        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS, target1);
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,
                target1 + ShortcutConstants.SERVICES_SEPARATOR + target2);

        int target1ShortcutTypes = ShortcutConstants.UserShortcutType.HARDWARE
                | ShortcutConstants.UserShortcutType.SOFTWARE;
        int target2ShortcutTypes = ShortcutConstants.UserShortcutType.HARDWARE;

        PreferredShortcuts.updatePreferredShortcutsFromSettings(mContext, Set.of(target1, target2));

        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, target1
                ))
                .isEqualTo(target1ShortcutTypes);
        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, target2
                ))
                .isEqualTo(target2ShortcutTypes);
    }

    private static void clearShortcuts() {
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS, "");
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE, "");
        Settings.Secure.putInt(
                sContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                AccessibilityUtil.State.OFF);
        Settings.Secure.putInt(
                sContentResolver,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED,
                AccessibilityUtil.State.OFF);
    }
}
