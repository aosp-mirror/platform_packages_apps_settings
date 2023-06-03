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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppLocaleUtil;
import com.android.settings.localepicker.AppLocalePickerActivity;

import java.util.List;

/**
 * A controller to update current locale information of application.
 */
public class AppLocalePreferenceController extends AppInfoPreferenceControllerBase {
    private static final String TAG = AppLocalePreferenceController.class.getSimpleName();

    private final List<ResolveInfo> mListInfos;

    public AppLocalePreferenceController(Context context, String key) {
        super(context, key);
        mListInfos = context.getPackageManager().queryIntentActivities(
                AppLocaleUtil.LAUNCHER_ENTRY_INTENT, PackageManager.GET_META_DATA);
    }

    @Override
    public int getAvailabilityStatus() {
        return canDisplayLocaleUi() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppLocaleDetails.class;
    }

    @Override
    public CharSequence getSummary() {
        return AppLocaleDetails.getSummary(mContext, mParent.getAppEntry());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), mPreferenceKey)) {
            return false;
        }

        if (mParent != null) {
            Intent intent = new Intent(mContext, AppLocalePickerActivity.class);
            intent.setData(Uri.parse("package:" + mParent.getAppEntry().info.packageName));
            mContext.startActivity(intent);
            return true;
        } else {
            Log.d(TAG, "mParent is null");
            return false;
        }
    }

    @VisibleForTesting
    boolean canDisplayLocaleUi() {
        return AppLocaleUtil
                .canDisplayLocaleUi(mContext, mParent.getAppEntry().info.packageName, mListInfos);
    }
}
