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

import static android.provider.Settings.Secure.NOTIFICATION_BADGING;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

public class BadgePreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "BadgePrefContr";
    private static final String KEY_BADGE = "badge";
    private static final int SYSTEM_WIDE_ON = 1;
    private static final int SYSTEM_WIDE_OFF = 0;

    public BadgePreferenceController(Context context,
            NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BADGE;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mAppRow == null && mChannel == null) {
            return false;
        }
        if (Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_BADGING, SYSTEM_WIDE_ON) == SYSTEM_WIDE_OFF) {
            return false;
        }
        if (mChannel != null) {
            if (isDefaultChannel()) {
                return true;
            } else {
                return mAppRow == null ? false : mAppRow.showBadge;
            }
        }
        return true;
    }

    public void updateState(Preference preference) {
        if (mAppRow != null) {
            RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
            pref.setDisabledByAdmin(mAdmin);
            if (mChannel != null) {
                pref.setChecked(mChannel.canShowBadge());
                pref.setEnabled(!pref.isDisabledByAdmin());
            } else {
                pref.setChecked(mAppRow.showBadge);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean showBadge = (Boolean) newValue;
        if (mChannel != null) {
            mChannel.setShowBadge(showBadge);
            saveChannel();
        } else if (mAppRow != null){
            mAppRow.showBadge = showBadge;
            mBackend.setShowBadge(mAppRow.pkg, mAppRow.uid, showBadge);
        }
        return true;
    }

}
