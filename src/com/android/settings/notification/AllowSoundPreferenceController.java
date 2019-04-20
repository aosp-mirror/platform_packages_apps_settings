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

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import android.app.NotificationChannel;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedSwitchPreference;

public class AllowSoundPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "AllowSoundPrefContr";
    private static final String KEY_IMPORTANCE = "allow_sound";
    private NotificationSettingsBase.ImportanceListener mImportanceListener;

    public AllowSoundPreferenceController(Context context,
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
        return mChannel != null && NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId());

    }

    @Override
    public void updateState(Preference preference) {
        if (mChannel != null) {
            RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
            pref.setDisabledByAdmin(mAdmin);
            pref.setEnabled(!pref.isDisabledByAdmin());
            pref.setChecked(mChannel.getImportance() >= IMPORTANCE_DEFAULT
                    || mChannel.getImportance() == IMPORTANCE_UNSPECIFIED);
        } else { Log.i(TAG, "tried to updatestate on a null channel?!"); }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel != null) {
            final int importance =
                    ((Boolean) newValue ? IMPORTANCE_UNSPECIFIED : IMPORTANCE_LOW);
            mChannel.setImportance(importance);
            mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
            saveChannel();
            mImportanceListener.onImportanceChanged();
        }
        return true;
    }
}
