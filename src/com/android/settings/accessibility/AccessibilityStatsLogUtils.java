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

import static com.android.settings.accessibility.TextReadingPreferenceFragment.BOLD_TEXT_KEY;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.DISPLAY_SIZE_KEY;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint.ACCESSIBILITY_SETTINGS;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint.DISPLAY_SETTINGS;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint.SUW_ANYTHING_ELSE;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint.SUW_VISION_SETTINGS;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.FONT_SIZE_KEY;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.HIGH_TEXT_CONTRAST_KEY;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.RESET_KEY;

import android.content.ComponentName;

import com.android.internal.util.FrameworkStatsLog;
import com.android.settings.core.instrumentation.SettingsStatsLog;

/** Methods for logging accessibility states. */
public final class AccessibilityStatsLogUtils {

    private AccessibilityStatsLogUtils() {}

    /**
     * Logs accessibility service name and its enabled status. Calls this when the user trigger
     * the accessibility service to be enabled/disabled.
     *
     * @param componentName component name of the service
     * @param enabled       {@code true} if the service is enabled
     */
    static void logAccessibilityServiceEnabled(ComponentName componentName, boolean enabled) {
        SettingsStatsLog.write(SettingsStatsLog.ACCESSIBILITY_SERVICE_REPORTED,
                componentName.flattenToString(), convertToLoggingServiceEnabled(enabled));
    }

    private static int convertToLoggingServiceEnabled(boolean enabled) {
        return enabled ? SettingsStatsLog.ACCESSIBILITY_SERVICE_REPORTED__SERVICE_STATUS__ENABLED
                : SettingsStatsLog.ACCESSIBILITY_SERVICE_REPORTED__SERVICE_STATUS__DISABLED;
    }

    /**
     * Logs when the non-a11y category service is disabled. Calls this when the user disables the
     * non-a11y category service for the first time.
     *
     * @param packageName package name of the service
     * @param durationMills    duration in milliseconds between starting the page and disabling the
     *                    service
     */
    static void logDisableNonA11yCategoryService(String packageName, long durationMills) {
        com.android.internal.accessibility.util.AccessibilityStatsLogUtils
                .logNonA11yToolServiceWarningReported(
                        packageName,
                        com.android.internal.accessibility.util.AccessibilityStatsLogUtils
                                .ACCESSIBILITY_PRIVACY_WARNING_STATUS_SERVICE_DISABLED,
                        durationMills);
    }

    /**
     * Converts to the key name for logging.
     *
     * @param prefKey the preference key
     * @return the int value which maps to the key name
     */
    static int convertToItemKeyName(String prefKey) {
        switch (prefKey) {
            case FONT_SIZE_KEY:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_FONT_SIZE;
            case DISPLAY_SIZE_KEY:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_DISPLAY_SIZE;
            case BOLD_TEXT_KEY:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_BOLD_TEXT;
            case HIGH_TEXT_CONTRAST_KEY:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_HIGH_CONTRAST_TEXT;
            case RESET_KEY:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_RESET;
            default:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_UNKNOWN_ITEM;
        }
    }

    /**
     * Converts to the entry point for logging.
     *
     * @param entryPoint the entry point
     * @return the int value which maps to the entry point
     */
    static int convertToEntryPoint(int entryPoint) {
        switch (entryPoint) {
            case SUW_VISION_SETTINGS:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__ENTRY_POINT__TEXT_READING_SUW_VISION_SETTINGS;
            case SUW_ANYTHING_ELSE:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__ENTRY_POINT__TEXT_READING_SUW_ANYTHING_ELSE;
            case DISPLAY_SETTINGS:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__ENTRY_POINT__TEXT_READING_DISPLAY_SETTINGS;
            case ACCESSIBILITY_SETTINGS:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__ENTRY_POINT__TEXT_READING_ACCESSIBILITY_SETTINGS;
            default:
                return SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__ENTRY_POINT__TEXT_READING_UNKNOWN_ENTRY;
        }
    }

    /**
     * Converts the entering page id where the hearing aid binding process starts for logging.
     *
     * @param pageId the entry page id where the hearing aid binding process starts
     * @return the int value for logging mapped from some page ids defined in
     * {@link SettingsStatsLog}
     */
    public static int convertToHearingAidInfoBondEntry(int pageId) {
        switch (pageId) {
            case SettingsStatsLog.SETTINGS_UICHANGED__PAGE_ID__SETTINGS_CONNECTED_DEVICE_CATEGORY:
                return FrameworkStatsLog.HEARING_AID_INFO_REPORTED__BOND_ENTRY__CONNECTED_DEVICES;
            case SettingsStatsLog.SETTINGS_UICHANGED__PAGE_ID__DIALOG_ACCESSIBILITY_HEARINGAID:
                return FrameworkStatsLog.HEARING_AID_INFO_REPORTED__BOND_ENTRY__ACCESSIBILITY_HEARING_AIDS;
            case SettingsStatsLog.SETTINGS_UICHANGED__PAGE_ID__DIALOG_ACCESSIBILITY_HEARING_AID_PAIR_ANOTHER:
                return FrameworkStatsLog.HEARING_AID_INFO_REPORTED__BOND_ENTRY__ACCESSIBILITY_HEARING_AID_PAIR_ANOTHER;
            case SettingsStatsLog.SETTINGS_UICHANGED__PAGE_ID__BLUETOOTH_FRAGMENT:
                return FrameworkStatsLog.HEARING_AID_INFO_REPORTED__BOND_ENTRY__BLUETOOTH;
            default:
                return FrameworkStatsLog.HEARING_AID_INFO_REPORTED__BOND_ENTRY__PAGE_UNKNOWN;
        }
    }
}
