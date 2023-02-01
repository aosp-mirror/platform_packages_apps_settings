/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.testutils;

import static android.provider.Settings.ACTION_BEDTIME_SETTINGS;
import static android.util.FeatureFlagUtils.SETTINGS_APP_ALLOW_DARK_THEME_ACTIVATION_AT_BEDTIME;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.util.FeatureFlagUtils;

/** A helper class for installing bedtime settings activity. */
public final class BedtimeSettingsUtils {
    private Context mContext;

    public BedtimeSettingsUtils(Context context) {
        mContext = context;
    }

    public void installBedtimeSettings(String wellbeingPackage, boolean enabled) {
        FeatureFlagUtils.setEnabled(mContext, SETTINGS_APP_ALLOW_DARK_THEME_ACTIVATION_AT_BEDTIME,
                true /* enabled */);
        Intent bedtimeSettingsIntent = new Intent(ACTION_BEDTIME_SETTINGS)
                .setPackage(wellbeingPackage);
        ResolveInfo bedtimeResolveInfo = new ResolveInfo();
        bedtimeResolveInfo.activityInfo = new ActivityInfo();
        bedtimeResolveInfo.activityInfo.name = "BedtimeSettings";
        bedtimeResolveInfo.activityInfo.packageName = "wellbeing";
        bedtimeResolveInfo.activityInfo.enabled = enabled;
        bedtimeResolveInfo.activityInfo.applicationInfo = new ApplicationInfo();
        bedtimeResolveInfo.activityInfo.applicationInfo.enabled = true;
        shadowOf(mContext.getPackageManager()).addResolveInfoForIntent(
                bedtimeSettingsIntent, bedtimeResolveInfo);
    }
}
