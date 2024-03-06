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

package com.android.settings.development;

import static com.android.settings.development.DevelopmentOptionsActivityRequestCodes.REQUEST_MOCK_LOCATION_APP;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.List;

public class MockLocationAppPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin, OnActivityResultListener {

    private static final String MOCK_LOCATION_APP_KEY = "mock_location_app";
    private static final int[] MOCK_LOCATION_APP_OPS = new int[]{AppOpsManager.OP_MOCK_LOCATION};

    private final DevelopmentSettingsDashboardFragment mFragment;
    private final AppOpsManager mAppsOpsManager;
    private final PackageManager mPackageManager;

    public MockLocationAppPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);

        mFragment = fragment;
        mAppsOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public String getPreferenceKey() {
        return MOCK_LOCATION_APP_KEY;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        if (Flags.deprecateListActivity()) {
            final Bundle args = new Bundle();
            args.putString(DevelopmentAppPicker.EXTRA_REQUESTING_PERMISSION,
                    Manifest.permission.ACCESS_MOCK_LOCATION);
            final String debugApp = Settings.Global.getString(
                    mContext.getContentResolver(), Settings.Global.DEBUG_APP);
            args.putString(DevelopmentAppPicker.EXTRA_SELECTING_APP, debugApp);
            new SubSettingLauncher(mContext)
                    .setDestination(DevelopmentAppPicker.class.getName())
                    .setSourceMetricsCategory(SettingsEnums.DEVELOPMENT)
                    .setArguments(args)
                    .setTitleRes(com.android.settingslib.R.string.select_application)
                    .setResultListener(mFragment, REQUEST_MOCK_LOCATION_APP)
                    .launch();
        } else {
            final Intent intent = new Intent(mContext, AppPicker.class);
            intent.putExtra(AppPicker.EXTRA_REQUESTIING_PERMISSION,
                    Manifest.permission.ACCESS_MOCK_LOCATION);
            mFragment.startActivityForResult(intent, REQUEST_MOCK_LOCATION_APP);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateMockLocation();
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_MOCK_LOCATION_APP || resultCode != Activity.RESULT_OK) {
            return false;
        }
        writeMockLocation(data.getAction());
        updateMockLocation();
        return true;
    }

    @Override
    public void onDeveloperOptionsDisabled() {
        super.onDeveloperOptionsDisabled();
        removeAllMockLocations();
    }

    private void updateMockLocation() {
        final String mockLocationApp = getCurrentMockLocationApp();

        if (!TextUtils.isEmpty(mockLocationApp)) {
            mPreference.setSummary(
                    mContext.getResources()
                            .getString(com.android.settingslib.R.string.mock_location_app_set,
                                    getAppLabel(mockLocationApp)));
        } else {
            mPreference.setSummary(
                    mContext.getResources()
                            .getString(com.android.settingslib.R.string.mock_location_app_not_set));
        }
    }

    private void writeMockLocation(String mockLocationAppName) {
        removeAllMockLocations();
        // Enable the app op of the new mock location app if such.
        if (!TextUtils.isEmpty(mockLocationAppName)) {
            try {
                final ApplicationInfo ai = mPackageManager.getApplicationInfo(
                        mockLocationAppName, PackageManager.MATCH_DISABLED_COMPONENTS);
                mAppsOpsManager.setMode(AppOpsManager.OP_MOCK_LOCATION, ai.uid,
                        mockLocationAppName, AppOpsManager.MODE_ALLOWED);
            } catch (PackageManager.NameNotFoundException e) {
                /* ignore */
            }
        }
    }

    private String getAppLabel(String mockLocationApp) {
        try {
            final ApplicationInfo ai = mPackageManager.getApplicationInfo(
                    mockLocationApp, PackageManager.MATCH_DISABLED_COMPONENTS);
            final CharSequence appLabel = mPackageManager.getApplicationLabel(ai);
            return appLabel != null ? appLabel.toString() : mockLocationApp;
        } catch (PackageManager.NameNotFoundException e) {
            return mockLocationApp;
        }
    }

    private void removeAllMockLocations() {
        // Disable the app op of the previous mock location app if such.
        final List<AppOpsManager.PackageOps> packageOps = mAppsOpsManager.getPackagesForOps(
                MOCK_LOCATION_APP_OPS);
        if (packageOps == null) {
            return;
        }
        // Should be one but in case we are in a bad state due to use of command line tools.
        for (AppOpsManager.PackageOps packageOp : packageOps) {
            if (packageOp.getOps().get(0).getMode() != AppOpsManager.MODE_ERRORED) {
                removeMockLocationForApp(packageOp.getPackageName());
            }
        }
    }

    private void removeMockLocationForApp(String appName) {
        try {
            final ApplicationInfo ai = mPackageManager.getApplicationInfo(
                    appName, PackageManager.MATCH_DISABLED_COMPONENTS);
            mAppsOpsManager.setMode(AppOpsManager.OP_MOCK_LOCATION, ai.uid,
                    appName, AppOpsManager.MODE_ERRORED);
        } catch (PackageManager.NameNotFoundException e) {
            /* ignore */
        }
    }

    @VisibleForTesting
    String getCurrentMockLocationApp() {
        final List<AppOpsManager.PackageOps> packageOps = mAppsOpsManager.getPackagesForOps(
                MOCK_LOCATION_APP_OPS);
        if (packageOps != null) {
            for (AppOpsManager.PackageOps packageOp : packageOps) {
                if (packageOp.getOps().get(0).getMode() == AppOpsManager.MODE_ALLOWED) {
                    return packageOp.getPackageName();
                }
            }
        }
        return null;
    }
}
