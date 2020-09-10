/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.display;

import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.bluetooth.RestrictionUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;

public class AdaptiveSleepDetailPreferenceController extends AdaptiveSleepPreferenceController {
    private RestrictionUtils mRestrictionUtils;

    public AdaptiveSleepDetailPreferenceController(Context context, String key,
            RestrictionUtils restrictionUtils) {
        super(context, key);
        mRestrictionUtils = restrictionUtils;
    }

    public AdaptiveSleepDetailPreferenceController(Context context, String key) {
        this(context, key, new RestrictionUtils());
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_adaptive_sleep_available)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final EnforcedAdmin enforcedAdmin = mRestrictionUtils.checkIfRestrictionEnforced(mContext,
                UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT);
        if (enforcedAdmin != null) {
            ((RestrictedSwitchPreference) preference).setDisabledByAdmin(enforcedAdmin);
        } else {
            preference.setEnabled(hasSufficientPermission(mContext.getPackageManager()));
        }
    }
}