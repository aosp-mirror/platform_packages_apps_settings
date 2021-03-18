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

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;

import com.android.internal.util.ArrayUtils;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.List;
/*
 * Connects info of apps that change wifi state to the ApplicationsState. Wraps around the generic
 * AppStateAppOpsBridge class to tailor to the semantics of CHANGE_WIFI_STATE. Also provides app
 * filters that can use the info.
 */
public class AppStateChangeWifiStateBridge extends AppStateAppOpsBridge {

    private static final String TAG = "AppStateChangeWifiStateBridge";
    private static final int APP_OPS_OP_CODE = AppOpsManager.OP_CHANGE_WIFI_STATE;
    private static final String PM_CHANGE_WIFI_STATE = Manifest.permission.CHANGE_WIFI_STATE;
    private static final String PM_NETWORK_SETTINGS = Manifest.permission.NETWORK_SETTINGS;

    private static final String[] PM_PERMISSIONS = {
            PM_CHANGE_WIFI_STATE
    };

    public AppStateChangeWifiStateBridge(Context context, ApplicationsState appState, Callback
            callback) {
        super(context, appState, callback, APP_OPS_OP_CODE, PM_PERMISSIONS);
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = getWifiSettingsInfo(pkg, uid);
    }

    @Override
    protected void loadAllExtraInfo() {
        final List<AppEntry> allApps = mAppSession.getAllApps();
        for (AppEntry entry : allApps) {
            updateExtraInfo(entry, entry.info.packageName, entry.info.uid);
        }
    }

    public WifiSettingsState getWifiSettingsInfo(String pkg, int uid) {
        PermissionState permissionState = super.getPermissionInfo(pkg, uid);
        return new WifiSettingsState(permissionState);
    }

    public static class WifiSettingsState extends AppStateAppOpsBridge.PermissionState {
        public WifiSettingsState(PermissionState permissionState) {
            super(permissionState.packageName, permissionState.userHandle);
            this.packageInfo = permissionState.packageInfo;
            this.appOpMode = permissionState.appOpMode;
            this.permissionDeclared = permissionState.permissionDeclared;
            this.staticPermissionGranted = permissionState.staticPermissionGranted;
        }
    }

    public static final AppFilter FILTER_CHANGE_WIFI_STATE = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            if (info == null || info.extraInfo == null
                    || !(info.extraInfo instanceof WifiSettingsState)) {
                return false;
            }
            WifiSettingsState wifiSettingsState = (WifiSettingsState) info.extraInfo;
            if (wifiSettingsState.packageInfo != null) {
                final String[] requestedPermissions
                        = wifiSettingsState.packageInfo.requestedPermissions;
                if (ArrayUtils.contains(requestedPermissions, PM_NETWORK_SETTINGS)) {
                    /*
                     * NETWORK_SETTINGS permission trumps CHANGE_WIFI_CONFIG, so remove this from
                     * the list.
                    */
                    return false;
                }
            }
            return wifiSettingsState.permissionDeclared;
        }
    };
}
