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

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.AppStateUsageBridge.UsageState;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

public class UsageAccessDetails extends AppInfoWithHeader implements OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final String KEY_APP_OPS_PREFERENCE_SCREEN = "app_ops_preference_screen";
    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String KEY_APP_OPS_SETTINGS_PREFS = "app_ops_settings_preference";
    private static final String KEY_APP_OPS_SETTINGS_DESC = "app_ops_settings_description";

    // Use a bridge to get the usage stats but don't initialize it to connect with all state.
    // TODO: Break out this functionality into its own class.
    private AppStateUsageBridge mUsageBridge;
    private AppOpsManager mAppOpsManager;
    private SwitchPreference mSwitchPref;
    private Preference mUsagePrefs;
    private Preference mUsageDesc;
    private Intent mSettingsIntent;
    private UsageState mUsageState;
    private DevicePolicyManager mDpm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mUsageBridge = new AppStateUsageBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mDpm = context.getSystemService(DevicePolicyManager.class);

        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        mSwitchPref = (SwitchPreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);
        mUsagePrefs = findPreference(KEY_APP_OPS_SETTINGS_PREFS);
        mUsageDesc = findPreference(KEY_APP_OPS_SETTINGS_DESC);

        getPreferenceScreen().setTitle(R.string.usage_access);
        mSwitchPref.setTitle(R.string.permit_usage_access);
        mUsagePrefs.setTitle(R.string.app_usage_preference);
        mUsageDesc.setSummary(R.string.usage_access_description);

        mSwitchPref.setOnPreferenceChangeListener(this);
        mUsagePrefs.setOnPreferenceClickListener(this);

        mSettingsIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Settings.INTENT_CATEGORY_USAGE_ACCESS_CONFIG)
                .setPackage(mPackageName);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mUsagePrefs) {
            if (mSettingsIntent != null) {
                try {
                    getActivity().startActivityAsUser(mSettingsIntent, new UserHandle(mUserId));
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "Unable to launch app usage access settings " + mSettingsIntent, e);
                }
            }
            return true;
        }
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
                            .setMessage(R.string.work_profile_usage_access_warning)
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

    private void setHasAccess(boolean newState) {
        logSpecialPermissionChange(newState, mPackageName);
        mAppOpsManager.setMode(AppOpsManager.OP_GET_USAGE_STATS, mPackageInfo.applicationInfo.uid,
                mPackageName, newState ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean newState, String packageName) {
        int logCategory = newState ? MetricsEvent.APP_SPECIAL_PERMISSION_USAGE_VIEW_ALLOW
                : MetricsEvent.APP_SPECIAL_PERMISSION_USAGE_VIEW_DENY;
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(),
                logCategory, packageName);
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null) {
            return false;
        }
        mUsageState = mUsageBridge.getUsageInfo(mPackageName,
                mPackageInfo.applicationInfo.uid);

        boolean hasAccess = mUsageState.isPermissible();
        mSwitchPref.setChecked(hasAccess);
        mSwitchPref.setEnabled(mUsageState.permissionDeclared);
        mUsagePrefs.setEnabled(hasAccess);

        ResolveInfo resolveInfo = mPm.resolveActivityAsUser(mSettingsIntent,
                PackageManager.GET_META_DATA, mUserId);
        if (resolveInfo != null) {
            if (findPreference(KEY_APP_OPS_SETTINGS_PREFS) == null) {
                getPreferenceScreen().addPreference(mUsagePrefs);
            }
            Bundle metaData = resolveInfo.activityInfo.metaData;
            mSettingsIntent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name));
            if (metaData != null
                    && metaData.containsKey(Settings.METADATA_USAGE_ACCESS_REASON)) {
                mSwitchPref.setSummary(
                        metaData.getString(Settings.METADATA_USAGE_ACCESS_REASON));
            }
        } else {
            if (findPreference(KEY_APP_OPS_SETTINGS_PREFS) != null) {
                getPreferenceScreen().removePreference(mUsagePrefs);
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
        return MetricsEvent.APPLICATIONS_USAGE_ACCESS_DETAIL;
    }

}
