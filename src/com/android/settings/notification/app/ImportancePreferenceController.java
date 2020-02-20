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

package com.android.settings.notification.app;

import static android.app.NotificationChannel.USER_LOCKED_SOUND;
import static android.app.NotificationManager.IMPORTANCE_DEFAULT;

import android.app.NotificationChannel;
import android.content.Context;
import android.media.RingtoneManager;
import android.provider.Settings;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;

import androidx.preference.Preference;

public class ImportancePreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener  {

    private static final String KEY_IMPORTANCE = "importance";
    private NotificationSettings.DependentFieldListener mDependentFieldListener;

    public ImportancePreferenceController(Context context,
            NotificationSettings.DependentFieldListener dependentFieldListener,
            NotificationBackend backend) {
        super(context, backend);
        mDependentFieldListener = dependentFieldListener;
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
            preference.setEnabled(mAdmin == null && !mChannel.isImportanceLockedByOEM());
            ImportancePreference pref = (ImportancePreference) preference;
            pref.setConfigurable(!mChannel.isImportanceLockedByOEM());
            pref.setImportance(mChannel.getImportance());
            pref.setDisplayInStatusBar(mBackend.showSilentInStatusBar(mContext.getPackageName()));
            pref.setDisplayOnLockscreen(Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1) == 1);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel != null) {
            final int importance = (Integer) newValue;

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
            mDependentFieldListener.onFieldValueChanged();
        }
        return true;
    }
}
