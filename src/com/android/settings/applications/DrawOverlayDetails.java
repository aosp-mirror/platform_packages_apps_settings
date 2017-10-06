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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import android.view.Window;
import android.view.WindowManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateOverlayBridge.OverlayState;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

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
    public void onResume() {
        super.onResume();
        getActivity().getWindow().addFlags(
                WindowManager.LayoutParams.PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
    }

    @Override
    public void onPause() {
        getActivity().getWindow().clearFlags(
                WindowManager.LayoutParams.PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOverlayBridge.release();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mOverlayPrefs) {
            if (mSettingsIntent != null) {
                try {
                    getActivity().startActivityAsUser(mSettingsIntent, new UserHandle(mUserId));
                } catch (ActivityNotFoundException e) {
                    Log.w(LOG_TAG, "Unable to launch app draw overlay settings " + mSettingsIntent, e);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchPref) {
            if (mOverlayState != null && (Boolean) newValue != mOverlayState.isPermissible()) {
                setCanDrawOverlay(!mOverlayState.isPermissible());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setCanDrawOverlay(boolean newState) {
        logSpecialPermissionChange(newState, mPackageName);
        mAppOpsManager.setMode(AppOpsManager.OP_SYSTEM_ALERT_WINDOW,
                mPackageInfo.applicationInfo.uid, mPackageName, newState
                ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean newState, String packageName) {
        int logCategory = newState ? MetricsEvent.APP_SPECIAL_PERMISSION_APPDRAW_ALLOW
                : MetricsEvent.APP_SPECIAL_PERMISSION_APPDRAW_DENY;
        FeatureFactory.getFactory(getContext())
                .getMetricsFeatureProvider().action(getContext(), logCategory, packageName);
    }

    @Override
    protected boolean refreshUi() {
        mOverlayState = mOverlayBridge.getOverlayInfo(mPackageName,
                mPackageInfo.applicationInfo.uid);

        boolean isAllowed = mOverlayState.isPermissible();
        mSwitchPref.setChecked(isAllowed);
        // you cannot ask a user to grant you a permission you did not have!
        mSwitchPref.setEnabled(mOverlayState.permissionDeclared && mOverlayState.controlEnabled);
        mOverlayPrefs.setEnabled(isAllowed);
        getPreferenceScreen().removePreference(mOverlayPrefs);

        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SYSTEM_ALERT_WINDOW_APPS;
    }

    public static CharSequence getSummary(Context context, AppEntry entry) {
        OverlayState state;
        if (entry.extraInfo instanceof OverlayState) {
            state = (OverlayState) entry.extraInfo;
        } else if (entry.extraInfo instanceof PermissionState) {
            state = new OverlayState((PermissionState) entry.extraInfo);
        } else {
            state = new AppStateOverlayBridge(context, null, null).getOverlayInfo(
                    entry.info.packageName, entry.info.uid);
        }

        return getSummary(context, state);
    }

    public static CharSequence getSummary(Context context, OverlayState overlayState) {
        return context.getString(overlayState.isPermissible() ?
            R.string.app_permission_summary_allowed : R.string.app_permission_summary_not_allowed);
    }
}
