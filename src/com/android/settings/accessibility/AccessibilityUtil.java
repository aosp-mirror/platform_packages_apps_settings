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

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.view.WindowInsets.Type.displayCutout;
import static android.view.WindowInsets.Type.systemBars;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.DEFAULT;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType;
import com.android.internal.accessibility.util.ShortcutUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.StringJoiner;

/** Provides utility methods to accessibility settings only. */
public final class AccessibilityUtil {

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
     * Denotes the quick setting tooltip type.
     *
     * {@code GUIDE_TO_EDIT} for QS tiles that need to be added by editing.
     * {@code GUIDE_TO_DIRECT_USE} for QS tiles that have been auto-added already.
     */
    public @interface QuickSettingsTooltipType {
        int GUIDE_TO_EDIT = 0;
        int GUIDE_TO_DIRECT_USE = 1;
    }

    /** Denotes the accessibility enabled status */
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
        int OFF = 0;
        int ON = 1;
    }

    /**
     * Returns On/Off string according to the setting which specifies the integer value 1 or 0. This
     * setting is defined in the secure system settings {@link android.provider.Settings.Secure}.
     */
    static CharSequence getSummary(
            Context context, String settingsSecureKey, @StringRes int enabledString,
            @StringRes int disabledString) {
        boolean enabled = Settings.Secure.getInt(context.getContentResolver(),
                settingsSecureKey, State.OFF) == State.ON;
        return context.getResources().getText(enabled ? enabledString : disabledString);
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

    /** Determines if a accessibility floating menu is being used. */
    public static boolean isFloatingMenuEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, /* def= */ -1)
                == ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
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
     * @param context       The current context.
     * @param shortcutTypes A combination of {@link UserShortcutType}.
     * @param componentName The component name that need to be opted in Settings.
     */
    static void optInAllValuesToSettings(Context context, int shortcutTypes,
            @NonNull ComponentName componentName) {
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            AccessibilityManager a11yManager = context.getSystemService(AccessibilityManager.class);
            if (a11yManager != null) {
                a11yManager.enableShortcutsForTargets(
                        /* enable= */ true,
                        shortcutTypes,
                        Set.of(componentName.flattenToString()),
                        UserHandle.myUserId()
                );
            }

            return;
        }

        if ((shortcutTypes & SOFTWARE) == SOFTWARE) {
            optInValueToSettings(context, SOFTWARE, componentName);
        }
        if (((shortcutTypes & HARDWARE) == HARDWARE)) {
            optInValueToSettings(context, HARDWARE, componentName);
        }
    }

    /**
     * Opts in component name into {@code shortcutType} colon-separated string in Settings.
     *
     * @param context       The current context.
     * @param shortcutType  The preferred shortcut type user selected.
     * @param componentName The component name that need to be opted in Settings.
     */
    @VisibleForTesting
    static void optInValueToSettings(Context context, @UserShortcutType int shortcutType,
            @NonNull ComponentName componentName) {
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            AccessibilityManager a11yManager = context.getSystemService(AccessibilityManager.class);
            if (a11yManager != null) {
                a11yManager.enableShortcutsForTargets(
                        /* enable= */ true,
                        shortcutType,
                        Set.of(componentName.flattenToString()),
                        UserHandle.myUserId()
                );
            }
            return;
        }

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
     * @param context       The current context.
     * @param shortcutTypes A combination of {@link UserShortcutType}.
     * @param componentName The component name that need to be opted out from Settings.
     */
    static void optOutAllValuesFromSettings(Context context, int shortcutTypes,
            @NonNull ComponentName componentName) {
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            AccessibilityManager a11yManager = context.getSystemService(AccessibilityManager.class);
            if (a11yManager != null) {
                a11yManager.enableShortcutsForTargets(
                        /* enable= */ false,
                        shortcutTypes,
                        Set.of(componentName.flattenToString()),
                        UserHandle.myUserId()
                );
            }
            return;
        }

        if ((shortcutTypes & SOFTWARE) == SOFTWARE) {
            optOutValueFromSettings(context, SOFTWARE, componentName);
        }
        if (((shortcutTypes & HARDWARE) == HARDWARE)) {
            optOutValueFromSettings(context, HARDWARE, componentName);
        }
    }

    /**
     * Opts out component name into {@code shortcutType} colon-separated string in Settings.
     *
     * @param context       The current context.
     * @param shortcutType  The preferred shortcut type user selected.
     * @param componentName The component name that need to be opted out from Settings.
     */
    @VisibleForTesting
    static void optOutValueFromSettings(Context context, @UserShortcutType int shortcutType,
            @NonNull ComponentName componentName) {
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            AccessibilityManager a11yManager = context.getSystemService(AccessibilityManager.class);
            if (a11yManager != null) {
                a11yManager.enableShortcutsForTargets(
                        /* enable= */ false,
                        shortcutType,
                        Set.of(componentName.flattenToString()),
                        UserHandle.myUserId()
                );
            }
            return;
        }

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
        if ((shortcutTypes & SOFTWARE) == SOFTWARE) {
            exist = hasValueInSettings(context, SOFTWARE, componentName);
        }
        if (((shortcutTypes & HARDWARE) == HARDWARE)) {
            exist |= hasValueInSettings(context, HARDWARE, componentName);
        }
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            if ((shortcutTypes & QUICK_SETTINGS)
                    == QUICK_SETTINGS) {
                exist |= hasValueInSettings(context, QUICK_SETTINGS,
                        componentName);
            }
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
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            return ShortcutUtils.getShortcutTargetsFromSettings(
                    context, shortcutType, UserHandle.myUserId()
            ).contains(componentName.flattenToString());
        }

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
        int shortcutTypes = DEFAULT;
        if (hasValuesInSettings(context, SOFTWARE, componentName)) {
            shortcutTypes |= SOFTWARE;
        }
        if (hasValuesInSettings(context, HARDWARE, componentName)) {
            shortcutTypes |= HARDWARE;
        }
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            if (hasValuesInSettings(context, QUICK_SETTINGS, componentName)) {
                shortcutTypes |= QUICK_SETTINGS;
            }
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
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            return ShortcutUtils.convertToKey(shortcutType);
        }

        switch (shortcutType) {
            case SOFTWARE:
                return Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
            case HARDWARE:
                return Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;
            case TRIPLETAP:
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

    /**
     * Gets the bounds of the display window excluding the insets of the system bar and display
     * cut out.
     *
     * @param context the current context.
     * @return the bounds of the display window.
     */
    public static Rect getDisplayBounds(Context context) {
        final WindowManager windowManager = context.getSystemService(WindowManager.class);
        final WindowMetrics metrics = windowManager.getCurrentWindowMetrics();

        final Rect displayBounds = metrics.getBounds();
        final Insets displayInsets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                systemBars() | displayCutout());
        displayBounds.inset(displayInsets);

        return displayBounds;
    }

    /**
     * Indicates if the accessibility service belongs to a system App.
     * @param info AccessibilityServiceInfo
     * @return {@code true} if the App is a system App.
     */
    public static boolean isSystemApp(@NonNull AccessibilityServiceInfo info) {
        return info.getResolveInfo().serviceInfo.applicationInfo.isSystemApp();
    }

    /**
     * Bypasses the timeout restriction if volume key shortcut assigned.
     *
     * @param context the current context.
     */
    public static void skipVolumeShortcutDialogTimeoutRestriction(Context context) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.SKIP_ACCESSIBILITY_SHORTCUT_DIALOG_TIMEOUT_RESTRICTION, /*
                    true */ 1);
    }
}
