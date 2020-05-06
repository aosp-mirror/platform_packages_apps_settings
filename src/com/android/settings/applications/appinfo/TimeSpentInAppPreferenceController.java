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
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.LiveDataController;
import com.android.settings.overlay.FeatureFactory;

import java.util.List;

/**
 * To Retrieve the time consumption of the application.
 */
public class TimeSpentInAppPreferenceController extends LiveDataController {
    @VisibleForTesting
    static final Intent SEE_TIME_IN_APP_TEMPLATE = new Intent(Settings.ACTION_APP_USAGE_SETTINGS);

    private final PackageManager mPackageManager;
    private final ApplicationFeatureProvider mAppFeatureProvider;
    private Intent mIntent;
    private String mPackageName;

    public TimeSpentInAppPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
        mAppFeatureProvider = FeatureFactory.getFactory(context)
                .getApplicationFeatureProvider(context);
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

    @Override
    protected CharSequence getSummaryTextInBackground() {
        return mAppFeatureProvider.getTimeSpentInApp(mPackageName);
    }

    private boolean isSystemApp(ResolveInfo info) {
        return info != null
                && info.activityInfo != null
                && info.activityInfo.applicationInfo != null
                && (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
}
