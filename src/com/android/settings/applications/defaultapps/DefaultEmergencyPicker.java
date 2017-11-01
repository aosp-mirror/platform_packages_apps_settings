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

package com.android.settings.applications.defaultapps;

import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.List;

public class DefaultEmergencyPicker extends DefaultAppPickerFragment {

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEFAULT_EMERGENCY_APP_PICKER;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        final List<ResolveInfo> infos = mPm.getPackageManager().queryIntentActivities(
                DefaultEmergencyPreferenceController.QUERY_INTENT, 0);
        PackageInfo bestMatch = null;
        for (ResolveInfo info : infos) {
            try {
                final PackageInfo packageInfo =
                        mPm.getPackageManager().getPackageInfo(info.activityInfo.packageName, 0);
                final ApplicationInfo appInfo = packageInfo.applicationInfo;
                candidates.add(new DefaultAppInfo(mPm, appInfo));
                // Get earliest installed system app.
                if (isSystemApp(appInfo) && (bestMatch == null ||
                        bestMatch.firstInstallTime > packageInfo.firstInstallTime)) {
                    bestMatch = packageInfo;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Skip unknown packages.
            }
            if (bestMatch != null) {
                final String defaultKey = getDefaultKey();
                if (TextUtils.isEmpty(defaultKey)) {
                    setDefaultKey(bestMatch.packageName);
                }
            }
        }
        return candidates;
    }

    @Override
    protected String getConfirmationMessage(CandidateInfo info) {
        return Utils.isPackageDirectBootAware(getContext(), info.getKey()) ? null
                : getContext().getString(R.string.direct_boot_unaware_dialog_message);
    }

    @Override
    protected String getDefaultKey() {
        return Settings.Secure.getString(getContext().getContentResolver(),
                Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        final ContentResolver contentResolver = getContext().getContentResolver();
        final String previousValue = Settings.Secure.getString(contentResolver,
                Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION);

        if (!TextUtils.isEmpty(key) && !TextUtils.equals(key, previousValue)) {
            Settings.Secure.putString(contentResolver,
                    Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION,
                    key);
            return true;
        }
        return false;
    }

    private boolean isSystemApp(ApplicationInfo info) {
        return info != null && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
}
