/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development;

import static android.app.Activity.RESULT_OK;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settingslib.applications.DefaultAppInfo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DevelopmentAppPicker extends DefaultAppPickerFragment {
    public static final String EXTRA_REQUESTING_PERMISSION = "REQUESTING_PERMISSION";
    public static final String EXTRA_DEBUGGABLE = "DEBUGGABLE";
    public static final String EXTRA_SELECTING_APP = "SELECTING_APP";

    private String mPermissionName;
    private boolean mDebuggableOnly;
    private String mSelectingApp;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        mPermissionName = arguments.getString(EXTRA_REQUESTING_PERMISSION);
        mDebuggableOnly = arguments.getBoolean(EXTRA_DEBUGGABLE);
        mSelectingApp = arguments.getString(EXTRA_SELECTING_APP);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVELOPMENT_APP_PICKER;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.development_app_picker;
    }

    @Override
    protected boolean shouldShowItemNone() {
        return true;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        List<DefaultAppInfo> packageInfoList = new ArrayList<DefaultAppInfo>();
        Context context = getContext();
        List<ApplicationInfo> installedApps = mPm.getInstalledApplications(0);
        for (ApplicationInfo ai : installedApps) {
            if (ai.uid == Process.SYSTEM_UID) {
                continue;
            }
            // Filter out apps that are not debuggable if required.
            if (mDebuggableOnly) {
                // On a user build, we only allow debugging of apps that
                // are marked as debuggable, otherwise (for platform development)
                // we allow all apps.
                if ((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0
                        && "user".equals(Build.TYPE)) {
                    continue;
                }
            }

            // Filter out apps that do not request the permission if required.
            if (mPermissionName != null) {
                boolean requestsPermission = false;
                try {
                    PackageInfo pi = mPm.getPackageInfo(ai.packageName,
                            PackageManager.GET_PERMISSIONS);
                    if (pi.requestedPermissions == null) {
                        continue;
                    }
                    for (String requestedPermission : pi.requestedPermissions) {
                        if (requestedPermission.equals(mPermissionName)) {
                            requestsPermission = true;
                            break;
                        }
                    }
                    if (!requestsPermission) {
                        continue;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    continue;
                }
            }
            DefaultAppInfo appInfo = new DefaultAppInfo(context, mPm, UserHandle.myUserId(), ai);
            packageInfoList.add(appInfo);
        }
        Collections.sort(packageInfoList, sLabelComparator);
        return packageInfoList;
    }

    @Override
    protected String getDefaultKey() {
        return mSelectingApp;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        DefaultAppInfo appInfo = (DefaultAppInfo) getCandidate(key);
        Intent intent = new Intent();
        if (appInfo != null && appInfo.packageItemInfo != null) {
            intent.setAction(appInfo.packageItemInfo.packageName);
        }
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }

    private static final Comparator<DefaultAppInfo> sLabelComparator =
            new Comparator<DefaultAppInfo>() {
                public int compare(DefaultAppInfo a, DefaultAppInfo b) {
                    return Collator.getInstance().compare(a.loadLabel(), b.loadLabel());
                }
            };
}
