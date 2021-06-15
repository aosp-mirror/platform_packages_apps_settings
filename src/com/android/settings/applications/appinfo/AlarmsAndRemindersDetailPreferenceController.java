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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateAlarmsAndRemindersBridge;

/**
 * Preference controller for
 * {@link com.android.settings.applications.appinfo.AlarmsAndRemindersDetails} Settings fragment.
 */
public class AlarmsAndRemindersDetailPreferenceController extends AppInfoPreferenceControllerBase {

    private String mPackageName;

    public AlarmsAndRemindersDetailPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return isCandidate() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getPreferenceSummary());
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AlarmsAndRemindersDetails.class;
    }

    @VisibleForTesting
    CharSequence getPreferenceSummary() {
        return AlarmsAndRemindersDetails.getSummary(mContext, mParent.getAppEntry());
    }

    @VisibleForTesting
    boolean isCandidate() {
        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo == null) {
            return false;
        }
        final AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState appState =
                new AppStateAlarmsAndRemindersBridge(mContext, null, null).createPermissionState(
                        mPackageName, packageInfo.applicationInfo.uid);
        return appState.shouldBeVisible();
    }

    void setPackageName(String packageName) {
        mPackageName = packageName;
    }
}
