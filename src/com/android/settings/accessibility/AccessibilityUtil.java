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

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.IntDef;

import com.android.settings.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Provides utility methods to accessibility settings only. */
final class AccessibilityUtil {

    private AccessibilityUtil(){}

    /**
     * Annotation for different accessibilityService fragment UI type.
     *
     * {@code LEGACY} for displaying appearance aligned with sdk version Q accessibility service
     * page, but only hardware shortcut allowed.
     * {@code INVISIBLE} for displaying appearance without switch bar.
     * {@code INTUITIVE} for displaying appearance with new design.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            AccessibilityServiceFragmentType.LEGACY,
            AccessibilityServiceFragmentType.INVISIBLE,
            AccessibilityServiceFragmentType.INTUITIVE,
    })

    public @interface AccessibilityServiceFragmentType {
        int LEGACY = 0;
        int INVISIBLE = 1;
        int INTUITIVE = 2;
    }

    /**
     * Annotation for different shortcut type UI type.
     *
     * {@code DEFAULT} for displaying default value.
     * {@code SOFTWARE} for displaying specifying the accessibility services or features which
     * choose accessibility button in the navigation bar as preferred shortcut.
     * {@code HARDWARE} for displaying specifying the accessibility services or features which
     * choose accessibility shortcut as preferred shortcut.
     * {@code TRIPLETAP} for displaying specifying magnification to be toggled via quickly
     * tapping screen 3 times as preferred shortcut.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            ShortcutType.DEFAULT,
            ShortcutType.SOFTWARE,
            ShortcutType.HARDWARE,
            ShortcutType.TRIPLETAP,
    })

    /** Denotes the shortcut type. */
    public @interface ShortcutType {
        int DEFAULT = 0;
        int SOFTWARE = 1; // 1 << 0
        int HARDWARE = 2; // 1 << 1
        int TRIPLETAP = 4; // 1 << 2
    }

    /** Denotes the accessibility enabled status */
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
        int OFF = 0;
        int ON = 1;
    }

    /**
     * Return On/Off string according to the setting which specifies the integer value 1 or 0. This
     * setting is defined in the secure system settings {@link android.provider.Settings.Secure}.
     */
    static CharSequence getSummary(Context context, String settingsSecureKey) {
        final boolean enabled = Settings.Secure.getInt(context.getContentResolver(),
                settingsSecureKey, State.OFF) == State.ON;
        final int resId = enabled ? R.string.accessibility_feature_state_on
                : R.string.accessibility_feature_state_off;
        return context.getResources().getText(resId);
    }

    /**
     * Capitalizes a string by capitalizing the first character and making the remaining characters
     * lower case.
     */
    public static String capitalize(String stringToCapitalize) {
        if (stringToCapitalize == null) {
            return null;
        }

        StringBuilder capitalizedString = new StringBuilder();
        if (stringToCapitalize.length() > 0) {
            capitalizedString.append(stringToCapitalize.substring(0, 1).toUpperCase());
            if (stringToCapitalize.length() > 1) {
                capitalizedString.append(stringToCapitalize.substring(1).toLowerCase());
            }
        }
        return capitalizedString.toString();
    }

    /** Determines if a gesture navigation bar is being used. */
    public static boolean isGestureNavigateEnabled(Context context) {
        return context.getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode)
                == NAV_BAR_MODE_GESTURAL;
    }

    /**
     * Gets the corresponding fragment type of a given accessibility service
     *
     * @param accessibilityServiceInfo The accessibilityService's info
     * @return int from {@link AccessibilityServiceFragmentType}
     */
    static @AccessibilityServiceFragmentType int getAccessibilityServiceFragmentType(
            AccessibilityServiceInfo accessibilityServiceInfo) {
        final int targetSdk = accessibilityServiceInfo.getResolveInfo()
                .serviceInfo.applicationInfo.targetSdkVersion;
        final boolean requestA11yButton = (accessibilityServiceInfo.flags
                & AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON) != 0;

        if (targetSdk <= Build.VERSION_CODES.Q) {
            return AccessibilityServiceFragmentType.LEGACY;
        }
        return requestA11yButton
                ? AccessibilityServiceFragmentType.INVISIBLE
                : AccessibilityServiceFragmentType.INTUITIVE;
    }
}
