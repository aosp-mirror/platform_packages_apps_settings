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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateInstallAppsBridge;

public class ExternalSourceDetailPreferenceController extends AppInfoPreferenceControllerBase {

    private String mPackageName;

    public ExternalSourceDetailPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        if (UserManager.get(mContext).isManagedProfile()) {
            return DISABLED_FOR_USER;
        }
        return isPotentialAppSource() ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getPreferenceSummary());
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return ExternalSourcesDetails.class;
    }

    @VisibleForTesting
    CharSequence getPreferenceSummary() {
        return ExternalSourcesDetails.getPreferenceSummary(mContext, mParent.getAppEntry());
    }

    @VisibleForTesting
    boolean isPotentialAppSource() {
        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo == null) {
            return false;
        }
        AppStateInstallAppsBridge.InstallAppsState appState =
                new AppStateInstallAppsBridge(mContext, null, null).createInstallAppsStateFor(
                        mPackageName, packageInfo.applicationInfo.uid);
        return appState.isPotentialAppSource();
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }
}
