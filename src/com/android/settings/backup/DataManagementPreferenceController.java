/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.backup;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class DataManagementPreferenceController extends BasePreferenceController {
    private PrivacySettingsConfigData mPSCD;

    public DataManagementPreferenceController(Context context, String key) {
        super(context, key);
        mPSCD = PrivacySettingsConfigData.getInstance();
    }

    @Override
    public int getAvailabilityStatus() {
        if (!PrivacySettingsUtils.isAdminUser(mContext)) {
            return DISABLED_FOR_USER;
        }
        boolean manageEnabled = (mPSCD.getManageIntent() != null) && mPSCD.isBackupEnabled();
        if (!manageEnabled) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (!isAvailable()) {
            return;
        }
        preference.setIntent(mPSCD.getManageIntent());
        final CharSequence manageLabel = mPSCD.getManageLabel();
        if (manageLabel != null) {
            preference.setTitle(manageLabel);
        }
    }
}