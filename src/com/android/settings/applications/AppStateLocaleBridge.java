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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;

import com.android.settings.Utils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates a application filter to restrict UI display of applications.
 * This is to avoid users from changing the per apps locale
 * Also provides app filters that can use the info.
 */
public class AppStateLocaleBridge extends AppStateBaseBridge {
    private static final String TAG = AppStateLocaleBridge.class.getSimpleName();

    private final Context mContext;
    private final Map<Integer, AppInfoByProfiles> mUserIdToAppInfoByProfiles = new ArrayMap<>();

    public AppStateLocaleBridge(Context context, ApplicationsState appState,
            Callback callback, UserManager userManager) {
        super(appState, callback);
        mContext = context;
        collectLocaleBridgeInfo(userManager);
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String packageName, int uid) {
        AppInfoByProfiles appInfoByProfiles = getAppInfo(UserHandle.getUserId(uid));

        app.extraInfo = AppLocaleUtil.canDisplayLocaleUi(appInfoByProfiles.mContextAsUser,
                app.info.packageName,
                appInfoByProfiles.mListInfos) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected void loadAllExtraInfo() {
        final List<AppEntry> allApps = mAppSession.getAllApps();
        for (int i = 0; i < allApps.size(); i++) {
            AppEntry app = allApps.get(i);
            AppInfoByProfiles appInfoByProfiles = getAppInfo(UserHandle.getUserId(app.info.uid));

            app.extraInfo = AppLocaleUtil.canDisplayLocaleUi(appInfoByProfiles.mContextAsUser,
                    app.info.packageName,
                    appInfoByProfiles.mListInfos) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    /** For the Settings which shows category of per app's locale. */
    public static final AppFilter FILTER_APPS_LOCALE =
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

    private void collectLocaleBridgeInfo(UserManager userManager) {
        List<Integer> userIds = new ArrayList<>();

        userIds.add(mContext.getUserId());
        int workUserId = Utils.getManagedProfileId(userManager, mContext.getUserId());
        if (workUserId != UserHandle.USER_NULL) {
            userIds.add(workUserId);
        }

        // Separate the app information by profiles.
        for (int userId : userIds) {
            if (!mUserIdToAppInfoByProfiles.containsKey(userId)) {
                mUserIdToAppInfoByProfiles.put(userId, new AppInfoByProfiles(mContext, userId));
            }
        }
    }

    private AppInfoByProfiles getAppInfo(int userId) {
        AppInfoByProfiles info;
        if (mUserIdToAppInfoByProfiles.containsKey(userId)) {
            info = mUserIdToAppInfoByProfiles.get(userId);
        } else {
            info = new AppInfoByProfiles(mContext, userId);
            mUserIdToAppInfoByProfiles.put(userId, info);
        }

        return info;
    }

    /**
     * The app information by profiles.
     */
    private static class AppInfoByProfiles {
        public final Context mContextAsUser;
        public final List<ResolveInfo> mListInfos;

        private AppInfoByProfiles(Context context, int userId) {
            mContextAsUser = context.createContextAsUser(UserHandle.of(userId), 0);
            mListInfos = mContextAsUser.getPackageManager().queryIntentActivities(
                    AppLocaleUtil.LAUNCHER_ENTRY_INTENT, PackageManager.GET_META_DATA);
        }
    }
}
