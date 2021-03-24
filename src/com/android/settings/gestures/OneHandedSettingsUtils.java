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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;

/**
 * The Util to query one-handed mode settings config
 */
public class OneHandedSettingsUtils {

    static final String SUPPORT_ONE_HANDED_MODE = "ro.support_one_handed_mode";

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

    OneHandedSettingsUtils(Context context) {
        mContext = context;
        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
    }

    /**
     * Get One-Handed mode support flag.
     */
    public static boolean isSupportOneHandedMode() {
        return SystemProperties.getBoolean(SUPPORT_ONE_HANDED_MODE, false);
    }

    /**
     * Get one-handed mode enable or disable flag from Settings provider.
     *
     * @param context App context
     * @return enable or disable one-handed mode flag.
     */
    public static boolean isOneHandedModeEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, 0) == 1;
    }

    /**
     * Set one-handed mode enable or disable flag to Settings provider.
     *
     * @param context App context
     * @param enable enable or disable one-handed mode.
     */
    public static void setSettingsOneHandedModeEnabled(Context context, boolean enable) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, enable ? 1 : 0);
    }

    /**
     * Get enabling taps app to exit one-handed mode flag from Settings provider.
     *
     * @param context App context
     * @return enable or disable taps app to exit.
     */
    public static boolean getSettingsTapsAppToExit(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.TAPS_APP_TO_EXIT, 1) == 1;
    }

    /**
     * Set enabling taps app to exit one-handed mode flag to Settings provider.
     *
     * @param context App context
     * @param enable  enable or disable when taping app to exit one-handed mode.
     */
    public static boolean setSettingsTapsAppToExit(Context context, boolean enable) {
        return Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.TAPS_APP_TO_EXIT, enable ? 1 : 0);
    }

    /**
     * Get one-handed mode timeout value from Settings provider.
     *
     * @param context App context
     * @return timeout value in seconds.
     */
    public static int getSettingsOneHandedModeTimeout(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedTimeout.MEDIUM.getValue() /* default MEDIUM(8) by UX */);
    }

    /**
     * Set one-handed mode timeout value to Settings provider.
     *
     * @param context App context
     * @param timeout timeout in seconds for exiting one-handed mode.
     */
    public static void setSettingsOneHandedModeTimeout(Context context, int timeout) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT, timeout);
    }

    /**
     * Get Swipe-down-notification enable or disable flag from Settings provider.
     *
     * @param context App context
     * @return enable or disable Swipe-down-notification flag.
     */
    public static boolean isSwipeDownNotificationEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED, 0) == 1;
    }

    /**
     * Set Swipe-down-notification enable or disable flag to Settings provider.
     *
     * @param context App context
     * @param enable enable or disable Swipe-down-notification.
     */
    public static void setSwipeDownNotificationEnabled(Context context, boolean enable) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED, enable ? 1 : 0);
    }

    /**
     * Register callback for observing Settings.Secure.ONE_HANDED_MODE_ENABLED state.
     * @param callback for state changes
     */
    public void registerToggleAwareObserver(TogglesCallback callback) {
        mSettingsObserver.observe();
        mSettingsObserver.setCallback(callback);
    }

    /**
     * Unregister callback for observing Settings.Secure.ONE_HANDED_MODE_ENABLED state.
     */
    public void unregisterToggleAwareObserver() {
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.unregisterContentObserver(mSettingsObserver);
    }

    private final class SettingsObserver extends ContentObserver {
        private TogglesCallback mCallback;

        private final Uri mOneHandedEnabledAware = Settings.Secure.getUriFor(
                Settings.Secure.ONE_HANDED_MODE_ENABLED);

        SettingsObserver(Handler handler) {
            super(handler);
        }

        private void setCallback(TogglesCallback callback) {
            mCallback = callback;
        }

        public void observe() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(mOneHandedEnabledAware, true, this);
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
         * @param uri
         */
        void onChange(Uri uri);
    }
}
