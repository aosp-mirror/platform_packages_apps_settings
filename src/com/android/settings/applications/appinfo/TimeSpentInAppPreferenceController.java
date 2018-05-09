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
 * limitations under the License.
 */

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.core.BasePreferenceController;

import java.util.List;

public class TimeSpentInAppPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    static final Intent SEE_TIME_IN_APP_TEMPLATE =
            new Intent("com.android.settings.action.TIME_SPENT_IN_APP");

    private final PackageManager mPackageManager;

    private Intent mIntent;
    private String mPackageName;

    public TimeSpentInAppPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
        mIntent = new Intent(SEE_TIME_IN_APP_TEMPLATE)
                .putExtra(Intent.EXTRA_PACKAGE_NAME, mPackageName);
    }

    @Override
    public int getAvailabilityStatus() {
        if (TextUtils.isEmpty(mPackageName)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        final List<ResolveInfo> resolved = mPackageManager.queryIntentActivities(mIntent,
                0 /* flags */);
        if (resolved == null || resolved.isEmpty()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        for (ResolveInfo info : resolved) {
            if (isSystemApp(info)) {
                return AVAILABLE;
            }
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            pref.setIntent(mIntent);
        }
    }

    private boolean isSystemApp(ResolveInfo info) {
        return info != null
                && info.activityInfo != null
                && info.activityInfo.applicationInfo != null
                && (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
}
