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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateWriteSettingsBridge.WriteSettingsState;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.List;

public class WriteSettingsDetails extends AppInfoWithHeader implements OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final String KEY_APP_OPS_PREFERENCE_SCREEN = "app_ops_preference_screen";
    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String KEY_APP_OPS_SETTINGS_PREFS = "app_ops_settings_preference";
    private static final String KEY_APP_OPS_SETTINGS_DESC = "app_ops_settings_description";
    private static final String LOG_TAG = "WriteSettingsDetails";

    private static final int [] APP_OPS_OP_CODE = {
            AppOpsManager.OP_WRITE_SETTINGS
    };

    // Use a bridge to get the overlay details but don't initialize it to connect with all state.
    // TODO: Break out this functionality into its own class.
    private AppStateWriteSettingsBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private SwitchPreference mSwitchPref;
    private Preference mWriteSettingsPrefs;
    private Preference mWriteSettingsDesc;
    private Intent mSettingsIntent;
    private WriteSettingsState mWriteSettingsState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mAppBridge = new AppStateWriteSettingsBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        mSwitchPref = (SwitchPreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);
        mWriteSettingsPrefs = findPreference(KEY_APP_OPS_SETTINGS_PREFS);
        mWriteSettingsDesc = findPreference(KEY_APP_OPS_SETTINGS_DESC);

        getPreferenceScreen().setTitle(R.string.write_settings);
        mSwitchPref.setTitle(R.string.permit_write_settings);
        mWriteSettingsPrefs.setTitle(R.string.write_settings_preference);
        mWriteSettingsDesc.setSummary(R.string.write_settings_description);

        mSwitchPref.setOnPreferenceChangeListener(this);
        mWriteSettingsPrefs.setOnPreferenceClickListener(this);

        mSettingsIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Settings.INTENT_CATEGORY_USAGE_ACCESS_CONFIG)
                .setPackage(mPackageName);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mWriteSettingsPrefs) {
            if (mSettingsIntent != null) {
                try {
                    getActivity().startActivityAsUser(mSettingsIntent, new UserHandle(mUserId));
                } catch (ActivityNotFoundException e) {
                    Log.w(LOG_TAG, "Unable to launch write system settings " + mSettingsIntent, e);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchPref) {
            if (mWriteSettingsState != null && (Boolean) newValue != mWriteSettingsState
                    .isPermissible()) {
                setCanWriteSettings(!mWriteSettingsState.isPermissible());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setCanWriteSettings(boolean newState) {
        mAppOpsManager.setMode(AppOpsManager.OP_WRITE_SETTINGS,
                mPackageInfo.applicationInfo.uid, mPackageName, newState
                ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
    }

    private boolean canWriteSettings(String pkgName) {
        int result = mAppOpsManager.noteOpNoThrow(AppOpsManager.OP_WRITE_SETTINGS,
                mPackageInfo.applicationInfo.uid, pkgName);
        if (result == AppOpsManager.MODE_ALLOWED) {
            return true;
        }

        return false;
    }

    @Override
    protected boolean refreshUi() {
        mWriteSettingsState = mAppBridge.getWriteSettingsInfo(mPackageName,
                mPackageInfo.applicationInfo.uid);

        boolean canWrite = mWriteSettingsState.isPermissible();
        mSwitchPref.setChecked(canWrite);
        // you can't ask a user for a permission you didn't even declare!
        mSwitchPref.setEnabled(mWriteSettingsState.permissionDeclared);
        mWriteSettingsPrefs.setEnabled(canWrite);
        getPreferenceScreen().removePreference(mWriteSettingsPrefs);

        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SYSTEM_ALERT_WINDOW_APPS;
    }

    public static CharSequence getSummary(Context context, AppEntry entry) {
        if (entry.extraInfo != null) {
            return getSummary(context, new WriteSettingsState((PermissionState)entry
                    .extraInfo));
        }

        // fallback if entry.extrainfo is null - although this should not happen
        return getSummary(context, entry.info.packageName);
    }

    public static CharSequence getSummary(Context context, WriteSettingsState writeSettingsState) {
        return context.getString(writeSettingsState.isPermissible() ? R.string.write_settings_on :
                R.string.write_settings_off);
    }

    public static CharSequence getSummary(Context context, String pkg) {
        // first check if pkg is a system pkg
        boolean isSystem = false;
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(pkg, 0);
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                isSystem = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // pkg doesn't even exist?
            Log.w(LOG_TAG, "Package " + pkg + " not found", e);
            return context.getString(R.string.write_settings_off);
        }

        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context
                .APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> packageOps = appOpsManager.getPackagesForOps(
                APP_OPS_OP_CODE);
        if (packageOps == null) {
            return context.getString(R.string.write_settings_off);
        }

        int uid = isSystem ? 0 : -1;
        for (AppOpsManager.PackageOps packageOp : packageOps) {
            if (pkg.equals(packageOp.getPackageName())) {
                uid = packageOp.getUid();
                break;
            }
        }

        if (uid == -1) {
            return context.getString(R.string.write_settings_off);
        }

        int mode = appOpsManager.noteOpNoThrow(AppOpsManager.OP_WRITE_SETTINGS, uid, pkg);
        return context.getString((mode == AppOpsManager.MODE_ALLOWED) ?
                R.string.write_settings_on : R.string.write_settings_off);
    }
}
