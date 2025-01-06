/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.app.Flags;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

public class PromotedNotificationsPreferenceController extends
        NotificationPreferenceController implements Preference.OnPreferenceChangeListener {
    private static final String KEY_PROMOTED_CATEGORY = "promoted_category";
    protected static final String KEY_PROMOTED_SWITCH = "promoted_switch";

    public PromotedNotificationsPreferenceController(@NonNull Context context,
            @NonNull NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return KEY_PROMOTED_CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        if (!Flags.uiRichOngoing()) {
            return false;
        }
        return super.isAvailable();
    }

    @Override
    boolean isIncludedInFilter() {
        // not a channel-specific preference; only at the app level
        return false;
    }

    /**
     * Updates the state of the promoted notifications switch. Because this controller governs
     * the full PreferenceCategory, we must find the switch preference within the category first.
     */
    public void updateState(@NonNull Preference preference) {
        PreferenceCategory category = (PreferenceCategory) preference;
        RestrictedSwitchPreference pref = category.findPreference(KEY_PROMOTED_SWITCH);

        if (pref != null && mAppRow != null) {
            pref.setDisabledByAdmin(mAdmin);
            pref.setEnabled(!pref.isDisabledByAdmin());
            pref.setChecked(mAppRow.canBePromoted);
            pref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @NonNull Object newValue) {
        final boolean canBePromoted = (Boolean) newValue;
        if (mAppRow != null && mAppRow.canBePromoted != canBePromoted) {
            mAppRow.canBePromoted = canBePromoted;
            mBackend.setCanBePromoted(mAppRow.pkg, mAppRow.uid, canBePromoted);
        }
        return true;
    }
}
