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
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

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
     * {@code VOLUME_SHORTCUT_TOGGLE} for displaying basic accessibility service fragment but
     * only hardware shortcut allowed.
     * {@code INVISIBLE_TOGGLE} for displaying basic accessibility service fragment without
     * switch bar.
     * {@code TOGGLE} for displaying basic accessibility service fragment.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            AccessibilityServiceFragmentType.VOLUME_SHORTCUT_TOGGLE,
            AccessibilityServiceFragmentType.INVISIBLE_TOGGLE,
            AccessibilityServiceFragmentType.TOGGLE,
    })

    public @interface AccessibilityServiceFragmentType {
        int VOLUME_SHORTCUT_TOGGLE = 0;
        int INVISIBLE_TOGGLE = 1;
        int TOGGLE = 2;
    }

    // TODO(b/147021230): Will move common functions and variables to
    //  android/internal/accessibility folder
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

    /**
     * Annotation for different user shortcut type UI type.
     *
     * {@code EMPTY} for displaying default value.
     * {@code SOFTWARE} for displaying specifying the accessibility services or features which
     * choose accessibility button in the navigation bar as preferred shortcut.
     * {@code HARDWARE} for displaying specifying the accessibility services or features which
     * choose accessibility shortcut as preferred shortcut.
     * {@code TRIPLETAP} for displaying specifying magnification to be toggled via quickly
     * tapping screen 3 times as preferred shortcut.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            UserShortcutType.EMPTY,
            UserShortcutType.SOFTWARE,
            UserShortcutType.HARDWARE,
            UserShortcutType.TRIPLETAP,
    })

    /** Denotes the user shortcut type. */
    public @interface UserShortcutType {
        int EMPTY = 0;
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

    /** Determines if a touch explore is being used. */
    public static boolean isTouchExploreEnabled(Context context) {
        final AccessibilityManager am = context.getSystemService(AccessibilityManager.class);
        return am.isTouchExplorationEnabled();
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
            return AccessibilityServiceFragmentType.VOLUME_SHORTCUT_TOGGLE;
        }
        return requestA11yButton
                ? AccessibilityServiceFragmentType.INVISIBLE_TOGGLE
                : AccessibilityServiceFragmentType.TOGGLE;
    }

    /**
     * Opts in component name into multiple {@code shortcutTypes} colon-separated string in
     * Settings.
     *
     * @param context The current context.
     * @param shortcutTypes  A combination of {@link UserShortcutType}.
     * @param componentName The component name that need to be opted in Settings.
     */
    static void optInAllValuesToSettings(Context context, int shortcutTypes,
            @NonNull ComponentName componentName) {
        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            optInValueToSettings(context, UserShortcutType.SOFTWARE, componentName);
        }
        if (((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE)) {
            optInValueToSettings(context, UserShortcutType.HARDWARE, componentName);
        }
    }

    /**
     * Opts in component name into {@code shortcutType} colon-separated string in Settings.
     *
     * @param context The current context.
     * @param shortcutType The preferred shortcut type user selected.
     * @param componentName The component name that need to be opted in Settings.
     */
    @VisibleForTesting
    static void optInValueToSettings(Context context, @UserShortcutType int shortcutType,
            @NonNull ComponentName componentName) {
        final String targetKey = convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);

        if (hasValueInSettings(context, shortcutType, componentName)) {
            return;
        }

        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));
        if (!TextUtils.isEmpty(targetString)) {
            joiner.add(targetString);
        }
        joiner.add(componentName.flattenToString());

        Settings.Secure.putString(context.getContentResolver(), targetKey, joiner.toString());
    }

    /**
     * Opts out component name into multiple {@code shortcutTypes} colon-separated string in
     * Settings.
     *
     * @param context The current context.
     * @param shortcutTypes A combination of {@link UserShortcutType}.
     * @param componentName The component name that need to be opted out from Settings.
     */
    static void optOutAllValuesFromSettings(Context context, int shortcutTypes,
            @NonNull ComponentName componentName) {
        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            optOutValueFromSettings(context, UserShortcutType.SOFTWARE, componentName);
        }
        if (((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE)) {
            optOutValueFromSettings(context, UserShortcutType.HARDWARE, componentName);
        }
    }

    /**
     * Opts out component name into {@code shortcutType} colon-separated string in Settings.
     *
     * @param context The current context.
     * @param shortcutType The preferred shortcut type user selected.
     * @param componentName The component name that need to be opted out from Settings.
     */
    @VisibleForTesting
    static void optOutValueFromSettings(Context context, @UserShortcutType int shortcutType,
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
     * Returns if component name existed in one of {@code shortcutTypes} string in Settings.
     *
     * @param context The current context.
     * @param shortcutTypes A combination of {@link UserShortcutType}.
     * @param componentName The component name that need to be checked existed in Settings.
     * @return {@code true} if componentName existed in Settings.
     */
    static boolean hasValuesInSettings(Context context, int shortcutTypes,
            @NonNull ComponentName componentName) {
        boolean exist = false;
        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            exist = hasValueInSettings(context, UserShortcutType.SOFTWARE, componentName);
        }
        if (((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE)) {
            exist |= hasValueInSettings(context, UserShortcutType.HARDWARE, componentName);
        }
        return exist;
    }

    /**
     * Returns if component name existed in {@code shortcutType} string Settings.
     *
     * @param context The current context.
     * @param shortcutType The preferred shortcut type user selected.
     * @param componentName The component name that need to be checked existed in Settings.
     * @return {@code true} if componentName existed in Settings.
     */
    @VisibleForTesting
    static boolean hasValueInSettings(Context context, @UserShortcutType int shortcutType,
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
     * Gets the corresponding user shortcut type of a given accessibility service.
     *
     * @param context The current context.
     * @param componentName The component name that need to be checked existed in Settings.
     * @return The user shortcut type if component name existed in {@code UserShortcutType} string
     * Settings.
     */
    static int getUserShortcutTypesFromSettings(Context context,
            @NonNull ComponentName componentName) {
        int shortcutTypes = UserShortcutType.EMPTY;
        if (hasValuesInSettings(context, UserShortcutType.SOFTWARE, componentName)) {
            shortcutTypes |= UserShortcutType.SOFTWARE;
        }
        if (hasValuesInSettings(context, UserShortcutType.HARDWARE, componentName)) {
            shortcutTypes |= UserShortcutType.HARDWARE;
        }
        return shortcutTypes;
    }

    /**
     * Converts {@link UserShortcutType} to key in Settings.
     *
     * @param shortcutType The shortcut type.
     * @return Mapping key in Settings.
     */
    static String convertKeyFromSettings(@UserShortcutType int shortcutType) {
        switch (shortcutType) {
            case UserShortcutType.SOFTWARE:
                return Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
            case UserShortcutType.HARDWARE:
                return Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;
            case UserShortcutType.TRIPLETAP:
                return Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED;
            default:
                throw new IllegalArgumentException(
                        "Unsupported userShortcutType " + shortcutType);
        }
    }

    /**
     * Gets the width of the screen.
     *
     * @param context the current context.
     * @return the width of the screen in terms of pixels.
     */
    public static int getScreenWidthPixels(Context context) {
        final Resources resources = context.getResources();
        final int screenWidthDp = resources.getConfiguration().screenWidthDp;

        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, screenWidthDp,
                resources.getDisplayMetrics()));
    }

    /**
     * Gets the height of the screen.
     *
     * @param context the current context.
     * @return the height of the screen in terms of pixels.
     */
    public static int getScreenHeightPixels(Context context) {
        final Resources resources = context.getResources();
        final int screenHeightDp = resources.getConfiguration().screenHeightDp;

        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, screenHeightDp,
                resources.getDisplayMetrics()));
    }
}
