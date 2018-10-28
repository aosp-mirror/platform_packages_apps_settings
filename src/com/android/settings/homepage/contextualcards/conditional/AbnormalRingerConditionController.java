/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.provider.Settings;

public abstract class AbnormalRingerConditionController implements ConditionalCardController {

    private static final IntentFilter FILTER =
            new IntentFilter(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);

    protected final AudioManager mAudioManager;
    private final Context mAppContext;
    private final ConditionManager mConditionManager;
    private final RingerModeChangeReceiver mReceiver;

    public AbnormalRingerConditionController(Context appContext, ConditionManager manager) {
        mAppContext = appContext;
        mConditionManager = manager;
        mAudioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        mReceiver = new RingerModeChangeReceiver();
    }

    @Override
    public void onPrimaryClick(Context context) {
        context.startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
    }

    @Override
    public void onActionClick() {
        mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 1, 0 /* flags */);
    }

    @Override
    public void startMonitoringStateChange() {
        mAppContext.registerReceiver(mReceiver, FILTER);
    }

    @Override
    public void stopMonitoringStateChange() {
        mAppContext.unregisterReceiver(mReceiver);
    }

    class RingerModeChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                mConditionManager.onConditionChanged();
            }
        }
    }
}
