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

package com.android.settings.nfc;

import static com.android.settingslib.applications.ApplicationsState.AppEntry;
import static com.android.settingslib.applications.ApplicationsState.AppFilter;

import android.app.ActivityManager;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter to display only in the Tag preference listed Apps on Nfc Tag Apps page.
 */
public class AppStateNfcTagAppsBridge extends AppStateBaseBridge{

    private static final String TAG = "AppStateNfcTagAppsBridge";

    private final Context mContext;
    private final NfcAdapter mNfcAdapter;
    // preference list cache
    private static Map<Integer, Map<String, Boolean>> sList = new HashMap<>();

    public AppStateNfcTagAppsBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(appState, callback);
        mContext = context;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
        if (mNfcAdapter != null && mNfcAdapter.isTagIntentAppPreferenceSupported()) {
            UserManager um = mContext.createContextAsUser(
                    UserHandle.of(ActivityManager.getCurrentUser()), 0)
                    .getSystemService(UserManager.class);
            List<UserHandle> luh = um.getEnabledProfiles();
            for (UserHandle uh : luh) {
                int userId = uh.getIdentifier();
                sList.put(userId, mNfcAdapter.getTagIntentAppPreferenceForUser(userId));
            }
        }
    }

    /**
     * Update the system and cached tag app preference lists.
     */
    public boolean updateApplist(int userId, String pkg, boolean allowed) {
        if (mNfcAdapter.setTagIntentAppPreferenceForUser(
                userId, pkg, allowed) == NfcAdapter.TAG_INTENT_APP_PREF_RESULT_SUCCESS) {
            sList.put(userId, mNfcAdapter.getTagIntentAppPreferenceForUser(userId));
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void loadAllExtraInfo() {
        final List<ApplicationsState.AppEntry> allApps = mAppSession.getAllApps();
        for (int i = 0; i < allApps.size(); i++) {
            ApplicationsState.AppEntry app = allApps.get(i);
            this.updateExtraInfo(app, app.info.packageName, app.info.uid);
        }
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        // Display package if is in the app preference list.
        int userId = UserHandle.getUserId(uid);
        Map<String, Boolean> map = sList.getOrDefault(userId, new HashMap<>());
        if (map.containsKey(pkg)) {
            app.extraInfo = new NfcTagAppState(/* exist */ true, /* allowed */ map.get(pkg));
        } else {
            app.extraInfo = new NfcTagAppState(/* exist */ false, /* allowed */ false);
        }
    }

    /**
     * Class to denote the nfc tag app preference state of the AppEntry
     */
    public static class NfcTagAppState {
        private boolean mIsExisted;
        private boolean mIsAllowed;

        public NfcTagAppState(boolean exist, boolean allowed) {
            mIsExisted = exist;
            mIsAllowed = allowed;
        }

        public boolean isExisted() {
            return mIsExisted;
        }

        public boolean isAllowed() {
            return mIsAllowed;
        }
    }

    public static final AppFilter FILTER_APPS_NFC_TAG =
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
                    NfcTagAppState state = (NfcTagAppState) entry.extraInfo;
                    return state.isExisted();
                }
            };
}
