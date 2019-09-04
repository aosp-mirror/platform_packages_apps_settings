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

package com.android.settings.applications.managedomainurls;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class InstantAppAccountPreferenceController extends BasePreferenceController {

    private Intent mLaunchIntent;

    public InstantAppAccountPreferenceController(Context context, String key) {
        super(context, key);
        initAppSettingsIntent();
    }

    @Override
    public int getAvailabilityStatus() {
        if (mLaunchIntent == null || WebActionCategoryController.isDisableWebActions(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }
        // TODO: Make this button actually launch the account chooser.
        if (mLaunchIntent != null) {
            mContext.startActivity(mLaunchIntent);
        }
        return true;
    }

    private void initAppSettingsIntent() {
        // Determine whether we should show the instant apps account chooser setting
        ComponentName instantAppSettingsComponent =
                mContext.getPackageManager().getInstantAppResolverSettingsComponent();
        Intent instantAppSettingsIntent = null;
        if (instantAppSettingsComponent != null) {
            instantAppSettingsIntent =
                    new Intent().setComponent(instantAppSettingsComponent);
        }

        if (instantAppSettingsIntent != null) {
            mLaunchIntent = instantAppSettingsIntent;
        }
    }
}
