/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.security;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

public class MemtagPagePreferenceController extends BasePreferenceController {
    static final String KEY_MEMTAG = "memtag_page";

    public MemtagPagePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return MemtagHelper.getAvailabilityStatus();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfMteIsDisabled(mContext);
        if (admin != null) {
            ((RestrictedPreference) preference).setDisabledByAdmin(admin);
        }
        refreshSummary(preference);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getResources().getString(MemtagHelper.getSummary());
    }
}
