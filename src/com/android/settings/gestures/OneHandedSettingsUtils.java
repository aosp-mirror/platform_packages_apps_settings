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

package com.android.settings.gestures;

import static com.android.internal.accessibility.AccessibilityShortcutController.ONE_HANDED_COMPONENT_NAME;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

/**
 * The Util to query one-handed mode settings config
 */
public class OneHandedSettingsUtils {

    static final String ONE_HANDED_MODE_TARGET_NAME =
            ONE_HANDED_COMPONENT_NAME.getShortClassName();

    static final String SUPPORT_ONE_HANDED_MODE = "ro.support_one_handed_mode";
    static final int OFF = 0;
    static final int ON = 1;
    static final Uri ONE_HANDED_MODE_ENABLED_URI =
            Settings.Secure.getUriFor(Settings.Secure.ONE_HANDED_MODE_ENABLED);
    static final Uri SHOW_NOTIFICATION_ENABLED_URI =
            Settings.Secure.getUriFor(Settings.Secure.SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED);
    static final Uri SOFTWARE_SHORTCUT_ENABLED_URI =
            Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS);
    static final Uri HARDWARE_SHORTCUT_ENABLED_URI =
            Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);

    public enum OneHandedTimeout {
        NEVER(0), SHORT(4), MEDIUM(8), LONG(12);

        private final int mValue;

        OneHandedTimeout(int value) {
            this.mValue = value;
        }

        public int getValue() {
            return mValue;
        }
    }

    private final Context mContext;
    private final SettingsObserver mSettingsObserver;

    private static int sCurrentUserId;

    OneHandedSettingsUtils(Context context) {
        mContext = context;
        sCurrentUserId = UserHandle.myUserId();
        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
    }

    /**
     * Gets One-Handed mode support flag.
     */
    public static boolean isSupportOneHandedMode() {
        return SystemProperties.getBoolean(SUPPORT_ONE_HANDED_MODE, false);
    }

    /**
     * Gets one-handed mode feature enable or disable flag from Settings provider.
     *
     * @param context App context
     * @return enable or disable one-handed mode flag.
     */
    public static boolean isOneHandedModeEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, OFF, sCurrentUserId) == ON;
    }

    /**
     * Sets one-handed mode enable or disable flag to Settings provider.
     *
     * @param context App context
     * @param enable  enable or disable one-handed mode.
     */
    public static void setOneHandedModeEnabled(Context context, boolean enable) {
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, enable ? ON : OFF, sCurrentUserId);
    }

    /**
     * Gets enabling taps app to exit one-handed mode flag from Settings provider.
     *
     * @param context App context
     * @return enable or disable taps app to exit.
     */
    public static boolean isTapsAppToExitEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.TAPS_APP_TO_EXIT, OFF, sCurrentUserId) == ON;
    }

    /**
     * Sets enabling taps app to exit one-handed mode flag to Settings provider.
     *
     * @param context App context
     * @param enable  enable or disable when taping app to exit one-handed mode.
     */
    public static boolean setTapsAppToExitEnabled(Context context, boolean enable) {
        return Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.TAPS_APP_TO_EXIT, enable ? ON : OFF, sCurrentUserId);
    }

    /**
     * Gets one-handed mode timeout value from Settings provider.
     *
     * @param context App context
     * @return timeout value in seconds.
     */
    public static int getTimeoutValue(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedTimeout.MEDIUM.getValue() /* default MEDIUM(8) by UX */,
                sCurrentUserId);
    }

    /**
     * Gets current user id from OneHandedSettingsUtils
     *
     * @return the current user id in OneHandedSettingsUtils
     */
    public static int getUserId() {
        return sCurrentUserId;
    }

    /**
     * Sets specific user id for OneHandedSettingsUtils
     *
     * @param userId the user id to be updated
     */
    public static void setUserId(int userId) {
        sCurrentUserId = userId;
    }

    /**
     * Sets one-handed mode timeout value to Settings provider.
     *
     * @param context App context
     * @param timeout timeout in seconds for exiting one-handed mode.
     */
    public static void setTimeoutValue(Context context, int timeout) {
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT, timeout, sCurrentUserId);
    }

    /**
     * Gets Swipe-down-notification enable or disable flag from Settings provider.
     *
     * @param context App context
     * @return enable or disable Swipe-down-notification flag.
     */
    public static boolean isSwipeDownNotificationEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED, OFF, sCurrentUserId) == ON;
    }

    /**
     * Sets Swipe-down-notification enable or disable flag to Settings provider.
     *
     * @param context App context
     * @param enable enable or disable Swipe-down-notification.
     */
    public static void setSwipeDownNotificationEnabled(Context context, boolean enable) {
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED, enable ? ON : OFF,
                sCurrentUserId);
    }

    /**
     * Set NavigationBar mode flag to Settings provider.
     * @param context App context
     * @param value Navigation bar mode:
     *  0 = 3 button
     *  1 = 2 button
     *  2 = fully gestural
     * @return true if the value was set, false on database errors.
     */
    @VisibleForTesting
    public boolean setNavigationBarMode(Context context, String value) {
        return Settings.Secure.putStringForUser(context.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, value, UserHandle.myUserId());
    }

    /**
     * Get NavigationBar mode flag from Settings provider.
     * @param context App context
     * @return Navigation bar mode:
     *  0 = 3 button
     *  1 = 2 button
     *  2 = fully gestural
     */
    public static int getNavigationBarMode(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, 2 /* fully gestural */, sCurrentUserId);
    }

    /**
     * Check if One-handed mode settings controllers can enabled or disabled.
     * @param context App context
     * @return true if controllers are able to enabled, false otherwise.
     *
     * Note: For better UX experience, just disabled controls that let users know to use
     * this feature, they need to make sure gesture navigation is turned on in system
     * navigation settings.
     */
    public static boolean canEnableController(Context context) {
        return ((OneHandedSettingsUtils.isOneHandedModeEnabled(context)
                && getNavigationBarMode(context) != 0 /* 3-button */)
                || getShortcutEnabled(context));
    }

    /**
     * Queries one-handed mode shortcut enabled in settings or not.
     *
     * @return true if user enabled one-handed shortcut in settings, false otherwise.
     */
    public static boolean getShortcutEnabled(Context context) {
        // Checks SOFTWARE_SHORTCUT_KEY
        final String targetsSW = Settings.Secure.getStringForUser(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS, sCurrentUserId);
        if (!TextUtils.isEmpty(targetsSW) && targetsSW.contains(ONE_HANDED_MODE_TARGET_NAME)) {
            return true;
        }

        // Checks HARDWARE_SHORTCUT_KEY
        final String targetsHW = Settings.Secure.getStringForUser(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE, sCurrentUserId);
        if (!TextUtils.isEmpty(targetsHW) && targetsHW.contains(ONE_HANDED_MODE_TARGET_NAME)) {
            return true;
        }
        return false;
    }

    /**
     * This is a test only API for set Shortcut enabled or not.
     */
    @VisibleForTesting
    public void setShortcutEnabled(Context context, boolean enabled) {
        final String targetName = enabled ? ONE_HANDED_MODE_TARGET_NAME : "";
        Settings.Secure.putStringForUser(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS, targetName, sCurrentUserId);
    }

    /**
     * Registers callback for observing Settings.Secure.ONE_HANDED_MODE_ENABLED state.
     * @param callback for state changes
     */
    public void registerToggleAwareObserver(TogglesCallback callback) {
        mSettingsObserver.observe();
        mSettingsObserver.setCallback(callback);
    }

    /**
     * Unregisters callback for observing Settings.Secure.ONE_HANDED_MODE_ENABLED state.
     */
    public void unregisterToggleAwareObserver() {
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.unregisterContentObserver(mSettingsObserver);
    }

    private final class SettingsObserver extends ContentObserver {
        private TogglesCallback mCallback;

        SettingsObserver(Handler handler) {
            super(handler);
        }

        private void setCallback(TogglesCallback callback) {
            mCallback = callback;
        }

        public void observe() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(ONE_HANDED_MODE_ENABLED_URI, true, this);
            resolver.registerContentObserver(SHOW_NOTIFICATION_ENABLED_URI, true, this);
            resolver.registerContentObserver(SOFTWARE_SHORTCUT_ENABLED_URI, true, this);
            resolver.registerContentObserver(HARDWARE_SHORTCUT_ENABLED_URI, true, this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mCallback != null) mCallback.onChange(uri);
        }
    }

    /**
     * An interface for when Settings.Secure key state changes.
     */
    public interface TogglesCallback {
        /**
         * Callback method for Settings.Secure key state changes.
         *
         * @param uri The Uri of the changed content.
         */
        void onChange(Uri uri);
    }
}
