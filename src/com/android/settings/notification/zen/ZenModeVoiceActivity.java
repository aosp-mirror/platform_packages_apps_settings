/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification.zen;

import static android.provider.Settings.EXTRA_DO_NOT_DISTURB_MODE_ENABLED;
import static android.provider.Settings.EXTRA_DO_NOT_DISTURB_MODE_MINUTES;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.icu.text.MessageFormat;
import android.media.AudioManager;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.format.DateFormat;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.utils.VoiceSettingsActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for modifying the Zen mode (Do not disturb) by voice
 * using the Voice Interaction API.
 */
public class ZenModeVoiceActivity extends VoiceSettingsActivity {
    private static final String TAG = "ZenModeVoiceActivity";
    private static final int MINUTES_MS = 60 * 1000;

    @Override
    protected boolean onVoiceSettingInteraction(Intent intent) {
        if (intent.hasExtra(EXTRA_DO_NOT_DISTURB_MODE_ENABLED)) {
            int minutes = intent.getIntExtra(EXTRA_DO_NOT_DISTURB_MODE_MINUTES, -1);
            Condition condition = null;
            int mode = Global.ZEN_MODE_OFF;

            if (intent.getBooleanExtra(EXTRA_DO_NOT_DISTURB_MODE_ENABLED, false)) {
                if (minutes > 0) {
                    condition = ZenModeConfig.toTimeCondition(this, minutes, UserHandle.myUserId());
                }
                mode = Global.ZEN_MODE_ALARMS;
            }
            setZenModeConfig(mode, condition);

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                // Show the current Zen Mode setting.
                audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION,
                         AudioManager.ADJUST_SAME,
                         AudioManager.FLAG_SHOW_UI);
            }
            notifySuccess(getChangeSummary(mode, minutes));
        } else {
            Log.v(TAG, "Missing extra android.provider.Settings.EXTRA_DO_NOT_DISTURB_MODE_ENABLED");
            finish();
        }
        return false;
    }

    private void setZenModeConfig(int mode, Condition condition) {
        if (condition != null) {
            NotificationManager.from(this).setZenMode(mode, condition.id, TAG);
        } else {
            NotificationManager.from(this).setZenMode(mode, null, TAG);
        }
     }

    /**
     * Produce a summary of the Zen mode change to be read aloud as TTS.
     */
    private CharSequence getChangeSummary(int mode, int minutes) {
        int indefinite = -1;

        switch (mode) {
            case Global.ZEN_MODE_ALARMS:
                indefinite = R.string.zen_mode_summary_alarms_only_indefinite;
                break;
            case Global.ZEN_MODE_OFF:
                indefinite = R.string.zen_mode_summary_always;
                break;
        };

        if (minutes < 0 || mode == Global.ZEN_MODE_OFF) {
            return getString(indefinite);
        }

        long time = System.currentTimeMillis() + minutes * MINUTES_MS;
        String skeleton = DateFormat.is24HourFormat(this, UserHandle.myUserId()) ? "Hm" : "hma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        CharSequence formattedTime = DateFormat.format(pattern, time);

        if (minutes < 60) {
            return buildMessage(R.string.zen_mode_summary_alarms_only_by_minute, minutes, formattedTime);
        } else if (minutes % 60 != 0) {
            return getResources().getString(R.string.zen_mode_summary_alarms_only_by_time, formattedTime);
        } else {
            int hours = minutes / 60;
            return buildMessage(R.string.zen_mode_summary_alarms_only_by_hour, hours, formattedTime);
        }
    }

    private CharSequence buildMessage(int resId, int count, CharSequence formattedTime) {
        MessageFormat msgFormat = new MessageFormat(
                getResources().getString(resId), Locale.getDefault());
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("count", count);
        arguments.put("time", formattedTime);
        return msgFormat.format(arguments);
    }
}
