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

package com.android.settings.datetime;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

public class AutoTimePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_AUTO_TIME = "auto_time";
    private final UpdateTimeAndDateCallback mCallback;

    public AutoTimePreferenceController(Context context, UpdateTimeAndDateCallback callback) {
        super(context);
        mCallback = callback;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedSwitchPreference)) {
            return;
        }
        if (!((RestrictedSwitchPreference) preference).isDisabledByAdmin()) {
            ((RestrictedSwitchPreference) preference).setDisabledByAdmin(
                    getEnforcedAdminProperty());
        }
        ((RestrictedSwitchPreference) preference).setChecked(isEnabled());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_TIME;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean autoEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME,
                autoEnabled ? 1 : 0);
        mCallback.updateTimeAndDateDisplay(mContext);
        return true;
    }

    public boolean isEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME, 0) > 0;
    }

    private RestrictedLockUtils.EnforcedAdmin getEnforcedAdminProperty() {
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_DATE_TIME,
                UserHandle.myUserId());
    }
}
