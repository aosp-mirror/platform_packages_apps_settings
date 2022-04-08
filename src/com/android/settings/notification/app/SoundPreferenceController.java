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

import static android.media.AudioAttributes.USAGE_ALARM;
import static android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;

public class SoundPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        PreferenceManager.OnActivityResultListener {

    private static final String KEY_SOUND = "ringtone";
    private final SettingsPreferenceFragment mFragment;
    private final NotificationSettings.DependentFieldListener mListener;
    private NotificationSoundPreference mPreference;
    protected static final int CODE = 200;

    public SoundPreferenceController(Context context, SettingsPreferenceFragment hostFragment,
            NotificationSettings.DependentFieldListener dependentFieldListener,
            NotificationBackend backend) {
        super(context, backend);
        mFragment = hostFragment;
        mListener = dependentFieldListener;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SOUND;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mChannel == null) {
            return false;
        }
        return checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT) && !isDefaultChannel();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
    }

    public void updateState(Preference preference) {
        if (mAppRow!= null && mChannel != null) {
            NotificationSoundPreference pref = (NotificationSoundPreference) preference;
            pref.setEnabled(mAdmin == null);
            pref.setRingtone(mChannel.getSound());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel != null) {
            mChannel.setSound((Uri) newValue, mChannel.getAudioAttributes());
            saveChannel();
        }
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_SOUND.equals(preference.getKey()) && mFragment != null) {
            NotificationSoundPreference pref = (NotificationSoundPreference) preference;
            if (mChannel != null && mChannel.getAudioAttributes() != null) {
                if (USAGE_ALARM == mChannel.getAudioAttributes().getUsage()) {
                    pref.setRingtoneType(RingtoneManager.TYPE_ALARM);
                } else if (USAGE_NOTIFICATION_RINGTONE
                        == mChannel.getAudioAttributes().getUsage()) {
                    pref.setRingtoneType(RingtoneManager.TYPE_RINGTONE);
                } else {
                    pref.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
                }
            }
            pref.onPrepareRingtonePickerIntent(pref.getIntent());
            mFragment.startActivityForResult(preference.getIntent(), CODE);
            return true;
        }
        return false;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (CODE == requestCode) {
            if (mPreference != null) {
                mPreference.onActivityResult(requestCode, resultCode, data);
            }
            // the importance hasn't changed, but the importance description might as a result of
            // user's selection.
            mListener.onFieldValueChanged();
            return true;
        }
        return false;
    }

    protected static boolean hasValidSound(NotificationChannel channel) {
        return channel != null
                && channel.getSound() != null && !Uri.EMPTY.equals(channel.getSound());
    }
}
