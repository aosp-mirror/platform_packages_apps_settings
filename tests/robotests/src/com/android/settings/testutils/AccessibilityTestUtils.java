/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.testutils;

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.testutils.shadow.SettingsShadowResources;

/**
 * Utility class for common methods used in the accessibility feature related tests
 */
public class AccessibilityTestUtils {

    public static void setSoftwareShortcutMode(
            Context context, boolean gestureNavEnabled, boolean floatingButtonEnabled) {
        int mode = floatingButtonEnabled ? ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU : -1;

        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, mode);

        if (gestureNavEnabled) {
            SettingsShadowResources.overrideResource(
                    com.android.internal.R.integer.config_navBarInteractionMode,
                    NAV_BAR_MODE_GESTURAL);
        } else {
            SettingsShadowResources.overrideResource(
                    com.android.internal.R.integer.config_navBarInteractionMode,
                    NAV_BAR_MODE_3BUTTON);
        }
    }
}
