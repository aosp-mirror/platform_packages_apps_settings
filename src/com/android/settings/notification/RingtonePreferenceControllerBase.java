/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.ThreadUtils;

public abstract class RingtonePreferenceControllerBase extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    public RingtonePreferenceControllerBase(Context context) {
        super(context);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ThreadUtils.postOnBackgroundThread(() -> updateSummary(preference));
    }

    private void updateSummary(Preference preference) {
        final Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(
                mContext, getRingtoneType());

        final CharSequence summary;
        try {
            summary = Ringtone.getTitle(
                    mContext, ringtoneUri, false /* followSettingsUri */, true /* allowRemote */);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Error getting ringtone summary.", e);
            return;
        }
        if (summary != null) {
            ThreadUtils.postOnMainThread(() -> preference.setSummary(summary));
        }
    }

    public abstract int getRingtoneType();

}
