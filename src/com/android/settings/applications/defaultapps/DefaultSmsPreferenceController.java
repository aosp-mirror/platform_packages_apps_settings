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

import android.content.ComponentName;
import android.content.Context;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.SmsApplication;

import java.util.Collection;

public class DefaultSmsPreferenceController extends DefaultAppPreferenceController {

    public DefaultSmsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        boolean isRestrictedUser = mUserManager.getUserInfo(mUserId).isRestricted();
        TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return !isRestrictedUser && tm.isSmsCapable();
    }

    @Override
    public String getPreferenceKey() {
        return "default_sms_app";
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        final ComponentName app = SmsApplication.getDefaultSmsApplication(mContext, true);
        if (app != null) {
            return new DefaultAppInfo(mPackageManager, mUserId, app);
        }
        return null;
    }

    public static boolean hasSmsPreference(String pkg, Context context) {
        Collection<SmsApplication.SmsApplicationData> smsApplications =
                SmsApplication.getApplicationCollection(context);
        for (SmsApplication.SmsApplicationData data : smsApplications) {
            if (data.mPackageName.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSmsDefault(String pkg, Context context) {
        ComponentName appName = SmsApplication.getDefaultSmsApplication(context, true);
        return appName != null && appName.getPackageName().equals(pkg);
    }
}
