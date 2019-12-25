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
     * Return On/Off string according to the setting which specifies the integer value 1 or 0. This
     * setting is defined in the secure system settings {@link android.provider.Settings.Secure}.
     */
    static CharSequence getSummary(Context context, String settingsSecureKey) {
        final boolean enabled = Settings.Secure.getInt(context.getContentResolver(),
                settingsSecureKey, 0) == 1;
        final int resId = enabled ? R.string.accessibility_feature_state_on
                : R.string.accessibility_feature_state_off;
        return context.getResources().getText(resId);
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
