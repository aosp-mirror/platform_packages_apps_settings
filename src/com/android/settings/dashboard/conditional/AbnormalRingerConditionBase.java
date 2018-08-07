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

package com.android.settings.dashboard.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.provider.Settings;

import com.android.settings.R;

public abstract class AbnormalRingerConditionBase extends Condition {

    private final IntentFilter mFilter;

    protected final AudioManager mAudioManager;

    private final RingerModeChangeReceiver mReceiver;

    AbnormalRingerConditionBase(ConditionManager manager) {
        super(manager);
        mAudioManager =
                (AudioManager) mManager.getContext().getSystemService(Context.AUDIO_SERVICE);
        mReceiver = new RingerModeChangeReceiver(this);

        mFilter = new IntentFilter(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
        manager.getContext().registerReceiver(mReceiver, mFilter);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] {
                mManager.getContext().getText(R.string.condition_device_muted_action_turn_on_sound)
        };
    }

    @Override
    public void onPrimaryClick() {
        mManager.getContext().startActivity(
                new Intent(Settings.ACTION_SOUND_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    public void onActionClick(int index) {
        mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 1, 0 /* flags */);
        refreshState();
    }

    static class RingerModeChangeReceiver extends BroadcastReceiver {

        private final AbnormalRingerConditionBase mCondition;

        public RingerModeChangeReceiver(AbnormalRingerConditionBase condition) {
            mCondition = condition;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                mCondition.refreshState();
            }
        }
    }
}
