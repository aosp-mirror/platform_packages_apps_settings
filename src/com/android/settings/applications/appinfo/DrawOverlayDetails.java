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

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateOverlayBridge;
import com.android.settings.applications.AppStateOverlayBridge.OverlayState;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

public class DrawOverlayDetails extends AppInfoWithHeader implements OnPreferenceChangeListener,
        OnPreferenceClickListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String LOG_TAG = "DrawOverlayDetails";

    private static final int[] APP_OPS_OP_CODE = {
            AppOpsManager.OP_SYSTEM_ALERT_WINDOW
    };

    // Use a bridge to get the overlay details but don't initialize it to connect with all state.
    // TODO: Break out this functionality into its own class.
    private AppStateOverlayBridge mOverlayBridge;
    private AppOpsManager mAppOpsManager;
    private SwitchPreference mSwitchPref;
    private Intent mSettingsIntent;
    private OverlayState mOverlayState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mOverlayBridge = new AppStateOverlayBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        // find preferences
        addPreferencesFromResource(R.xml.draw_overlay_permissions_details);
        mSwitchPref = (SwitchPreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);

        mSettingsIntent = new Intent(Intent.ACTION_MAIN)
                .setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getWindow().addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
    }

    @Override
    public void onPause() {
        super.onPause();
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.privateFlags &= ~SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;
        window.setAttributes(attrs);
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

        ResolveInfo resolveInfo = mPm.resolveActivityAsUser(mSettingsIntent,
                PackageManager.GET_META_DATA, mUserId);

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
                R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed);
    }
}
