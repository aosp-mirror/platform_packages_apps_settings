/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.security;

import static android.app.admin.DevicePolicyResources.Strings.Settings.MORE_SECURITY_SETTINGS_WORK_PROFILE_SUMMARY;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.CrossProfileApps;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller to decide the summary of "Advanced settings" option in the
 * Security settings screen.
 */
public class SecurityAdvancedSettingsController extends BasePreferenceController {

    private final CrossProfileApps mCrossProfileApps;
    private final DevicePolicyManager mDevicePolicyManager;

    public SecurityAdvancedSettingsController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mCrossProfileApps = context.getSystemService(CrossProfileApps.class);
        mDevicePolicyManager = context.getSystemService(DevicePolicyManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return isWorkProfilePresent()
                ? mDevicePolicyManager.getResources().getString(
                        MORE_SECURITY_SETTINGS_WORK_PROFILE_SUMMARY,
                    () -> mContext.getResources().getString(
                            R.string.security_advanced_settings_work_profile_settings_summary))
                : mContext.getResources().getString(
                        R.string.security_advanced_settings_no_work_profile_settings_summary);
    }

    private boolean isWorkProfilePresent() {
        return !mCrossProfileApps.getTargetUserProfiles().isEmpty();
    }
}
