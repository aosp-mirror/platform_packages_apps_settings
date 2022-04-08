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
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.internal.telephony.CellBroadcastUtils;
import com.android.settings.accounts.AccountRestrictionHelper;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Base class for preference controller that handles preference that enforce adjust volume
 * restriction
 */
public class EmergencyBroadcastPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private final String mPrefKey;

    private AccountRestrictionHelper mHelper;
    private UserManager mUserManager;
    private PackageManager mPm;

    public EmergencyBroadcastPreferenceController(Context context, String prefKey) {
        this(context, new AccountRestrictionHelper(context), prefKey);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    EmergencyBroadcastPreferenceController(Context context, AccountRestrictionHelper helper,
            String prefKey) {
        super(context);
        mPrefKey = prefKey;
        mHelper = helper;
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPm = mContext.getPackageManager();
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedPreference)) {
            return;
        }
        ((RestrictedPreference) preference).checkRestrictionAndSetDisabled(
                UserManager.DISALLOW_CONFIG_CELL_BROADCASTS);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return mPrefKey;
    }

    @Override
    public boolean isAvailable() {
        return mUserManager.isAdminUser() && isCellBroadcastAppLinkEnabled()
                && !mHelper.hasBaseUserRestriction(
                UserManager.DISALLOW_CONFIG_CELL_BROADCASTS, UserHandle.myUserId());
    }

    private boolean isCellBroadcastAppLinkEnabled() {
        // Enable link to CMAS app settings depending on the value in config.xml.
        boolean enabled = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        if (enabled) {
            try {
                String packageName = CellBroadcastUtils
                        .getDefaultCellBroadcastReceiverPackageName(mContext);
                if (packageName == null || mPm.getApplicationEnabledSetting(packageName)
                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    enabled = false;  // CMAS app disabled
                }
            } catch (IllegalArgumentException ignored) {
                enabled = false;  // CMAS app not installed
            }
        }
        return enabled;
    }

}