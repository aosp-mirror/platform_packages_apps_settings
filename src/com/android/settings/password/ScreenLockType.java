/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.password;

import android.app.admin.DevicePolicyManager;

/**
 * List of screen lock type options that are available in ChooseLockGeneric. Provides the key and
 * the associated quality, and also some helper functions to translate between them.
 */
public enum ScreenLockType {

    NONE(
            DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED,
            "unlock_set_off"),
    SWIPE(
            DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED,
            "unlock_set_none"),
    PATTERN(
            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING,
            "unlock_set_pattern"),
    PIN(
            DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
            DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX,
            "unlock_set_pin"),
    PASSWORD(
            DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC,
            DevicePolicyManager.PASSWORD_QUALITY_COMPLEX,
            "unlock_set_password"),
    MANAGED(
            DevicePolicyManager.PASSWORD_QUALITY_MANAGED,
            "unlock_set_managed");

    /**
     * The default quality of the type of lock used. For example, in the case of PIN, the default
     * quality if PASSWORD_QUALITY_NUMERIC, while the highest quality is
     * PASSWORD_QUALITY_NUMERIC_COMPLEX.
     */
    public final int defaultQuality;

    /**
     * The highest quality for the given type of lock. For example, in the case of password, the
     * default quality is PASSWORD_QUALITY_ALPHABETIC, but the highest possible quality is
     * PASSWORD_QUALITY_COMPLEX.
     */
    public final int maxQuality;

    public final String preferenceKey;

    ScreenLockType(int quality, String preferenceKey) {
        this(quality, quality, preferenceKey);
    }

    ScreenLockType(int defaultQuality, int maxQuality, String preferenceKey) {
        this.defaultQuality = defaultQuality;
        this.maxQuality = maxQuality;
        this.preferenceKey = preferenceKey;
    }

    /**
     * Gets the screen lock type for the given quality. Note that this method assumes that a screen
     * lock is enabled, which means if the quality is
     * {@link DevicePolicyManager#PASSWORD_QUALITY_UNSPECIFIED}, the returned type will be
     * {@link #SWIPE} and not {@link #NONE}.
     */
    public static ScreenLockType fromQuality(int quality) {
        switch (quality) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                return ScreenLockType.PATTERN;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                return ScreenLockType.PIN;
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                return ScreenLockType.PASSWORD;
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                return ScreenLockType.MANAGED;
            case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
                return ScreenLockType.SWIPE;
        }
        return null;
    }

    public static ScreenLockType fromKey(String key) {
        for (ScreenLockType lock : ScreenLockType.values()) {
            if (lock.preferenceKey.equals(key)) {
                return lock;
            }
        }
        return null;
    }
}
