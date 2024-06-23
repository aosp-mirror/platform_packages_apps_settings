/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.content.pm.PackageManager.GET_ACTIVITIES;

import static com.android.settingslib.applications.ApplicationsState.AppEntry;
import static com.android.settingslib.applications.ApplicationsState.AppFilter;

import android.content.Context;
import android.util.Log;

import com.android.settings.Utils;
import com.android.settingslib.applications.ApplicationsState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Filter to display only allowlisted apps on Cloned Apps page.
 */
public class AppStateClonedAppsBridge extends AppStateBaseBridge{

    private static final String TAG = "ClonedAppsBridge";

    private final Context mContext;
    private final List<String> mAllowedApps;
    private List<String> mCloneProfileApps = new ArrayList<>();
    private int mCloneUserId;

    public AppStateClonedAppsBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(appState, callback);
        mContext = context;
        mAllowedApps = Arrays.asList(mContext.getResources()
                .getStringArray(com.android.internal.R.array.cloneable_apps));
    }

    @Override
    protected void loadAllExtraInfo() {
        mCloneUserId = Utils.getCloneUserId(mContext);
        if (mCloneUserId != -1) {
            mCloneProfileApps = mContext.getPackageManager()
                    .getInstalledPackagesAsUser(GET_ACTIVITIES,
                            mCloneUserId).stream().map(x -> x.packageName).toList();
        } else if (!mCloneProfileApps.isEmpty()) {
            // In case we remove clone profile (mCloneUserId becomes -1), the bridge state should
            // reflect the same by setting cloneProfileApps as empty, without building the entire
            // page.
            mCloneProfileApps = new ArrayList<>();
        }

        final List<ApplicationsState.AppEntry> allApps = mAppSession.getAllApps();
        for (int i = 0; i < allApps.size(); i++) {
            ApplicationsState.AppEntry app = allApps.get(i);
            this.updateExtraInfo(app, app.info.packageName, app.info.uid);
        }
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        // Display package if allowlisted but not yet cloned.
        // Or if the app is present in clone profile alongwith being in allowlist.
        if (mAllowedApps.contains(pkg)
                && ((!mCloneProfileApps.contains(pkg) || (app.isClonedProfile())))) {
            app.extraInfo = Boolean.TRUE;
        } else {
            app.extraInfo = Boolean.FALSE;
        }
    }

    public static final AppFilter FILTER_APPS_CLONE =
            new AppFilter() {
                @Override
                public void init() {
                }

                @Override
                public boolean filterApp(AppEntry entry) {
                    if (entry.extraInfo == null) {
                        Log.d(TAG, "[" + entry.info.packageName + "]" + " has No extra info.");
                        return false;
                    }
                    return (Boolean) entry.extraInfo;
                }
            };
}
