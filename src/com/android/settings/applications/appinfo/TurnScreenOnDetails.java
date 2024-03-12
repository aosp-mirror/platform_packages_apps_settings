/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_ERRORED;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateTurnScreenOnBridge;
import com.android.settingslib.applications.ApplicationsState;

/**
 * Detail page for turn screen on special app access.
 */
public class TurnScreenOnDetails extends AppInfoWithHeader implements OnPreferenceChangeListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";

    private AppStateTurnScreenOnBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private TwoStatePreference mSwitchPref;
    private AppStateAppOpsBridge.PermissionState mPermissionState;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getActivity();
        mAppBridge = new AppStateTurnScreenOnBridge(context, mState, /*callback=*/ null);
        mAppOpsManager = context.getSystemService(AppOpsManager.class);

        // find preferences
        addPreferencesFromResource(R.xml.turn_screen_on_permissions_details);
        mSwitchPref = (TwoStatePreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);
        mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean checked = (Boolean) newValue;
        if (preference == mSwitchPref) {
            if (mPermissionState != null && checked != mPermissionState.isPermissible()) {
                if (Settings.TurnScreenOnSettingsActivity.class.getName().equals(
                        getIntent().getComponent().getClassName())) {
                    setResult(checked ? RESULT_OK : RESULT_CANCELED);
                }
                setCanTurnScreenOn(checked);
                refreshUi();
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return false;
        }
        mPermissionState = mAppBridge.getPermissionInfo(mPackageName,
                mPackageInfo.applicationInfo.uid);
        mSwitchPref.setEnabled(mPermissionState.permissionDeclared);
        mSwitchPref.setChecked(mPermissionState.isPermissible());
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_MANAGE_TURN_SCREEN_ON;
    }

    /**
     * Sets whether the app associated with the given {@code packageName} is allowed to turn the
     * screen on.
     */
    void setCanTurnScreenOn(boolean newState) {
        mAppOpsManager.setUidMode(AppOpsManager.OPSTR_TURN_SCREEN_ON,
                mPackageInfo.applicationInfo.uid, newState ? MODE_ALLOWED : MODE_ERRORED);
    }

    /**
     * @return the summary for the current state of whether the app associated with the given
     * packageName is allowed to turn the screen on.
     */
    public static int getSummary(Context context, ApplicationsState.AppEntry entry) {
        final AppStateAppOpsBridge.PermissionState state;
        if (entry.extraInfo instanceof AppStateAppOpsBridge.PermissionState) {
            state = (AppStateAppOpsBridge.PermissionState) entry.extraInfo;
        } else {
            state = new AppStateTurnScreenOnBridge(context, /*appState=*/ null,
                    /*callback=*/ null).getPermissionInfo(entry.info.packageName, entry.info.uid);
        }
        return state.isPermissible() ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed;
    }
}
