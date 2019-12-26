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
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.android.settings.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.StringJoiner;

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

    // TODO(b/147021230): Will move common functions and variables to
    //  android/internal/accessibility folder
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

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
            PreferredShortcutType.DEFAULT,
            PreferredShortcutType.SOFTWARE,
            PreferredShortcutType.HARDWARE,
            PreferredShortcutType.TRIPLETAP,
    })

    /** Denotes the shortcut type. */
    public @interface PreferredShortcutType {
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
     * Gets the corresponding fragment type of a given accessibility service.
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

    /**
     * Opts in component name into colon-separated {@code shortcutType} key's string in Settings.
     *
     * @param context The current context.
     * @param shortcutType The preferred shortcut type user selected.
     * @param componentName The component name that need to be opted in Settings.
     */
    static void optInValueToSettings(Context context, @PreferredShortcutType int shortcutType,
            @NonNull ComponentName componentName) {
        final String targetKey = convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);

        if (TextUtils.isEmpty(targetString)) {
            return;
        }

        if (hasValueInSettings(context, shortcutType, componentName)) {
            return;
        }

        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));

        joiner.add(targetString);
        joiner.add(componentName.flattenToString());

        Settings.Secure.putString(context.getContentResolver(), targetKey, joiner.toString());
    }

    /**
     * Opts out component name into colon-separated {@code shortcutType} key's string in Settings.
     *
     * @param context The current context.
     * @param shortcutType The preferred shortcut type user selected.
     * @param componentName The component name that need to be opted out from Settings.
     */
    static void optOutValueFromSettings(Context context, @PreferredShortcutType int shortcutType,
            @NonNull ComponentName componentName) {
        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));
        final String targetKey = convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);

        if (TextUtils.isEmpty(targetString)) {
            return;
        }

        sStringColonSplitter.setString(targetString);
        while (sStringColonSplitter.hasNext()) {
            final String name = sStringColonSplitter.next();
            if (TextUtils.isEmpty(name) || (componentName.flattenToString()).equals(name)) {
                continue;
            }
            joiner.add(name);
        }

        Settings.Secure.putString(context.getContentResolver(), targetKey, joiner.toString());
    }

    /**
     * Returns if component name existed in Settings.
     *
     * @param context The current context.
     * @param shortcutType The preferred shortcut type user selected.
     * @param componentName The component name that need to be checked existed in Settings.
     * @return {@code true} if componentName existed in Settings.
     */
    static boolean hasValueInSettings(Context context, @PreferredShortcutType int shortcutType,
            @NonNull ComponentName componentName) {
        final String targetKey = convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);

        if (TextUtils.isEmpty(targetString)) {
            return false;
        }

        sStringColonSplitter.setString(targetString);

        while (sStringColonSplitter.hasNext()) {
            final String name = sStringColonSplitter.next();
            if ((componentName.flattenToString()).equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts {@link PreferredShortcutType} to key in Settings.
     *
     * @param shortcutType The shortcut type.
     * @return Mapping key in Settings.
     */
    static String convertKeyFromSettings(@PreferredShortcutType int shortcutType) {
        switch (shortcutType) {
            case PreferredShortcutType.SOFTWARE:
                return Settings.Secure.ACCESSIBILITY_BUTTON_TARGET_COMPONENT;
            case PreferredShortcutType.HARDWARE:
                return Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;
            case PreferredShortcutType.TRIPLETAP:
                return Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED;
            default:
                throw new IllegalArgumentException(
                        "Unsupported preferredShortcutType " + shortcutType);
        }
    }
}
