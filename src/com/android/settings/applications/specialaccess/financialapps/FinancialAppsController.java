/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.applications.specialaccess.financialapps;

import static android.Manifest.permission.SMS_FINANCIAL_TRANSACTIONS;
import static android.Manifest.permission.READ_SMS;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.List;

public class FinancialAppsController extends BasePreferenceController
        implements ApplicationsState.Callbacks {
    private final static String TAG = FinancialAppsController.class.getSimpleName();

    @VisibleForTesting
    PreferenceScreen mRoot;

    public FinancialAppsController(Context context, String key) {
        super(context, key);
    }

    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mRoot = screen;
    }

    @Override
    public void updateState(Preference preference) {
        updateList();
    }

    private void updateList() {
        mRoot.removeAll();

        final PackageManager packageManager = mContext.getPackageManager();
        final AppOpsManager appOpsManager = mContext.getSystemService(AppOpsManager.class);

        final List<PackageInfo> installedPackages =
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        final int numPackages = installedPackages.size();
        for (int i = 0; i < numPackages; i++) {
            final PackageInfo installedPackage = installedPackages.get(i);

            if (installedPackage.requestedPermissions == null) {
                continue;
            }
            final int targetSdk = installedPackage.applicationInfo.targetSdkVersion;
            final String pkgName = installedPackage.packageName;

            if ((targetSdk >= Build.VERSION_CODES.Q
                    && ArrayUtils.contains(installedPackage.requestedPermissions,
                            SMS_FINANCIAL_TRANSACTIONS))
                    || (targetSdk < Build.VERSION_CODES.Q
                    && ArrayUtils.contains(installedPackage.requestedPermissions,
                            READ_SMS))) {
                final SwitchPreference pref = new SwitchPreference(mRoot.getContext());
                pref.setTitle(installedPackage.applicationInfo.loadLabel(packageManager));
                pref.setKey(pkgName);

                pref.setChecked(
                        appOpsManager.checkOp(
                                targetSdk >= Build.VERSION_CODES.Q
                                        ? AppOpsManager.OP_SMS_FINANCIAL_TRANSACTIONS
                                        : AppOpsManager.OP_READ_SMS,
                                installedPackage.applicationInfo.uid,
                                pkgName) == AppOpsManager.MODE_ALLOWED);

                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    final int uid;
                    try {
                        uid = packageManager.getPackageInfo(preference.getKey(), 0)
                                .applicationInfo.uid;
                    } catch (NameNotFoundException e) {
                        Log.e(TAG, "onPreferenceChange: Failed to get uid for "
                                + preference.getKey());
                        return false;
                    }

                    appOpsManager.setMode(
                            targetSdk >= Build.VERSION_CODES.Q
                                    ? AppOpsManager.OP_SMS_FINANCIAL_TRANSACTIONS
                                    : AppOpsManager.OP_READ_SMS,
                            uid,
                            pkgName,
                            (Boolean) newValue ? AppOpsManager.MODE_ALLOWED
                                    : AppOpsManager.MODE_IGNORED);
                    return true;
                });
                mRoot.addPreference(pref);
            }
        }
    }

    @Override
    public void onRunningStateChanged(boolean running) {}

    @Override
    public void onPackageListChanged() {
        updateList();
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {}

    @Override
    public void onPackageIconChanged() {}

    @Override
    public void onPackageSizeChanged(String packageName) {}

    @Override
    public void onAllSizesComputed() {}

    @Override
    public void onLauncherInfoChanged() {}

    @Override
    public void onLoadEntriesCompleted() {}
}
