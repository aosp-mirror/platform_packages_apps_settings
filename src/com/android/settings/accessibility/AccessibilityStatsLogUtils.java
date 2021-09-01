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

import android.content.ComponentName;

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
}
