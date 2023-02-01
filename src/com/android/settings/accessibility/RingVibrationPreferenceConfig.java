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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.media.AudioManager;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.provider.Settings;

/** General configuration for ringtone vibration intensity settings. */
public class RingVibrationPreferenceConfig extends VibrationPreferenceConfig {
    private final AudioManager mAudioManager;

    public RingVibrationPreferenceConfig(Context context) {
        super(context, Settings.System.RING_VIBRATION_INTENSITY,
                VibrationAttributes.USAGE_RINGTONE);
        mAudioManager = context.getSystemService(AudioManager.class);
    }

    @Override
    public boolean isRestrictedByRingerModeSilent() {
        // Incoming calls never vibrate when the phone is in silent mode.
        return true;
    }

    @Override
    public boolean updateIntensity(int intensity) {
        final boolean success = super.updateIntensity(intensity);

        // VIBRATE_WHEN_RINGING is deprecated but should still reflect the intensity setting.
        // Ramping ringer is independent of the ring intensity and should not be affected.
        Settings.System.putInt(mContentResolver, Settings.System.VIBRATE_WHEN_RINGING,
                (intensity == Vibrator.VIBRATION_INTENSITY_OFF) ? OFF : ON);

        return success;
    }
}
