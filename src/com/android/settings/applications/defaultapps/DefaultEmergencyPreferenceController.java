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
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.util.List;

public class DefaultEmergencyPreferenceController extends DefaultAppPreferenceController {

    private static final boolean DEFAULT_EMERGENCY_APP_IS_CONFIGURABLE = false;

    public static final Intent QUERY_INTENT = new Intent(
            TelephonyManager.ACTION_EMERGENCY_ASSISTANCE);

    public DefaultEmergencyPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return DEFAULT_EMERGENCY_APP_IS_CONFIGURABLE
                && isCapable()
                && mPackageManager.getPackageManager().resolveActivity(QUERY_INTENT, 0) != null;
    }

    @Override
    public String getPreferenceKey() {
        return "default_emergency_app";
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        return null;
    }

    private boolean isCapable() {
        return TelephonyManager.EMERGENCY_ASSISTANCE_ENABLED
                && mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
    }

    public static boolean hasEmergencyPreference(String pkg, Context context) {
        Intent i = new Intent(QUERY_INTENT);
        i.setPackage(pkg);
        final List<ResolveInfo> resolveInfos =
                context.getPackageManager().queryIntentActivities(i, 0);
        return resolveInfos != null && resolveInfos.size() != 0;
    }

    public static boolean isEmergencyDefault(String pkg, Context context) {
        String defaultPackage = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION);
        return defaultPackage != null && defaultPackage.equals(pkg);
    }
}
