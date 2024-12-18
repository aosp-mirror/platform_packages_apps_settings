/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privacy;

import static android.safetylabel.SafetyLabelConstants.SAFETY_LABEL_CHANGE_NOTIFICATIONS_ENABLED;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.DeviceConfig;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

/**
 * PreferenceController which hides the Data Sharing update if safety labels aren't enabled
 * TODO b/264939792: Add tests
 */
public class AppDataSharingUpdatesPreferenceController extends BasePreferenceController {
    public AppDataSharingUpdatesPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);

        Preference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            pref.setIntent(new Intent(Intent.ACTION_REVIEW_APP_DATA_SHARING_UPDATES)
                    .setPackage(mContext.getPackageManager().getPermissionControllerPackageName()));
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return isPrivacySafetyLabelChangeNotificationsEnabled(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private boolean isPrivacySafetyLabelChangeNotificationsEnabled(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                    SAFETY_LABEL_CHANGE_NOTIFICATIONS_ENABLED, true)
                && !packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
                && !packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                && !packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH);
    }
}
