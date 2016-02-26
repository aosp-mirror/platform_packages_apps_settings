/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService.Ranking;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.RestrictedPreference;


import java.util.List;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final Intent APP_NOTIFICATION_PREFS_CATEGORY_INTENT
            = new Intent(Intent.ACTION_MAIN)
                .addCategory(Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES);

    private AppRow mAppRow;
    private boolean mDndVisualEffectsSuppressed;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAppRow == null) return;
        AppHeader.createAppHeader(this, mAppRow.icon, mAppRow.label, mAppRow.pkg, mAppRow.uid,
                mAppRow.settingsIntent);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_APP_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.app_notification_settings);
        mImportance = (ImportanceSeekBarPreference) findPreference(KEY_IMPORTANCE);
        mImportanceReset = (LayoutPreference) findPreference(KEY_IMPORTANCE_RESET);
        mImportanceTitle = (RestrictedPreference) findPreference(KEY_IMPORTANCE_TITLE);
        mPriority =
                (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BYPASS_DND);
        mSensitive =
                (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_SENSITIVE);
        mBlock = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BLOCK);
        mSilent = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_SILENT);

        mAppRow = mBackend.loadAppRow(mPm, mPkgInfo);

        NotificationManager.Policy policy =
                NotificationManager.from(mContext).getNotificationPolicy();
        mDndVisualEffectsSuppressed = policy == null ? false : policy.suppressedVisualEffects != 0;

        // load settings intent
        ArrayMap<String, AppRow> rows = new ArrayMap<String, AppRow>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);

        setupImportancePrefs(mAppRow.systemApp, mAppRow.appImportance, mAppRow.banned);
        setupPriorityPref(mAppRow.appBypassDnd);
        setupSensitivePref(mAppRow.appSensitive);
        updateDependents(mAppRow.appImportance);
    }

    @Override
    protected void updateDependents(int importance) {
        final boolean lockscreenSecure = new LockPatternUtils(getActivity()).isSecure(
                UserHandle.myUserId());
        final boolean lockscreenNotificationsEnabled = getLockscreenNotificationsEnabled();
        final boolean allowPrivate = getLockscreenAllowPrivateNotifications();

        if (getPreferenceScreen().findPreference(mBlock.getKey()) != null) {
            setVisible(mSilent, checkCanBeVisible(Ranking.IMPORTANCE_MIN, importance));
            mSilent.setChecked(importance == Ranking.IMPORTANCE_LOW);
        }
        setVisible(mPriority, checkCanBeVisible(Ranking.IMPORTANCE_DEFAULT, importance)
                && !mDndVisualEffectsSuppressed);
        setVisible(mSensitive, checkCanBeVisible(Ranking.IMPORTANCE_MIN, importance)
                && lockscreenSecure && lockscreenNotificationsEnabled && allowPrivate);
    }

    protected boolean checkCanBeVisible(int minImportanceVisible, int importance) {
        if (importance == Ranking.IMPORTANCE_UNSPECIFIED) {
            return true;
        }
        return importance >= minImportanceVisible;
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
    }

    private List<ResolveInfo> queryNotificationConfigActivities() {
        if (DEBUG) Log.d(TAG, "APP_NOTIFICATION_PREFS_CATEGORY_INTENT is "
                + APP_NOTIFICATION_PREFS_CATEGORY_INTENT);
        final List<ResolveInfo> resolveInfos = mPm.queryIntentActivities(
                APP_NOTIFICATION_PREFS_CATEGORY_INTENT,
                0 //PackageManager.MATCH_DEFAULT_ONLY
        );
        return resolveInfos;
    }

    private void collectConfigActivities(ArrayMap<String, AppRow> rows) {
        final List<ResolveInfo> resolveInfos = queryNotificationConfigActivities();
        applyConfigActivities(rows, resolveInfos);
    }

    private void applyConfigActivities(ArrayMap<String, AppRow> rows,
            List<ResolveInfo> resolveInfos) {
        if (DEBUG) Log.d(TAG, "Found " + resolveInfos.size() + " preference activities"
                + (resolveInfos.size() == 0 ? " ;_;" : ""));
        for (ResolveInfo ri : resolveInfos) {
            final ActivityInfo activityInfo = ri.activityInfo;
            final ApplicationInfo appInfo = activityInfo.applicationInfo;
            final AppRow row = rows.get(appInfo.packageName);
            if (row == null) {
                if (DEBUG) Log.v(TAG, "Ignoring notification preference activity ("
                        + activityInfo.name + ") for unknown package "
                        + activityInfo.packageName);
                continue;
            }
            if (row.settingsIntent != null) {
                if (DEBUG) Log.v(TAG, "Ignoring duplicate notification preference activity ("
                        + activityInfo.name + ") for package "
                        + activityInfo.packageName);
                continue;
            }
            row.settingsIntent = new Intent(APP_NOTIFICATION_PREFS_CATEGORY_INTENT)
                    .setClassName(activityInfo.packageName, activityInfo.name);
        }
    }
}
