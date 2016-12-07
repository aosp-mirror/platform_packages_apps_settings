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
package com.android.settings.network;

import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;

import static android.os.UserHandle.myUserId;
import static android.os.UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS;
import static com.android.settingslib.RestrictedLockUtils.hasBaseUserRestriction;

public class MobileNetworkPreferenceController extends PreferenceController {

    private static final String KEY_MOBILE_NETWORK_SETTINGS = "mobile_network_settings";

    private final UserManager mUserManager;
    private final boolean mIsSecondaryUser;

    public MobileNetworkPreferenceController(Context context) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mIsSecondaryUser = !mUserManager.isAdminUser();
    }

    @Override
    public boolean isAvailable() {
        return !mIsSecondaryUser
                && !Utils.isWifiOnly(mContext)
                && !hasBaseUserRestriction(mContext, DISALLOW_CONFIG_MOBILE_NETWORKS, myUserId());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MOBILE_NETWORK_SETTINGS;
    }
}
