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
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.applications.AppStateUsageBridge.UsageState;

public class UsageAccessDetails extends AppInfoWithHeader implements OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final String KEY_USAGE_SWITCH = "usage_switch";
    private static final String KEY_USAGE_PREFS = "app_usage_preference";

    // Use a bridge to get the usage stats but don't initialize it to connect with all state.
    // TODO: Break out this functionality into its own class.
    private AppStateUsageBridge mUsageBridge;
    private AppOpsManager mAppOpsManager;
    private SwitchPreference mSwitchPref;
    private Preference mUsagePrefs;
    private Intent mSettingsIntent;
    private UsageState mUsageState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mUsageBridge = new AppStateUsageBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        addPreferencesFromResource(R.xml.usage_access_details);
        mSwitchPref = (SwitchPreference) findPreference(KEY_USAGE_SWITCH);
        mUsagePrefs = findPreference(KEY_USAGE_PREFS);

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
            if (mUsageState != null && (Boolean) newValue != mUsageState.hasAccess()) {
                setHasAccess(!mUsageState.hasAccess());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setHasAccess(boolean newState) {
        mAppOpsManager.setMode(AppOpsManager.OP_GET_USAGE_STATS, mPackageInfo.applicationInfo.uid,
                mPackageName, newState ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);
    }

    @Override
    protected boolean refreshUi() {
        mUsageState = mUsageBridge.getUsageInfo(mPackageName,
                mPackageInfo.applicationInfo.uid);

        boolean hasAccess = mUsageState.hasAccess();
        mSwitchPref.setChecked(hasAccess);
        mUsagePrefs.setEnabled(hasAccess);

        ResolveInfo resolveInfo = mPm.resolveActivityAsUser(mSettingsIntent,
                PackageManager.GET_META_DATA, mUserId);
        if (resolveInfo != null) {
            if (findPreference(KEY_USAGE_PREFS) == null) {
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
            if (findPreference(KEY_USAGE_PREFS) != null) {
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
    protected int getMetricsCategory() {
        return InstrumentedFragment.VIEW_CATEGORY_USAGE_ACCESS_DETAIL;
    }

}
