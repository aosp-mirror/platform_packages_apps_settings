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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;

public class SoundPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        PreferenceManager.OnActivityResultListener {

    private static final String KEY_SOUND = "ringtone";
    private final SettingsPreferenceFragment mFragment;
    private final NotificationSettingsBase.ImportanceListener mListener;
    private NotificationSoundPreference mPreference;
    protected static final int CODE = 200;

    public SoundPreferenceController(Context context, SettingsPreferenceFragment hostFragment,
            NotificationSettingsBase.ImportanceListener importanceListener,
            NotificationBackend backend) {
        super(context, backend);
        mFragment = hostFragment;
        mListener = importanceListener;
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

        mPreference = (NotificationSoundPreference) screen.findPreference(getPreferenceKey());
    }

    public void updateState(Preference preference) {
        if (mAppRow!= null && mChannel != null) {
            NotificationSoundPreference pref = (NotificationSoundPreference) preference;
            pref.setEnabled(mAdmin == null && isChannelConfigurable());
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
            mListener.onImportanceChanged();
            return true;
        }
        return false;
    }

    protected static boolean hasValidSound(NotificationChannel channel) {
        return channel != null
                && channel.getSound() != null && !Uri.EMPTY.equals(channel.getSound());
    }
}
