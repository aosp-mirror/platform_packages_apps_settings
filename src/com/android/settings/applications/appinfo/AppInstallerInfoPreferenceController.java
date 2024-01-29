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
import android.content.Intent;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppStoreUtil;
import com.android.settingslib.applications.AppUtils;

public class AppInstallerInfoPreferenceController extends AppInfoPreferenceControllerBase {

    private String mPackageName;
    private String mInstallerPackage;
    private CharSequence mInstallerLabel;

    public AppInstallerInfoPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        if (AppUtils.isMainlineModule(mContext.getPackageManager(), mPackageName)) {
            return DISABLED_FOR_USER;
        }

        return mInstallerLabel != null ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        final int detailsStringId = AppUtils.isInstant(mParent.getPackageInfo().applicationInfo)
                ? R.string.instant_app_details_summary
                : R.string.app_install_details_summary;
        preference.setSummary(mContext.getString(detailsStringId, mInstallerLabel));

        Intent intent = AppStoreUtil.getAppStoreLink(mContext, mInstallerPackage, mPackageName);
        if (intent != null) {
            preference.setIntent(intent);
        } else {
            preference.setEnabled(false);
        }
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
        mInstallerPackage = AppStoreUtil.getInstallerPackageName(mContext, mPackageName);
        mInstallerLabel = Utils.getApplicationLabel(mContext, mInstallerPackage);
    }
}
