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
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.TogglePreferenceController;

/**
 * Configures the behaviour of long press power button action.
 */
public class LongPressPowerButtonPreferenceController extends TogglePreferenceController implements
        LifecycleObserver {

    private static final String POWER_BUTTON_LONG_PRESS_SETTING =
            Settings.Global.POWER_BUTTON_LONG_PRESS;
    private static final Uri POWER_BUTTON_LONG_PRESS_SETTING_URI =
            Settings.Global.getUriFor(POWER_BUTTON_LONG_PRESS_SETTING);
    private static final String KEY_CHORD_POWER_VOLUME_UP_SETTING =
            Settings.Global.KEY_CHORD_POWER_VOLUME_UP;

    // Used for fallback to global actions if necessary.
    @VisibleForTesting
    static final String CARDS_AVAILABLE_KEY =
            Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;
    @VisibleForTesting
    static final String CARDS_ENABLED_KEY = Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;

    /**
     * Values used for long press power button behaviour when Assist setting is enabled.
     *
     * {@link com.android.server.policy.PhoneWindowManager#LONG_PRESS_POWER_GLOBAL_ACTIONS} for
     * source of the value.
     */
    @VisibleForTesting
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    @VisibleForTesting
    static final int LONG_PRESS_POWER_SHUT_OFF = 2;
    @VisibleForTesting
    static final int LONG_PRESS_POWER_ASSISTANT_VALUE = 5; // Settings.Secure.ASSISTANT

    /**
     * Values used for volume key chord behaviour when Assist setting is enabled.
     *
     * Values based on config_keyChordPowerVolumeUp in
     * frameworks/base/core/res/res/values/config.xml
     */
    @VisibleForTesting
    static final int KEY_CHORD_POWER_VOLUME_UP_NO_ACTION = 0;
    @VisibleForTesting
    static final int KEY_CHORD_POWER_VOLUME_UP_MUTE_TOGGLE = 1;
    @VisibleForTesting
    static final int KEY_CHORD_POWER_VOLUME_UP_GLOBAL_ACTIONS = 2;

    /**
     * Value used for long press power button behaviour when the Assist setting is disabled.
     *
     * If this value matches Assist setting, then it falls back to Global Actions panel or
     * power menu, depending on their respective settings.
     */
    private static final int POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE =
            R.integer.config_longPressOnPowerBehavior;

    private static final int KEY_CHORD_POWER_VOLUME_UP_DEFAULT_VALUE_RESOURCE =
            R.integer.config_keyChordPowerVolumeUp;

    @Nullable
    private SettingObserver mSettingsObserver;

    public LongPressPowerButtonPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSettingsObserver = new SettingObserver(screen.findPreference(getPreferenceKey()));
    }

    /**
     * Called when the settings pages resumes.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (mSettingsObserver != null) {
            mSettingsObserver.register();
        }
    }

    /**
     * Called when the settings page pauses.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (mSettingsObserver != null) {
            mSettingsObserver.unregister();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean enabled = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable);
        return enabled ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        final int powerButtonValue = Settings.Global.getInt(mContext.getContentResolver(),
                POWER_BUTTON_LONG_PRESS_SETTING,
                mContext.getResources().getInteger(POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE));
        return powerButtonValue == LONG_PRESS_POWER_ASSISTANT_VALUE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (setPowerLongPressValue(isChecked)) {
            // The key chord value is dependant on the long press setting and it always
            // toggled in tandem. POWER_BUTTON_LONG_PRESS_SETTING is always the source
            // of truth for both.
            return setPowerVolumeChordValue(isChecked);
        }

        return false;
    }

    private boolean setPowerLongPressValue(boolean isChecked) {
        if (isChecked) {
            return Settings.Global.putInt(mContext.getContentResolver(),
                    POWER_BUTTON_LONG_PRESS_SETTING, LONG_PRESS_POWER_ASSISTANT_VALUE);
        }

        // We need to determine the right disabled value - we set it to device default
        // if it's different than Assist, otherwise we fallback to either global actions or power
        //menu.
        final int defaultPowerButtonValue = mContext.getResources().getInteger(
                POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE);
        if (defaultPowerButtonValue == LONG_PRESS_POWER_ASSISTANT_VALUE) {
            final int fallbackValue = isCardsOrControlsAvailable() ? LONG_PRESS_POWER_GLOBAL_ACTIONS
                    : LONG_PRESS_POWER_SHUT_OFF;
            return Settings.Global.putInt(mContext.getContentResolver(),
                    POWER_BUTTON_LONG_PRESS_SETTING, fallbackValue);
        }

        return Settings.Global.putInt(mContext.getContentResolver(),
                POWER_BUTTON_LONG_PRESS_SETTING, defaultPowerButtonValue);
    }

    /**
     * Updates {@link Settings.Global.KEY_CHORD_POWER_VOLUME_UP} based on the changed value of
     * {@link #POWER_BUTTON_LONG_PRESS_SETTING}. If power button is used for Assist, key chord
     * should show the power menu.
     */
    private boolean setPowerVolumeChordValue(boolean isPowerButtonLongPressChecked) {
        if (isPowerButtonLongPressChecked) {
            return Settings.Global.putInt(mContext.getContentResolver(),
                    KEY_CHORD_POWER_VOLUME_UP_SETTING, KEY_CHORD_POWER_VOLUME_UP_GLOBAL_ACTIONS);
        }

        // If the key chord defaults to mute toggle, we restore that setting if LPP option is
        // disabled. Otherwise we default to no action.
        boolean isMuteToggleKeyChordDefault = mContext.getResources().getInteger(
                KEY_CHORD_POWER_VOLUME_UP_DEFAULT_VALUE_RESOURCE)
                == KEY_CHORD_POWER_VOLUME_UP_MUTE_TOGGLE;
        return Settings.Global.putInt(mContext.getContentResolver(),
                KEY_CHORD_POWER_VOLUME_UP_SETTING, isMuteToggleKeyChordDefault
                        ? KEY_CHORD_POWER_VOLUME_UP_MUTE_TOGGLE
                        : KEY_CHORD_POWER_VOLUME_UP_NO_ACTION);
    }

    /**
     * Returns true if the global actions menu on power button click is enabled via any of the
     * content options.
     */
    private boolean isCardsOrControlsAvailable() {
        final ContentResolver resolver = mContext.getContentResolver();
        final boolean cardsAvailable = Settings.Secure.getInt(resolver, CARDS_AVAILABLE_KEY, 0)
                != 0;
        final boolean controlsAvailable = mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CONTROLS);
        return cardsAvailable || controlsAvailable;
    }

    private final class SettingObserver extends ContentObserver {

        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(new Handler(Looper.getMainLooper()));
            mPreference = preference;
        }

        public void register() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(POWER_BUTTON_LONG_PRESS_SETTING_URI, false, this);
        }

        public void unregister() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateState(mPreference);
        }
    }

}
