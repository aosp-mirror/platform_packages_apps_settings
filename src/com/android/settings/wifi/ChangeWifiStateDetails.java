/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.AppStateChangeWifiStateBridge.WifiSettingsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

public class ChangeWifiStateDetails extends AppInfoWithHeader
        implements OnPreferenceChangeListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String LOG_TAG = "ChangeWifiStateDetails";

    private AppStateChangeWifiStateBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private TwoStatePreference mSwitchPref;
    private WifiSettingsState mWifiSettingsState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity();
        mAppBridge = new AppStateChangeWifiStateBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        // find preferences
        addPreferencesFromResource(R.xml.change_wifi_state_details);
        mSwitchPref = (TwoStatePreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);

        // set title/summary for all of them
        mSwitchPref.setTitle(R.string.change_wifi_state_app_detail_switch);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONFIGURE_WIFI;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchPref) {
            if (mWifiSettingsState != null && (Boolean) newValue
                    != mWifiSettingsState.isPermissible()) {
                setCanChangeWifiState(!mWifiSettingsState.isPermissible());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setCanChangeWifiState(boolean newState) {
        logSpecialPermissionChange(newState, mPackageName);
        mAppOpsManager.setMode(AppOpsManager.OP_CHANGE_WIFI_STATE,
                mPackageInfo.applicationInfo.uid, mPackageName, newState
                        ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);
    }

    protected void logSpecialPermissionChange(boolean newState, String packageName) {
        int logCategory = newState ? SettingsEnums.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_ALLOW
                : SettingsEnums.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_DENY;
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(getContext(),
                logCategory, packageName);
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return false;
        }
        mWifiSettingsState = mAppBridge.getWifiSettingsInfo(mPackageName,
                mPackageInfo.applicationInfo.uid);

        boolean canChange = mWifiSettingsState.isPermissible();
        mSwitchPref.setChecked(canChange);
        // you can't ask a user for a permission you didn't even declare!
        mSwitchPref.setEnabled(mWifiSettingsState.permissionDeclared);
        return true;
    }

    public static CharSequence getSummary(Context context, AppEntry entry) {
        WifiSettingsState state;
        if (entry.extraInfo instanceof WifiSettingsState) {
            state = (WifiSettingsState) entry.extraInfo;
        } else if (entry.extraInfo instanceof PermissionState) {
            state = new WifiSettingsState((PermissionState) entry.extraInfo);
        } else {
            state = new AppStateChangeWifiStateBridge(context, null, null).getWifiSettingsInfo(
                    entry.info.packageName, entry.info.uid);
        }
        return getSummary(context, state);
    }

    public static CharSequence getSummary(Context context, WifiSettingsState wifiSettingsState) {
        return context.getString(wifiSettingsState.isPermissible()
                ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed);
    }
}
