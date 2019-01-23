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

import android.app.role.RoleManager;
import android.app.role.RoleManagerCallback;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;

public class DefaultEmergencyPicker extends DefaultAppPickerFragment {
    private static final String TAG = "DefaultEmergencyPicker";
    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEFAULT_EMERGENCY_APP_PICKER;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.default_emergency_settings;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        final List<ResolveInfo> infos = mPm.queryIntentActivities(
                DefaultEmergencyPreferenceController.QUERY_INTENT, 0);
        PackageInfo bestMatch = null;
        final Context context = getContext();
        for (ResolveInfo info : infos) {
            try {
                final PackageInfo packageInfo =
                        mPm.getPackageInfo(info.activityInfo.packageName, 0);
                final ApplicationInfo appInfo = packageInfo.applicationInfo;
                candidates.add(new DefaultAppInfo(context, mPm, mUserId, appInfo));
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
        RoleManager roleManager = getContext().getSystemService(RoleManager.class);
        return CollectionUtils.firstOrNull(roleManager.getRoleHolders(RoleManager.ROLE_EMERGENCY));
    }

    @Override
    protected boolean setDefaultKey(String key) {
        final String previousValue = getDefaultKey();

        if (!TextUtils.isEmpty(key) && !TextUtils.equals(key, previousValue)) {
            getContext().getSystemService(RoleManager.class)
                      .addRoleHolderAsUser(
                              RoleManager.ROLE_EMERGENCY, key, 0, Process.myUserHandle(),
                              AsyncTask.THREAD_POOL_EXECUTOR, new RoleManagerCallback() {
                                  @Override
                                  public void onSuccess() {}

                                  @Override
                                  public void onFailure() {
                                      Log.e(TAG, "Failed to set emergency default app.");
                                  }
                              });
            return true;
        }
        return false;
    }

    private boolean isSystemApp(ApplicationInfo info) {
        return info != null && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
}
