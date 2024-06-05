/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Vibration intensity settings configuration to be shared between different preference
 * controllers that handle the same setting key.
 */
public abstract class VibrationPreferenceConfig {

    /**
     * SettingsProvider key for the main "Vibration & haptics" toggle preference, that can disable
     * all device vibrations.
     */
    public static final String MAIN_SWITCH_SETTING_KEY = Settings.System.VIBRATE_ON;
    private static final VibrationEffect PREVIEW_VIBRATION_EFFECT =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK);

    protected final ContentResolver mContentResolver;
    private final AudioManager mAudioManager;
    private final Vibrator mVibrator;
    private final String mSettingKey;
    private final String mRingerModeSilentSummary;
    private final int mDefaultIntensity;
    private final VibrationAttributes mPreviewVibrationAttributes;

    /** Returns true if the user setting for enabling device vibrations is enabled. */
    public static boolean isMainVibrationSwitchEnabled(ContentResolver contentResolver) {
        return Settings.System.getInt(contentResolver, MAIN_SWITCH_SETTING_KEY, ON) == ON;
    }

    /** Play a vibration effect with intensity just selected by the user. */
    public static void playVibrationPreview(Vibrator vibrator,
            @VibrationAttributes.Usage int vibrationUsage) {
        playVibrationPreview(vibrator, createPreviewVibrationAttributes(vibrationUsage));
    }

    /**
     * Play a vibration effect with intensity just selected by the user.
     *
     * @param vibrator The {@link Vibrator} used to play the vibration.
     * @param vibrationAttributes The {@link VibrationAttributes} to indicate the
     *        vibration information.
     */
    public static void playVibrationPreview(Vibrator vibrator,
            VibrationAttributes vibrationAttributes) {
        vibrator.vibrate(PREVIEW_VIBRATION_EFFECT, vibrationAttributes);
    }

    public VibrationPreferenceConfig(Context context, String settingKey,
            @VibrationAttributes.Usage int vibrationUsage) {
        mContentResolver = context.getContentResolver();
        mVibrator = context.getSystemService(Vibrator.class);
        mAudioManager = context.getSystemService(AudioManager.class);
        mRingerModeSilentSummary = context.getString(
                R.string.accessibility_vibration_setting_disabled_for_silent_mode_summary);
        mSettingKey = settingKey;
        mDefaultIntensity = mVibrator.getDefaultVibrationIntensity(vibrationUsage);
        mPreviewVibrationAttributes = createPreviewVibrationAttributes(vibrationUsage);
    }

    /** Returns the setting key for this setting preference. */
    public String getSettingKey() {
        return mSettingKey;
    }

    /** Returns the summary string for this setting preference. */
    @Nullable
    public CharSequence getSummary() {
        return isRestrictedByRingerModeSilent() && isRingerModeSilent()
                ? mRingerModeSilentSummary : null;
    }

    /** Returns true if this setting preference is enabled for user update. */
    public boolean isPreferenceEnabled() {
        return isMainVibrationSwitchEnabled(mContentResolver)
                && (!isRestrictedByRingerModeSilent() || !isRingerModeSilent());
    }

    /**
     * Returns true if this setting preference should be disabled when the device is in silent mode.
     */
    public boolean isRestrictedByRingerModeSilent() {
        return false;
    }

    /** Returns the default intensity to be displayed when the setting value is not set. */
    public int getDefaultIntensity() {
        return mDefaultIntensity;
    }

    /** Reads setting value for corresponding {@link VibrationPreferenceConfig} */
    public int readIntensity() {
        return Settings.System.getInt(mContentResolver, mSettingKey, mDefaultIntensity);
    }

    /** Update setting value for corresponding {@link VibrationPreferenceConfig} */
    public boolean updateIntensity(int intensity) {
        return Settings.System.putInt(mContentResolver, mSettingKey, intensity);
    }

    /** Play a vibration effect with intensity just selected by the user. */
    public void playVibrationPreview() {
        mVibrator.vibrate(PREVIEW_VIBRATION_EFFECT, mPreviewVibrationAttributes);
    }

    private boolean isRingerModeSilent() {
        // AudioManager.isSilentMode() also returns true when ringer mode is VIBRATE.
        // The vibration preferences are only disabled when the ringer mode is SILENT.
        return mAudioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_SILENT;
    }

    static VibrationAttributes createPreviewVibrationAttributes(
            @VibrationAttributes.Usage int vibrationUsage) {
        return new VibrationAttributes.Builder()
                .setUsage(vibrationUsage)
                .setFlags(
                        // Enforce fresh settings to be applied for the preview vibration, as they
                        // are played immediately after the new user values are set.
                        VibrationAttributes.FLAG_INVALIDATE_SETTINGS_CACHE
                        // Bypass user settings to allow vibration previews to be played while in
                        // limited interruptions' mode, e.g. zen mode.
                        | VibrationAttributes.FLAG_BYPASS_INTERRUPTION_POLICY)
                .build();
    }

    /** {@link ContentObserver} for a setting described by a {@link VibrationPreferenceConfig}. */
    public static final class SettingObserver extends ContentObserver {
        private static final Uri MAIN_SWITCH_SETTING_URI =
                Settings.System.getUriFor(MAIN_SWITCH_SETTING_KEY);
        private static final IntentFilter INTERNAL_RINGER_MODE_CHANGED_INTENT_FILTER =
                new IntentFilter(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);

        private final Uri mUri;
        @Nullable
        private final BroadcastReceiver mRingerModeChangeReceiver;

        private AbstractPreferenceController mPreferenceController;
        private Preference mPreference;

        /** Creates observer for given preference. */
        public SettingObserver(VibrationPreferenceConfig preferenceConfig) {
            super(new Handler(/* async= */ true));
            mUri = Settings.System.getUriFor(preferenceConfig.getSettingKey());

            if (preferenceConfig.isRestrictedByRingerModeSilent()) {
                // If this preference is restricted by AudioManager.getRingerModeInternal() result
                // for the device mode, then listen to changes in that value using the broadcast
                // intent action INTERNAL_RINGER_MODE_CHANGED_ACTION.
                mRingerModeChangeReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final String action = intent.getAction();
                        if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                            notifyChange();
                        }
                    }
                };
            } else {
                // No need to register a receiver if this preference is not affected by ringer mode.
                mRingerModeChangeReceiver = null;
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mUri.equals(uri) || MAIN_SWITCH_SETTING_URI.equals(uri)) {
                notifyChange();
            }
        }

        private void notifyChange() {
            if (mPreferenceController != null && mPreference != null) {
                mPreferenceController.updateState(mPreference);
            }
        }

        /**
         * Register this observer to given {@link Context}, to be called from lifecycle
         * {@code onStart} method.
         */
        public void register(Context context) {
            if (mRingerModeChangeReceiver != null) {
                context.registerReceiver(mRingerModeChangeReceiver,
                        INTERNAL_RINGER_MODE_CHANGED_INTENT_FILTER);
            }
            context.getContentResolver().registerContentObserver(
                    mUri, /* notifyForDescendants= */ false, this);
            context.getContentResolver().registerContentObserver(
                    MAIN_SWITCH_SETTING_URI, /* notifyForDescendants= */ false, this);
        }

        /**
         * Unregister this observer from given {@link Context}, to be called from lifecycle
         * {@code onStop} method.
         */
        public void unregister(Context context) {
            if (mRingerModeChangeReceiver != null) {
                context.unregisterReceiver(mRingerModeChangeReceiver);
            }
            context.getContentResolver().unregisterContentObserver(this);
        }

        /**
         * Binds this observer to given controller and preference, once it has been displayed to the
         * user.
         */
        public void onDisplayPreference(AbstractPreferenceController controller,
                Preference preference) {
            mPreferenceController = controller;
            mPreference = preference;
        }
    }
}
