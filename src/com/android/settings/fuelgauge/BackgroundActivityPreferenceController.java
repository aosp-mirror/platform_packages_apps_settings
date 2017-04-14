/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.fuelgauge;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;

/**
 * Controller to control whether an app can run in the background
 */
public class BackgroundActivityPreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "BgActivityPrefContr";
    private static final String KEY_BACKGROUND_ACTIVITY = "background_activity";

    private final PackageManager mPackageManager;
    private final AppOpsManager mAppOpsManager;
    private final String[] mPackages;
    private final int mUid;

    private String mTargetPackage;

    public BackgroundActivityPreferenceController(Context context, int uid) {
        super(context);
        mPackageManager = context.getPackageManager();
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mUid = uid;
        mPackages = mPackageManager.getPackagesForUid(mUid);
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, mUid, mTargetPackage);
        if (mode == AppOpsManager.MODE_ERRORED) {
            preference.setEnabled(false);
        } else {
            final boolean checked = mode != AppOpsManager.MODE_IGNORED;
            ((SwitchPreference) preference).setChecked(checked);
        }

        updateSummary(preference);
    }

    @Override
    public boolean isAvailable() {
        if (mPackages == null) {
            return false;
        }
        for (final String packageName : mPackages) {
            if (isLegacyApp(packageName)) {
                mTargetPackage = packageName;
                return true;
            }
        }

        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BACKGROUND_ACTIVITY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean switchOn = (Boolean) newValue;
        mAppOpsManager.setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, mUid, mTargetPackage,
                switchOn ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);

        updateSummary(preference);
        return true;
    }

    @VisibleForTesting
    String getTargetPackage() {
        return mTargetPackage;
    }

    @VisibleForTesting
    boolean isLegacyApp(final String packageName) {
        try {
            ApplicationInfo info = mPackageManager.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA);

            return info.targetSdkVersion < Build.VERSION_CODES.O;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find package: " + packageName, e);
        }

        return false;
    }

    @VisibleForTesting
    void updateSummary(Preference preference) {
        final int mode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, mUid, mTargetPackage);

        if (mode == AppOpsManager.MODE_ERRORED) {
            preference.setSummary(R.string.background_activity_summary_disabled);
        } else {
            final boolean checked = mode != AppOpsManager.MODE_IGNORED;
            preference.setSummary(checked ? R.string.background_activity_summary_on
                    : R.string.background_activity_summary_off);
        }
    }
}
