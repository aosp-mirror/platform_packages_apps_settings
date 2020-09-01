/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.interactacrossprofiles;

import android.content.Context;
import android.content.pm.CrossProfileApps;

import androidx.preference.Preference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase;

public class InteractAcrossProfilesDetailsPreferenceController
        extends AppInfoPreferenceControllerBase {

    private String mPackageName;

    public InteractAcrossProfilesDetailsPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return canUserAttemptToConfigureInteractAcrossProfiles() ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getPreferenceSummary());
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return InteractAcrossProfilesDetails.class;
    }

    private CharSequence getPreferenceSummary() {
        return InteractAcrossProfilesDetails.getPreferenceSummary(mContext, mPackageName);
    }

    private boolean canUserAttemptToConfigureInteractAcrossProfiles() {
        return mContext.getSystemService(CrossProfileApps.class)
                .canUserAttemptToConfigureInteractAcrossProfiles(mPackageName);
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }
}
