/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.accounts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.List;

public class EmergencyInfoPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    Intent mIntent;

    public EmergencyInfoPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void updateRawDataToIndex(List<SearchIndexableRaw> rawData) {
        if (isAvailable()) {
            SearchIndexableRaw data = new SearchIndexableRaw(mContext);
            final Resources res = mContext.getResources();
            data.title = res.getString(com.android.settings.R.string.emergency_info_title);
            data.screenTitle = res.getString(com.android.settings.R.string.emergency_info_title);
            rawData.add(data);
        }
    }

    @Override
    public void updateState(Preference preference) {
        UserInfo info = mContext.getSystemService(UserManager.class).getUserInfo(
                UserHandle.myUserId());
        preference.setSummary(mContext.getString(R.string.emergency_info_summary, info.name));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey()) && mIntent != null) {
            mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(mIntent);
            return true;
        }
        return false;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(R.bool.config_show_emergency_info_in_device_info)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        // If the variant of emergency info can not work, we should fallback to AOSP version.
        if (isEmergencyInfoSupported()) {
            return AVAILABLE;
        } else if (isAOSPVersionSupported()) {
            return AVAILABLE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    private boolean isEmergencyInfoSupported() {
        final String packageName = mContext.getResources().getString(
                R.string.config_emergency_package_name);
        final String intentName = mContext.getResources().getString(
                R.string.config_emergency_intent_action);
        mIntent = new Intent(intentName).setPackage(packageName);
        final List<ResolveInfo> infos = mContext.getPackageManager().queryIntentActivities(mIntent,
                0);

        return infos != null && !infos.isEmpty();
    }

    private boolean isAOSPVersionSupported() {
        final String aospPackageName = mContext.getResources().getString(
                R.string.config_aosp_emergency_package_name);
        final String aospIntentName = mContext.getResources().getString(
                R.string.config_aosp_emergency_intent_action);

        mIntent = new Intent(aospIntentName).setPackage(aospPackageName);
        final List<ResolveInfo> infos = mContext.getPackageManager().queryIntentActivities(mIntent,
                0);

        return infos != null && !infos.isEmpty();
    }
}
