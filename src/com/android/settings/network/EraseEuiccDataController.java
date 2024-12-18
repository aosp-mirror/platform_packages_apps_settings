/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.system.ResetDashboardFragment;

/**
 * Controller for erasing Euicc data
 */
public class EraseEuiccDataController extends BasePreferenceController {
    private ResetDashboardFragment mHostFragment;

    private final UserManager mUm;

    public EraseEuiccDataController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUm = context.getSystemService(UserManager.class);
    }

    public void setFragment(ResetDashboardFragment hostFragment) {
        mHostFragment = hostFragment;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        if (SubscriptionUtil.shouldShowRacDialogWhenErasingAllEsims(mContext)) {
            EuiccRacConnectivityDialogFragment.show(mHostFragment);
        } else {
            EraseEuiccDataDialogFragment.show(mHostFragment);
        }
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isAllowedUser = (mUm.isAdminUser() || Utils.isDemoUser(mContext))
                && !MobileNetworkUtils.isMobileNetworkUserRestricted(mContext);
        boolean hasEuiccFeature = mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_EUICC);
        return SubscriptionUtil.isSimHardwareVisible(mContext)
                && isAllowedUser
                && hasEuiccFeature ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }
}
