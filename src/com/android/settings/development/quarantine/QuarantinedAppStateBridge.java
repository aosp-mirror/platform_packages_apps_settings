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

package com.android.settings.development.quarantine;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;

public class QuarantinedAppStateBridge extends AppStateBaseBridge {
    private Context mContext;

    public QuarantinedAppStateBridge(Context context,
            ApplicationsState appState, Callback callback) {
        super(appState, callback);
        mContext = context;
    }

    @Override
    protected void loadAllExtraInfo() {
        final ArrayList<AppEntry> apps = mAppSession.getAllApps();
        for (int i = 0; i < apps.size(); i++) {
            final AppEntry app = apps.get(i);
            updateExtraInfo(app, app.info.packageName, app.info.uid);
        }
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = isPackageQuarantined(pkg, uid);
    }

    private boolean isPackageQuarantined(String pkg, int uid) {
        final PackageManager pm = mContext.createContextAsUser(
                UserHandle.getUserHandleForUid(uid), 0).getPackageManager();
        try {
            return pm.isPackageQuarantined(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
