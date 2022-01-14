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

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import com.google.common.annotations.VisibleForTesting;

/**
 * Preference controller for the ramping ringer setting key, controlled via {@link AudioManager}.
 *
 * <p>This preference depends on the {@link Settings.System#RING_VIBRATION_INTENSITY}, and it will
 * be disabled and display the unchecked state when the ring intensity is set to OFF. The actual
 * ramping ringer setting will not be overwritten when the ring intensity is turned off, so the
 * user original value will be naturally restored when the ring intensity is enabled again.
 */
public class VibrationRampingRingerTogglePreferenceController
        extends TogglePreferenceController implements LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    static final String DEVICE_CONFIG_KEY = "ramping_ringer_enabled";

    private final ContentObserver mSettingObserver;
    private final Vibrator mVibrator;
    private final AudioManager mAudioManager;

    private Preference mPreference;

    public VibrationRampingRingerTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mVibrator = context.getSystemService(Vibrator.class);
        mAudioManager = context.getSystemService(AudioManager.class);
        mSettingObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateState(mPreference);
            }
        };
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean rampingRingerEnabledOnTelephonyConfig =
                DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_TELEPHONY, DEVICE_CONFIG_KEY, false);
        return (Utils.isVoiceCapable(mContext) && !rampingRingerEnabledOnTelephonyConfig)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.APPLY_RAMPING_RINGER),
                /* notifyForDescendants= */ false,
                mSettingObserver);
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.RING_VIBRATION_INTENSITY),
                /* notifyForDescendants= */ false,
                mSettingObserver);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingObserver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setEnabled(isRingVibrationEnabled());
    }

    @Override
    public boolean isChecked() {
        return isRingVibrationEnabled() && mAudioManager.isRampingRingerEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isRingVibrationEnabled()) {
            // Don't update ramping ringer setting value if ring vibration is disabled.
            mAudioManager.setRampingRingerEnabled(isChecked);
        }
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setEnabled(isRingVibrationEnabled());
        }
    }

    private boolean isRingVibrationEnabled() {
        final int ringIntensity = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.RING_VIBRATION_INTENSITY,
                mVibrator.getDefaultVibrationIntensity(VibrationAttributes.USAGE_RINGTONE));
        return ringIntensity != Vibrator.VIBRATION_INTENSITY_OFF;
    }
}
