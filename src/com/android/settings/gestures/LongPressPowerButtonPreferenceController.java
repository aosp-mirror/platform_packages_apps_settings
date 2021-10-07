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

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Configures the behaviour of long press power button action.
 */
public class LongPressPowerButtonPreferenceController extends TogglePreferenceController {

    private static final String POWER_BUTTON_LONG_PRESS_SETTING =
            Settings.Global.POWER_BUTTON_LONG_PRESS;
    private static final String KEY_CHORD_POWER_VOLUME_UP_SETTING =
            Settings.Global.KEY_CHORD_POWER_VOLUME_UP;

    private static final String FOOTER_HINT_KEY = "power_menu_power_volume_up_hint";
    private static final String ASSIST_SWITCH_KEY = "gesture_power_menu_long_press_for_assist";

    /**
     * Values used for long press power button behaviour when Assist setting is enabled.
     *
     * {@link com.android.server.policy.PhoneWindowManager#LONG_PRESS_POWER_GLOBAL_ACTIONS} for
     * source of the value.
     */
    @VisibleForTesting
    static final int LONG_PRESS_POWER_NO_ACTION = 0;
    @VisibleForTesting
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
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
            com.android.internal.R.integer.config_longPressOnPowerBehavior;

    private static final int KEY_CHORD_POWER_VOLUME_UP_DEFAULT_VALUE_RESOURCE =
            com.android.internal.R.integer.config_keyChordPowerVolumeUp;

    @MonotonicNonNull
    @VisibleForTesting
    Preference mFooterHint;

    @MonotonicNonNull
    @VisibleForTesting
    Preference mAssistSwitch;

    public LongPressPowerButtonPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mFooterHint = screen.findPreference(FOOTER_HINT_KEY);
        mAssistSwitch = screen.findPreference(ASSIST_SWITCH_KEY);
        refreshStateDisplay();
    }

    @Override
    public CharSequence getSummary() {
        final int powerButtonValue = getPowerButtonValue();
        if (powerButtonValue == LONG_PRESS_POWER_ASSISTANT_VALUE) {
            return mContext.getString(R.string.power_menu_summary_long_press_for_assist_enabled);
        } else if (powerButtonValue == LONG_PRESS_POWER_GLOBAL_ACTIONS) {
            return mContext.getString(
                    R.string.power_menu_summary_long_press_for_assist_disabled_with_power_menu);
        } else {
            return mContext.getString(
                    R.string.power_menu_summary_long_press_for_assist_disabled_no_action);
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
        return getPowerButtonValue() == LONG_PRESS_POWER_ASSISTANT_VALUE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (setPowerLongPressValue(isChecked)) {
            // The key chord value is dependant on the long press setting and it always
            // toggled in tandem. POWER_BUTTON_LONG_PRESS_SETTING is always the source
            // of truth for both.
            setPowerVolumeChordValue(isChecked);
            refreshStateDisplay();
            return true;
        }

        return false;
    }

    private void refreshStateDisplay() {
        if (mAssistSwitch != null) {
            mAssistSwitch.setSummary(getSummary());
        }

        if (mFooterHint != null) {
            String footerHintText = mContext.getString(R.string.power_menu_power_volume_up_hint);
            // If the device supports hush gesture, we need to notify the user where to find
            // the setting.
            if (mContext.getResources().getBoolean(
                    com.android.internal.R.bool.config_volumeHushGestureEnabled)) {
                footerHintText = footerHintText + "\n\n" + mContext.getString(
                        R.string.power_menu_power_prevent_ringing_hint);
            }

            mFooterHint.setSummary(footerHintText);
            mFooterHint.setVisible(isPowerMenuKeyChordEnabled(mContext));
        }
    }

    private int getPowerButtonValue() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                POWER_BUTTON_LONG_PRESS_SETTING,
                mContext.getResources().getInteger(POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE));
    }

    private static boolean isPowerMenuKeyChordEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                KEY_CHORD_POWER_VOLUME_UP_SETTING,
                context.getResources().getInteger(
                        com.android.internal.R.integer.config_keyChordPowerVolumeUp))
                == KEY_CHORD_POWER_VOLUME_UP_GLOBAL_ACTIONS;
    }

    private boolean setPowerLongPressValue(boolean isChecked) {
        if (isChecked) {
            return Settings.Global.putInt(mContext.getContentResolver(),
                    POWER_BUTTON_LONG_PRESS_SETTING, LONG_PRESS_POWER_ASSISTANT_VALUE);
        }

        // We need to determine the right disabled value based on the device default
        // for long-press power.

        // If the default is to start the assistant, then the fallback is GlobalActions.
        final int defaultPowerButtonValue = mContext.getResources().getInteger(
                POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE);
        if (defaultPowerButtonValue == LONG_PRESS_POWER_ASSISTANT_VALUE) {
            return Settings.Global.putInt(mContext.getContentResolver(),
                    POWER_BUTTON_LONG_PRESS_SETTING, LONG_PRESS_POWER_GLOBAL_ACTIONS);
        }

        // If the default is something different than Assist, we use that default.
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

        // We restore key chord to the default value.
        int keyChordDefaultValue = mContext.getResources().getInteger(
                KEY_CHORD_POWER_VOLUME_UP_DEFAULT_VALUE_RESOURCE);
        return Settings.Global.putInt(mContext.getContentResolver(),
                KEY_CHORD_POWER_VOLUME_UP_SETTING, keyChordDefaultValue);
    }

}
