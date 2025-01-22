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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.GESTURE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TWOFINGER_DOUBLETAP;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Rect;
import android.icu.text.CaseMap;
import android.os.Build;
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
import com.android.settings.R;
import com.android.settings.utils.LocaleUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Provides utility methods to accessibility settings only. */
public final class AccessibilityUtil {
    // LINT.IfChange(shortcut_type_ui_order)
    static final int[] SHORTCUTS_ORDER_IN_UI = {
            QUICK_SETTINGS,
            SOFTWARE, // FAB displays before gesture. Navbar displays without gesture.
            GESTURE,
            HARDWARE,
            TWOFINGER_DOUBLETAP,
            TRIPLETAP
    };
    // LINT.ThenChange(/res/xml/accessibility_edit_shortcuts.xml:shortcut_type_ui_order)

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
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, -1)
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
     * Gets the corresponding user shortcut type of a given accessibility service.
     *
     * @param context The current context.
     * @param componentName The component name that need to be checked existed in Settings.
     * @return The user shortcut type if component name existed in {@code UserShortcutType} string
     * Settings.
     */
    static int getUserShortcutTypesFromSettings(Context context,
            @NonNull ComponentName componentName) {
        int shortcutTypes = UserShortcutType.DEFAULT;
        for (int shortcutType : AccessibilityUtil.SHORTCUTS_ORDER_IN_UI) {
            if (!android.provider.Flags.a11yStandaloneGestureEnabled()) {
                if ((shortcutType & GESTURE) == GESTURE) {
                    continue;
                }
            }
            if (ShortcutUtils.isShortcutContained(
                    context, shortcutType, componentName.flattenToString())) {
                shortcutTypes |= shortcutType;
            }
        }

        return shortcutTypes;
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

    /**
     * Assembles a localized string describing the provided shortcut types.
     */
    public static CharSequence getShortcutSummaryList(Context context, int shortcutTypes) {
        final List<CharSequence> list = new ArrayList<>();

        for (int shortcutType : AccessibilityUtil.SHORTCUTS_ORDER_IN_UI) {
            if (!android.provider.Flags.a11yStandaloneGestureEnabled()
                    && (shortcutType & GESTURE) == GESTURE) {
                continue;
            }
            if (!com.android.server.accessibility.Flags
                    .enableMagnificationMultipleFingerMultipleTapGesture()
                    && (shortcutType & TWOFINGER_DOUBLETAP) == TWOFINGER_DOUBLETAP) {
                continue;
            }

            if ((shortcutTypes & shortcutType) == shortcutType) {
                list.add(switch (shortcutType) {
                    case QUICK_SETTINGS -> context.getText(
                            R.string.accessibility_feature_shortcut_setting_summary_quick_settings);
                    case SOFTWARE -> getSoftwareShortcutSummary(context);
                    case GESTURE -> context.getText(
                            R.string.accessibility_shortcut_edit_summary_software_gesture);
                    case HARDWARE -> context.getText(
                            R.string.accessibility_shortcut_hardware_keyword);
                    case TWOFINGER_DOUBLETAP -> context.getString(
                            R.string.accessibility_shortcut_two_finger_double_tap_keyword, 2);
                    case TRIPLETAP -> context.getText(
                            R.string.accessibility_shortcut_triple_tap_keyword);
                    default -> "";
                });
            }
        }

        list.sort(CharSequence::compare);
        return CaseMap.toTitle().wholeString().noLowercase().apply(Locale.getDefault(), /* iter= */
                null, LocaleUtils.getConcatenatedString(list));
    }

    @VisibleForTesting
    static CharSequence getSoftwareShortcutSummary(Context context) {
        if (android.provider.Flags.a11yStandaloneGestureEnabled()) {
            return context.getText(R.string.accessibility_shortcut_edit_summary_software);
        }
        int resId;
        if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_summary_software;
        } else if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_summary_software_gesture;
        } else {
            resId = R.string.accessibility_shortcut_edit_summary_software;
        }
        return context.getText(resId);
    }
}
