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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.notification.NotificationBackend.AppRow;

import java.util.List;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_BLOCK = "block";
    private static final String KEY_APP_SETTINGS = "app_settings";
    private static final String KEY_CATEGORIES = "categories";

    private static final Intent APP_NOTIFICATION_PREFS_CATEGORY_INTENT
            = new Intent(Intent.ACTION_MAIN)
                .addCategory(Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES);

    private SwitchPreference mBlock;
    private PreferenceCategory mCategories;
    private AppRow mAppRow;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAppRow == null) return;
        AppHeader.createAppHeader(this, mAppRow.icon, mAppRow.label, mAppRow.pkg, mAppRow.uid);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_APP_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.app_notification_settings);
        getPreferenceScreen().setOrderingAsAdded(true);
        mAppRow = mBackend.loadAppRow(mPm, mPkgInfo);

        // load settings intent
        ArrayMap<String, AppRow> rows = new ArrayMap<String, AppRow>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);

        // Add topics
        List<Notification.Topic> topics = mBackend.getTopics(mPkg, mUid);
        if (topics.size() <= 1) {
            setupImportancePref(mAppRow, null, mAppRow.appImportance);
            setupPriorityPref(null, mAppRow.appBypassDnd);
            setupSensitivePref(null, mAppRow.appSensitive);
        } else {
            setupBlockSwitch();
            mCategories = new PreferenceCategory(getPrefContext());
            mCategories.setKey(KEY_CATEGORIES);
            mCategories.setTitle(R.string.notification_topic_categories);
            mCategories.setOrderingAsAdded(true);
            getPreferenceScreen().addPreference(mCategories);
            for (Notification.Topic topic : topics) {
                Preference topicPreference = new Preference(getPrefContext());
                topicPreference.setKey(topic.getId());
                topicPreference.setTitle(topic.getLabel());
                // Create intent for this preference.
                Bundle topicArgs = new Bundle();
                topicArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
                topicArgs.putParcelable(TopicNotificationSettings.ARG_TOPIC, topic);
                topicArgs.putBoolean(AppHeader.EXTRA_HIDE_INFO_BUTTON, true);
                topicArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mPkg);
                topicArgs.putParcelable(TopicNotificationSettings.ARG_PACKAGE_INFO, mPkgInfo);

                Intent topicIntent = Utils.onBuildStartFragmentIntent(getActivity(),
                        TopicNotificationSettings.class.getName(),
                        topicArgs, null, R.string.topic_notifications_title, null, false);
                topicPreference.setIntent(topicIntent);
                mCategories.addPreference(topicPreference);
            }
        }

        if (mAppRow.settingsIntent != null) {
            findPreference(KEY_APP_SETTINGS).setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mContext.startActivity(mAppRow.settingsIntent);
                    return true;
                }
            });
        } else {
            removePreference(KEY_APP_SETTINGS);
        }
    }

    @Override
    protected void updateDependents(int progress) {
        updateDependents(progress == NotificationListenerService.Ranking.IMPORTANCE_NONE);
    }

    private void updateDependents(boolean banned) {
        if (mBlock != null) {
            mBlock.setEnabled(!mAppRow.systemApp);
        }
        if (mCategories != null) {
            setVisible(mCategories, !banned);
        }
    }

    private void setupBlockSwitch() {
        mBlock = new SwitchPreference(getPrefContext());
        mBlock.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean banned = (Boolean) newValue;
                if (banned) {
                    MetricsLogger.action(getActivity(), MetricsLogger.ACTION_BAN_APP_NOTES, mPkg);
                }
                final boolean success =  mBackend.setNotificationsBanned(mPkg, mUid, banned);
                if (success) {
                    updateDependents(banned);
                }
                return success;
            }
        });
        mBlock.setKey(KEY_BLOCK);
        mBlock.setTitle(R.string.app_notification_block_title);
        mBlock.setSummary(R.string.app_notification_block_summary);
        getPreferenceScreen().addPreference(mBlock);
        mBlock.setChecked(mAppRow.banned);
        updateDependents(mAppRow.banned);
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
