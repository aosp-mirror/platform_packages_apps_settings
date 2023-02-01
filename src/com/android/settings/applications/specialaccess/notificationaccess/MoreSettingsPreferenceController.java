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

package com.android.settings.applications.specialaccess.notificationaccess;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.service.notification.NotificationListenerService;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import java.util.List;

/**
 * Controls link to reach more preference settings inside the app.
 */
public class MoreSettingsPreferenceController extends BasePreferenceController {

    private static final String TAG = "MoreSettingsPrefContr";
    private static final String KEY_MORE_SETTINGS = "more_settings";

    PackageManager mPm;
    String mPackage;
    Intent mIntent = new Intent(NotificationListenerService.ACTION_SETTINGS_HOME);

    public MoreSettingsPreferenceController(Context context) {
        super(context, KEY_MORE_SETTINGS);
    }

    @Override
    public int getAvailabilityStatus() {
        final List<ResolveInfo> resolveInfos = mPm.queryIntentActivities(
                mIntent,
                PackageManager.ResolveInfoFlags.of(0));
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MORE_SETTINGS;
    }

    public MoreSettingsPreferenceController setPackageManager(PackageManager pm) {
        mPm = pm;
        return this;
    }

    public MoreSettingsPreferenceController setPackage(String pkg) {
        mPackage = pkg;
        mIntent.setPackage(mPackage);
        return this;
    }

    public void updateState(Preference preference) {
        preference.setIntent(mIntent);
    }
}
