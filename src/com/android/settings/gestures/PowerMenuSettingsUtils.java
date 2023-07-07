/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.provider.Settings;

/** Common code for long press power settings shared between controllers. */
final class PowerMenuSettingsUtils {

    /** Setting storing the current behaviour of long press power. */
    private static final String POWER_BUTTON_LONG_PRESS_SETTING =
            Settings.Global.POWER_BUTTON_LONG_PRESS;

    /** Setting storing the current behaviour of key chord power + volume up. */
    private static final String KEY_CHORD_POWER_VOLUME_UP_SETTING =
            Settings.Global.KEY_CHORD_POWER_VOLUME_UP;

    /**
     * Value used for long press power button behaviour when long press power for Assistant is
     * disabled.
     *
     * <p>If this value matches long press power for Assistant, then it falls back to Global Actions
     * panel (i.e., the Power Menu), depending on their respective settings.
     */
    private static final int POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE =
            com.android.internal.R.integer.config_longPressOnPowerBehavior;

    /**
     * Value used for key chord power + volume up behaviour when long press power for Assistant is
     * disabled.
     */
    private static final int KEY_CHORD_POWER_VOLUME_UP_DEFAULT_VALUE_RESOURCE =
            com.android.internal.R.integer.config_keyChordPowerVolumeUp;

    private static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1; // a.k.a., Power Menu
    private static final int LONG_PRESS_POWER_ASSISTANT_VALUE = 5; // Settings.Secure.ASSISTANT

    private static final int KEY_CHORD_POWER_VOLUME_UP_GLOBAL_ACTIONS = 2;

    private static final Uri POWER_BUTTON_LONG_PRESS_URI =
            Settings.Global.getUriFor(POWER_BUTTON_LONG_PRESS_SETTING);

    /**
     * @return true if long press power for assistant is currently enabled.
     */
    public static boolean isLongPressPowerForAssistantEnabled(Context context) {
        int longPressPowerSettingValue = Settings.Global.getInt(
                context.getContentResolver(),
                POWER_BUTTON_LONG_PRESS_SETTING,
                context.getResources().getInteger(POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE));
        return longPressPowerSettingValue == LONG_PRESS_POWER_ASSISTANT_VALUE;
    }

    /**
     * @return true if long press power for assistant setting is available on the device.
     */
    public static boolean isLongPressPowerSettingAvailable(Context context) {
        if (!context.getResources().getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable)) {
            return false;
        }

        int defaultLongPressPowerSettingValue =
                context.getResources().getInteger(POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE);
        switch (defaultLongPressPowerSettingValue) {
            case LONG_PRESS_POWER_GLOBAL_ACTIONS:
            case LONG_PRESS_POWER_ASSISTANT_VALUE:
                // We support switching between Power Menu and Digital Assistant.
                return true;
            default:
                // All other combinations are not supported.
                return false;
        }
    }

    public static boolean setLongPressPowerForAssistant(Context context) {
        if (Settings.Global.putInt(
                context.getContentResolver(),
                POWER_BUTTON_LONG_PRESS_SETTING,
                LONG_PRESS_POWER_ASSISTANT_VALUE)) {
            // Make power + volume up buttons to open the power menu
            Settings.Global.putInt(
                    context.getContentResolver(),
                    KEY_CHORD_POWER_VOLUME_UP_SETTING,
                    KEY_CHORD_POWER_VOLUME_UP_GLOBAL_ACTIONS);
            return true;
        }
        return false;
    }

    public static boolean setLongPressPowerForPowerMenu(Context context) {
        if (Settings.Global.putInt(
                context.getContentResolver(),
                POWER_BUTTON_LONG_PRESS_SETTING,
                LONG_PRESS_POWER_GLOBAL_ACTIONS)) {
            // We restore power + volume up buttons to the default action.
            int keyChordDefaultValue =
                    context.getResources()
                            .getInteger(KEY_CHORD_POWER_VOLUME_UP_DEFAULT_VALUE_RESOURCE);
            Settings.Global.putInt(
                    context.getContentResolver(),
                    KEY_CHORD_POWER_VOLUME_UP_SETTING,
                    keyChordDefaultValue);
            return true;
        }
        return false;
    }

    private final Context mContext;
    private final SettingsObserver mSettingsObserver;

    PowerMenuSettingsUtils(Context context) {
        mContext = context;
        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
    }

    /**
     * Registers callback for observing SettingsProvider state.
     *
     * @param callback for state changes
     */
    public void registerObserver(SettingsStateCallback callback) {
        mSettingsObserver.setCallback(callback);
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(POWER_BUTTON_LONG_PRESS_URI, true, mSettingsObserver);
    }

    /** Unregisters callback for observing SettingsProvider state. */
    public void unregisterObserver() {
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.unregisterContentObserver(mSettingsObserver);
    }

    /** An interface for when SettingsProvider key state changes. */
    public interface SettingsStateCallback {
        /** Callback method for SettingsProvider key state changes. */
        void onChange(Uri uri);
    }

    private static final class SettingsObserver extends ContentObserver {
        private SettingsStateCallback mCallback;

        SettingsObserver(Handler handler) {
            super(handler);
        }

        private void setCallback(SettingsStateCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mCallback != null) {
                mCallback.onChange(uri);
            }
        }
    }
}
