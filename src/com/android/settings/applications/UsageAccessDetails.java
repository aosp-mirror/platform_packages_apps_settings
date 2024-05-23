/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.applications;

import static android.app.AppOpsManager.OP_GET_USAGE_STATS;
import static android.app.AppOpsManager.OP_LOADER_USAGE_STATS;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_DISABLE_USAGE_ACCESS_WARNING;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;

import com.android.settings.R;
import com.android.settings.applications.AppStateUsageBridge.UsageState;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class UsageAccessDetails extends AppInfoWithHeader implements OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final String TAG = UsageAccessDetails.class.getSimpleName();
    private static final String KEY_APP_OPS_PREFERENCE_SCREEN = "app_ops_preference_screen";
    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String KEY_APP_OPS_SETTINGS_DESC = "app_ops_settings_description";

    // Use a bridge to get the usage stats but don't initialize it to connect with all state.
    // TODO: Break out this functionality into its own class.
    private AppStateUsageBridge mUsageBridge;
    private AppOpsManager mAppOpsManager;
    private RestrictedSwitchPreference mSwitchPref;
    private Preference mUsageDesc;
    private Intent mSettingsIntent;
    private UsageState mUsageState;
    private DevicePolicyManager mDpm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        if (TextUtils.equals(mPackageName, context.getPackageName())) {
            Log.w(TAG, "Unsupported app package.");
            finish();
        }

        mUsageBridge = new AppStateUsageBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mDpm = context.getSystemService(DevicePolicyManager.class);

        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        mSwitchPref = (RestrictedSwitchPreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);
        mUsageDesc = findPreference(KEY_APP_OPS_SETTINGS_DESC);

        getPreferenceScreen().setTitle(R.string.usage_access);
        mSwitchPref.setTitle(R.string.permit_usage_access);
        mUsageDesc.setTitle(R.string.usage_access_description);

        mSwitchPref.setOnPreferenceChangeListener(this);

        mSettingsIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Settings.INTENT_CATEGORY_USAGE_ACCESS_CONFIG)
                .setPackage(mPackageName);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchPref) {
            if (mUsageState != null && (Boolean) newValue != mUsageState.isPermissible()) {
                if (mUsageState.isPermissible() && mDpm.isProfileOwnerApp(mPackageName)) {
                    new AlertDialog.Builder(getContext())
                            .setIcon(com.android.internal.R.drawable.ic_dialog_alert_material)
                            .setTitle(android.R.string.dialog_alert_title)
                            .setMessage(mDpm.getResources().getString(
                                    WORK_PROFILE_DISABLE_USAGE_ACCESS_WARNING,
                                    () -> getString(R.string.work_profile_usage_access_warning)))
                            .setPositiveButton(R.string.okay, null)
                            .show();
                }
                setHasAccess(!mUsageState.isPermissible());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private static boolean doesAnyPermissionMatch(String permissionToMatch, String[] permissions) {
        for (String permission : permissions) {
            if (permissionToMatch.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    private void setHasAccess(boolean newState) {
        logSpecialPermissionChange(newState, mPackageName);

        final int newAppOpMode = newState ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED;
        final int uid = mPackageInfo.applicationInfo.uid;
        if (doesAnyPermissionMatch(Manifest.permission.PACKAGE_USAGE_STATS,
                mUsageState.packageInfo.requestedPermissions)) {
            mAppOpsManager.setMode(OP_GET_USAGE_STATS, uid, mPackageName, newAppOpMode);
        }
        if (doesAnyPermissionMatch(Manifest.permission.LOADER_USAGE_STATS,
                mUsageState.packageInfo.requestedPermissions)) {
            mAppOpsManager.setMode(OP_LOADER_USAGE_STATS, uid, mPackageName, newAppOpMode);
        }
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean newState, String packageName) {
        int logCategory = newState ? SettingsEnums.APP_SPECIAL_PERMISSION_USAGE_VIEW_ALLOW
                : SettingsEnums.APP_SPECIAL_PERMISSION_USAGE_VIEW_DENY;
        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        metricsFeatureProvider.action(
                metricsFeatureProvider.getAttribution(getActivity()),
                logCategory,
                getMetricsCategory(),
                packageName,
                0);
    }

    @Override
    protected boolean refreshUi() {
        retrieveAppEntry();
        if (mAppEntry == null) {
            return false;
        }
        if (mPackageInfo == null) {
            return false; // onCreate must have failed, make sure to exit
        }
        mUsageState = mUsageBridge.getUsageInfo(mPackageName,
                mPackageInfo.applicationInfo.uid);

        boolean hasAccess = mUsageState.isPermissible();
        boolean shouldEnable = mUsageState.permissionDeclared;

        if (shouldEnable && !hasAccess) {
            mSwitchPref.checkEcmRestrictionAndSetDisabled(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    mPackageName);
            shouldEnable = !mSwitchPref.isDisabledByEcm();
        }

        mSwitchPref.setChecked(hasAccess);
        mSwitchPref.setEnabled(shouldEnable);

        ResolveInfo resolveInfo = mPm.resolveActivityAsUser(mSettingsIntent,
                PackageManager.GET_META_DATA, mUserId);
        if (resolveInfo != null) {
            Bundle metaData = resolveInfo.activityInfo.metaData;
            mSettingsIntent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name));
            if (metaData != null
                    && metaData.containsKey(Settings.METADATA_USAGE_ACCESS_REASON)) {
                mSwitchPref.setSummary(
                        metaData.getString(Settings.METADATA_USAGE_ACCESS_REASON));
            }
        }

        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPLICATIONS_USAGE_ACCESS_DETAIL;
    }

}
