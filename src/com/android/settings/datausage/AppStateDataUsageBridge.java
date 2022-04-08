/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.datausage;

import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;

public class AppStateDataUsageBridge extends AppStateBaseBridge {

    private static final String TAG = "AppStateDataUsageBridge";

    private final DataSaverBackend mDataSaverBackend;

    public AppStateDataUsageBridge(ApplicationsState appState, Callback callback,
            DataSaverBackend backend) {
        super(appState, callback);
        mDataSaverBackend = backend;
    }

    @Override
    protected void loadAllExtraInfo() {
        ArrayList<AppEntry> apps = mAppSession.getAllApps();
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppEntry app = apps.get(i);
            app.extraInfo = new DataUsageState(mDataSaverBackend.isWhitelisted(app.info.uid),
                    mDataSaverBackend.isBlacklisted(app.info.uid));
        }
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = new DataUsageState(mDataSaverBackend.isWhitelisted(uid),
                mDataSaverBackend.isBlacklisted(uid));
    }

    public static class DataUsageState {
        public boolean isDataSaverWhitelisted;
        public boolean isDataSaverBlacklisted;

        public DataUsageState(boolean isDataSaverWhitelisted, boolean isDataSaverBlacklisted) {
            this.isDataSaverWhitelisted = isDataSaverWhitelisted;
            this.isDataSaverBlacklisted = isDataSaverBlacklisted;
        }
    }
}
