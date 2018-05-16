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

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.provider.Settings;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

public class RingerMutedCondition extends AbnormalRingerConditionBase {

    private final NotificationManager mNotificationManager;

    RingerMutedCondition(ConditionManager manager) {
        super(manager);
        mNotificationManager =
                (NotificationManager) mManager.getContext().getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void refreshState() {
        int zen = Settings.Global.ZEN_MODE_OFF;
        if (mNotificationManager != null) {
            zen = mNotificationManager.getZenMode();
        }
        final boolean zenModeEnabled = zen != Settings.Global.ZEN_MODE_OFF;
        final boolean isSilent =
                mAudioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_SILENT;
        setActive(isSilent && !zenModeEnabled);
    }

    @Override
    public int getMetricsConstant() {
        return MetricsProto.MetricsEvent.SETTINGS_CONDITION_DEVICE_MUTED;
    }

    @Override
    public Drawable getIcon() {
        return mManager.getContext().getDrawable(R.drawable.ic_notifications_off_24dp);
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getText(R.string.condition_device_muted_title);
    }

    @Override
    public CharSequence getSummary() {
        return mManager.getContext().getText(R.string.condition_device_muted_summary);
    }
}
