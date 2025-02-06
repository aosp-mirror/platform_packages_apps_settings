/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.android.internal.R;

/** Common code for double tap power settings shared between controllers. */
final class DoubleTapPowerSettingsUtils {

    /** Configuration value indicating double tap power button gesture is disabled. */
    static final int DOUBLE_TAP_POWER_DISABLED_MODE = 0;
    /** Configuration value indicating double tap power button gesture should launch camera. */
    static final int DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE = 1;
    /**
     * Configuration value indicating double tap power button gesture should launch one of many
     * target actions.
     */
    static final int DOUBLE_TAP_POWER_MULTI_TARGET_MODE = 2;

    /** Setting storing whether the double tap power button gesture is enabled. */
    private static final String DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED =
            Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED;

    static final Uri DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED_URI =
            Settings.Secure.getUriFor(DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED);

    /** Setting storing the target action of the double tap power button gesture. */
    private static final String DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION =
            Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE;

    static final Uri DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION_URI =
            Settings.Secure.getUriFor(DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION);

    private static final int DOUBLE_TAP_POWER_BUTTON_CAMERA_LAUNCH_VALUE = 0;
    private static final int DOUBLE_TAP_POWER_BUTTON_WALLET_LAUNCH_VALUE = 1;

    static final int ON = 1;
    static final int OFF = 0;

    /**
     * @return true if double tap power button gesture is available.
     */
    public static boolean isMultiTargetDoubleTapPowerButtonGestureAvailable(
            @NonNull Context context) {
        return context.getResources()
                .getInteger(
                        R.integer.config_doubleTapPowerGestureMode)
                == DOUBLE_TAP_POWER_MULTI_TARGET_MODE;
    }

    /**
     * Gets double tap power button gesture enable or disable flag from Settings provider.
     *
     * @param context App context
     * @return true if double tap on the power button gesture is currently enabled.
     */
    public static boolean isDoubleTapPowerButtonGestureEnabled(@NonNull Context context) {
        return Settings.Secure.getInt(
                context.getContentResolver(), DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED, ON)
                == ON;
    }

    /**
     * Sets double tap power button gesture enable or disable flag to Settings provider.
     *
     * @param context App context
     * @param enable  enable or disable double tap power button gesture.
     * @return {@code true} if the setting is updated.
     */
    public static boolean setDoubleTapPowerButtonGestureEnabled(
            @NonNull Context context, boolean enable) {
        return Settings.Secure.putInt(
                context.getContentResolver(),
                DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED,
                enable ? ON : OFF);
    }

    /**
     * @param context App context
     * @return true if double tap on the power button gesture for camera launch is currently
     * enabled.
     */
    public static boolean isDoubleTapPowerButtonGestureForCameraLaunchEnabled(
            @NonNull Context context) {
        return Settings.Secure.getInt(
                context.getContentResolver(),
                DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION,
                context.getResources()
                        .getInteger(
                                com.android.internal.R.integer
                                        .config_doubleTapPowerGestureMultiTargetDefaultAction))
                == DOUBLE_TAP_POWER_BUTTON_CAMERA_LAUNCH_VALUE;
    }

    /**
     * Sets double tap power button gesture behavior to launch the camera.
     *
     * @param context App context
     * @return {@code true} if the setting is updated.
     */
    public static boolean setDoubleTapPowerButtonForCameraLaunch(@NonNull Context context) {
        return Settings.Secure.putInt(
                context.getContentResolver(),
                DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION,
                DOUBLE_TAP_POWER_BUTTON_CAMERA_LAUNCH_VALUE);
    }

    /**
     * Sets double tap power button gesture behavior to launch the wallet.
     *
     * @param context App context
     * @return {@code true} if the setting is updated.
     */
    public static boolean setDoubleTapPowerButtonForWalletLaunch(@NonNull Context context) {
        return Settings.Secure.putInt(
                context.getContentResolver(),
                DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION,
                DOUBLE_TAP_POWER_BUTTON_WALLET_LAUNCH_VALUE);
    }

    /**
     * Registers observer for settings state.
     *
     * @param observer Settings Content Observer
     */
    public static void registerObserver(
            @NonNull Context context, @NonNull ContentObserver observer) {
        final ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(
                DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED_URI, true, observer);
        resolver.registerContentObserver(
                DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION_URI, true, observer);
    }

    /** Unregisters observer. */
    public static void unregisterObserver(
            @NonNull Context context, @NonNull ContentObserver observer) {
        final ContentResolver resolver = context.getContentResolver();
        resolver.unregisterContentObserver(observer);
    }
}
