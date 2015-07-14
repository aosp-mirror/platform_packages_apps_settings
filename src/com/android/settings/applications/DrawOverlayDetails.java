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
import android.content.pm.ResolveInfo;
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
import com.android.settings.applications.AppStateOverlayBridge.OverlayState;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.List;

public class DrawOverlayDetails extends AppInfoWithHeader implements OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String KEY_APP_OPS_SETTINGS_PREFS = "app_ops_settings_preference";
    private static final String KEY_APP_OPS_SETTINGS_DESC = "app_ops_settings_description";
    private static final String LOG_TAG = "DrawOverlayDetails";

    private static final int [] APP_OPS_OP_CODE = {
            AppOpsManager.OP_SYSTEM_ALERT_WINDOW
    };

    // Use a bridge to get the overlay details but don't initialize it to connect with all state.
    // TODO: Break out this functionality into its own class.
    private AppStateOverlayBridge mOverlayBridge;
    private AppOpsManager mAppOpsManager;
    private SwitchPreference mSwitchPref;
    private Preference mOverlayPrefs;
    private Preference mOverlayDesc;
    private Intent mSettingsIntent;
    private OverlayState mOverlayState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mOverlayBridge = new AppStateOverlayBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        // find preferences
        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        mSwitchPref = (SwitchPreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);
        mOverlayPrefs = findPreference(KEY_APP_OPS_SETTINGS_PREFS);
        mOverlayDesc = findPreference(KEY_APP_OPS_SETTINGS_DESC);

        // set title/summary for all of them
        getPreferenceScreen().setTitle(R.string.draw_overlay);
        mSwitchPref.setTitle(R.string.permit_draw_overlay);
        mOverlayPrefs.setTitle(R.string.app_overlay_permission_preference);
        mOverlayDesc.setSummary(R.string.allow_overlay_description);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);
        mOverlayPrefs.setOnPreferenceClickListener(this);

        mSettingsIntent = new Intent(Intent.ACTION_MAIN)
                .setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mOverlayPrefs) {
            if (mSettingsIntent != null) {
                try {
                    getActivity().startActivityAsUser(mSettingsIntent, new UserHandle(mUserId));
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "Unable to launch app draw overlay settings " + mSettingsIntent, e);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchPref) {
            if (mOverlayState != null && (Boolean) newValue != mOverlayState.isAllowed()) {
                setCanDrawOverlay(!mOverlayState.isAllowed());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setCanDrawOverlay(boolean newState) {
        mAppOpsManager.setMode(AppOpsManager.OP_SYSTEM_ALERT_WINDOW,
                mPackageInfo.applicationInfo.uid, mPackageName, newState
                ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
        canDrawOverlay(mPackageName);
    }

    private boolean canDrawOverlay(String pkgName) {
        int result = mAppOpsManager.noteOpNoThrow(AppOpsManager.OP_SYSTEM_ALERT_WINDOW,
                mPackageInfo.applicationInfo.uid, pkgName);
        if (result == AppOpsManager.MODE_ALLOWED) {
            return true;
        }

        return false;
    }

    @Override
    protected boolean refreshUi() {
        mOverlayState = mOverlayBridge.getOverlayInfo(mPackageName,
                mPackageInfo.applicationInfo.uid);

        boolean isAllowed = mOverlayState.isAllowed();
        mSwitchPref.setChecked(isAllowed);
        mOverlayPrefs.setEnabled(isAllowed);

        ResolveInfo resolveInfo = mPm.resolveActivityAsUser(mSettingsIntent,
                PackageManager.GET_META_DATA, mUserId);
        if (resolveInfo == null) {
            if (findPreference(KEY_APP_OPS_SETTINGS_PREFS) != null) {
                getPreferenceScreen().removePreference(mOverlayPrefs);
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
        return MetricsLogger.SYSTEM_ALERT_WINDOW_APPS;
    }

    public static CharSequence getSummary(Context context, AppEntry entry) {
        return getSummary(context, entry.info.packageName);
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
            Log.w(TAG, "Package " + pkg + " not found", e);
            return context.getString(R.string.system_alert_window_off);
        }

        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context
                .APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> packageOps = appOpsManager.getPackagesForOps(
                APP_OPS_OP_CODE);
        if (packageOps == null) {
            return context.getString(R.string.system_alert_window_off);
        }

        int uid = isSystem ? 0 : -1;
        for (AppOpsManager.PackageOps packageOp : packageOps) {
            if (pkg.equals(packageOp.getPackageName())) {
                uid = packageOp.getUid();
                break;
            }
        }

        if (uid == -1) {
            return context.getString(R.string.system_alert_window_off);
        }

        int mode = appOpsManager.noteOpNoThrow(AppOpsManager.OP_SYSTEM_ALERT_WINDOW, uid, pkg);
        return context.getString((mode == AppOpsManager.MODE_ALLOWED) ?
                R.string.system_alert_window_on : R.string.system_alert_window_off);
    }
}
