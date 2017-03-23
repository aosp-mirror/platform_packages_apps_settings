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

package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.DefaultDialerManager;
import android.telephony.TelephonyManager;

import java.util.List;

public class DefaultPhonePreferenceController extends DefaultAppPreferenceController {

    public DefaultPhonePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        final TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (!tm.isVoiceCapable()) {
            return false;
        }
        final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        final boolean hasUserRestriction =
                um.hasUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS);

        if (hasUserRestriction) {
            return false;
        }
        final List<String> candidates = getCandidates();
        return candidates != null && !candidates.isEmpty();
    }

    @Override
    public String getPreferenceKey() {
        return "default_phone_app";
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        try {
            return new DefaultAppInfo(mPackageManager,
                    mPackageManager.getPackageManager().getApplicationInfo(
                            DefaultDialerManager.getDefaultDialerApplication(mContext, mUserId),
                            0));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private List<String> getCandidates() {
        return DefaultDialerManager.getInstalledDialerApplications(mContext, mUserId);
    }

    public static boolean hasPhonePreference(String pkg, Context context) {
        List<String> dialerPackages =
                DefaultDialerManager.getInstalledDialerApplications(context, UserHandle.myUserId());
        return dialerPackages.contains(pkg);
    }

    public static boolean isPhoneDefault(String pkg, Context context) {
        String def = DefaultDialerManager.getDefaultDialerApplication(context,
                UserHandle.myUserId());
        return def != null && def.equals(pkg);
    }
}
