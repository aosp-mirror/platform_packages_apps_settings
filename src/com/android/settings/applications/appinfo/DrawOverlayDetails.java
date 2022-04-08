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
package com.android.settings.applications.appinfo;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateOverlayBridge;
import com.android.settings.applications.AppStateOverlayBridge.OverlayState;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class DrawOverlayDetails extends AppInfoWithHeader implements OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String LOG_TAG = "DrawOverlayDetails";

    // Use a bridge to get the overlay details but don't initialize it to connect with all state.
    // TODO: Break out this functionality into its own class.
    private AppStateOverlayBridge mOverlayBridge;
    private AppOpsManager mAppOpsManager;
    private SwitchPreference mSwitchPref;
    private OverlayState mOverlayState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mOverlayBridge = new AppStateOverlayBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        if (!Utils.isSystemAlertWindowEnabled(context)) {
            mPackageInfo = null;
            return;
        }

        // find preferences
        addPreferencesFromResource(R.xml.draw_overlay_permissions_details);
        mSwitchPref = findPreference(KEY_APP_OPS_SETTINGS_SWITCH);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);
    }

    // Override here so we don't have an empty screen
    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        // if we don't have a package info, show a page saying this is unsupported
        if (mPackageInfo == null) {
            return inflater.inflate(R.layout.manage_applications_apps_unsupported, null);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOverlayBridge.release();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
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
        int logCategory = newState ? SettingsEnums.APP_SPECIAL_PERMISSION_APPDRAW_ALLOW
                : SettingsEnums.APP_SPECIAL_PERMISSION_APPDRAW_DENY;
        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider();
        metricsFeatureProvider.action(
                metricsFeatureProvider.getAttribution(getActivity()),
                logCategory,
                getMetricsCategory(),
                packageName,
                0);
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null) {
            return true;
        }

        mOverlayState = mOverlayBridge.getOverlayInfo(mPackageName,
                mPackageInfo.applicationInfo.uid);

        boolean isAllowed = mOverlayState.isPermissible();
        mSwitchPref.setChecked(isAllowed);
        // you cannot ask a user to grant you a permission you did not have!
        mSwitchPref.setEnabled(mOverlayState.permissionDeclared && mOverlayState.controlEnabled);

        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SYSTEM_ALERT_WINDOW_APPS;
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
                R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed);
    }
}
