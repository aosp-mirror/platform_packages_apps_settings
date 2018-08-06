/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import static android.app.NotificationChannel.USER_LOCKED_SOUND;
import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.core.PreferenceControllerMixin;

public class ImportancePreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener  {

    private static final String KEY_IMPORTANCE = "importance";
    private NotificationSettingsBase.ImportanceListener mImportanceListener;

    public ImportancePreferenceController(Context context,
            NotificationSettingsBase.ImportanceListener importanceListener,
            NotificationBackend backend) {
        super(context, backend);
        mImportanceListener = importanceListener;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_IMPORTANCE;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mChannel == null) {
            return false;
        }
        return !isDefaultChannel();
    }

    @Override
    public void updateState(Preference preference) {
        if (mAppRow!= null && mChannel != null) {
            preference.setEnabled(mAdmin == null && isChannelConfigurable());
            preference.setSummary(getImportanceSummary(mChannel));

            int importances = IMPORTANCE_HIGH - IMPORTANCE_MIN + 1;
            CharSequence[] entries = new CharSequence[importances];
            CharSequence[] values = new CharSequence[importances];

            int index = 0;
            for (int i = IMPORTANCE_HIGH; i >= IMPORTANCE_MIN; i--) {
                NotificationChannel channel = new NotificationChannel("", "", i);
                entries[index] = getImportanceSummary(channel);
                values[index] = String.valueOf(i);
                index++;
            }

            RestrictedListPreference pref = (RestrictedListPreference) preference;
            pref.setEntries(entries);
            pref.setEntryValues(values);
            pref.setValue(String.valueOf(mChannel.getImportance()));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel != null) {
            final int importance = Integer.parseInt((String) newValue);

            // If you are moving from an importance level without sound to one with sound,
            // but the sound you had selected was "Silence",
            // then set sound for this channel to your default sound,
            // because you probably intended to cause this channel to actually start making sound.
            if (mChannel.getImportance() < IMPORTANCE_DEFAULT
                    && !SoundPreferenceController.hasValidSound(mChannel)
                    && importance >= IMPORTANCE_DEFAULT) {
                mChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        mChannel.getAudioAttributes());
                mChannel.lockFields(USER_LOCKED_SOUND);
            }

            mChannel.setImportance(importance);
            mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
            saveChannel();
            mImportanceListener.onImportanceChanged();
        }
        return true;
    }

    protected String getImportanceSummary(NotificationChannel channel) {
        String summary = "";
        int importance = channel.getImportance();
        switch (importance) {
            case IMPORTANCE_UNSPECIFIED:
                summary = mContext.getString(R.string.notification_importance_unspecified);
                break;
            case NotificationManager.IMPORTANCE_MIN:
                summary = mContext.getString(R.string.notification_importance_min);
                break;
            case NotificationManager.IMPORTANCE_LOW:
                summary = mContext.getString(R.string.notification_importance_low);
                break;
            case NotificationManager.IMPORTANCE_DEFAULT:
                if (SoundPreferenceController.hasValidSound(channel)) {
                    summary = mContext.getString(R.string.notification_importance_default);
                } else {
                    summary = mContext.getString(R.string.notification_importance_low);
                }
                break;
            case NotificationManager.IMPORTANCE_HIGH:
            case NotificationManager.IMPORTANCE_MAX:
                if (SoundPreferenceController.hasValidSound(channel)) {
                    summary = mContext.getString(R.string.notification_importance_high);
                } else {
                    summary = mContext.getString(R.string.notification_importance_high_silent);
                }
                break;
            default:
                return "";
        }

        return summary;
    }
}
