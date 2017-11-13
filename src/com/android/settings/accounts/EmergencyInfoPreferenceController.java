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
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

public class EmergencyInfoPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_EMERGENCY_INFO = "emergency_info";
    private static final String ACTION_EDIT_EMERGENCY_INFO = "android.settings.EDIT_EMERGENCY_INFO";
    private static final String PACKAGE_NAME_EMERGENCY = "com.android.emergency";

    public EmergencyInfoPreferenceController(Context context) {
        super(context);
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

    public void updateState(Preference preference) {
        UserInfo info = mContext.getSystemService(UserManager.class).getUserInfo(
            UserHandle.myUserId());
        preference.setSummary(mContext.getString(R.string.emergency_info_summary, info.name));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_EMERGENCY_INFO.equals(preference.getKey())) {
            Intent intent = new Intent(ACTION_EDIT_EMERGENCY_INFO);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        Intent intent = new Intent(ACTION_EDIT_EMERGENCY_INFO).setPackage(PACKAGE_NAME_EMERGENCY);
        List<ResolveInfo> infos = mContext.getPackageManager().queryIntentActivities(intent, 0);
        return infos != null && !infos.isEmpty();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_EMERGENCY_INFO;
    }
}
