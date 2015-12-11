/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.notification.NotificationBackend.TopicRow;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

/** These settings are per topic, so should not be returned in global search results. */
public class TopicNotificationSettings extends SettingsPreferenceFragment {
    private static final String TAG = "TopicNotiSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    protected static final String ARG_TOPIC = "arg_topic";
    protected static final String ARG_PACKAGE_INFO = "arg_info";
    private static final String KEY_BYPASS_DND = "bypass_dnd";
    private static final String KEY_SENSITIVE = "sensitive";
    private static final String KEY_IMPORTANCE = "importance";

    private final NotificationBackend mBackend = new NotificationBackend();

    private Context mContext;
    private ImportanceSeekBarPreference mImportance;
    private SwitchPreference mPriority;
    private SwitchPreference mSensitive;
    private TopicRow mTopicRow;
    private boolean mCreated;
    private boolean mIsSystemPackage;
    private int mUid;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onActivityCreated mCreated=" + mCreated);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
        if (mTopicRow == null) return;
        AppHeader.createAppHeader(
                this, mTopicRow.icon, mTopicRow.label, mTopicRow.pkg, mTopicRow.uid);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_TOPIC_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        if (DEBUG) Log.d(TAG, "onCreate getIntent()=" + intent);
        if (intent == null && args == null) {
            Log.w(TAG, "No intent");
            toastAndFinish();
            return;
        }

        final Notification.Topic topic = args != null && args.containsKey(ARG_TOPIC)
                ? (Notification.Topic) args.getParcelable(ARG_TOPIC) : null;

        if (topic == null) {
            toastAndFinish();
            return;
        }

        final PackageInfo info = args != null && args.containsKey(ARG_PACKAGE_INFO)
                ? (PackageInfo) args.getParcelable(ARG_PACKAGE_INFO) : null;
        if (info == null) {
            Log.w(TAG, "Failed to find package info");
            toastAndFinish();
            return;
        }

        mUid = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_UID)
                ? args.getInt(AppInfoBase.ARG_PACKAGE_UID)
                : intent.getIntExtra(Settings.EXTRA_APP_UID, -1);
        if (mUid == -1) {
            Log.w(TAG, "Missing extras: " + Settings.EXTRA_APP_UID + " was " + mUid);
            toastAndFinish();
            return;
        }

        final PackageManager pm = getPackageManager();
        mIsSystemPackage = Utils.isSystemPackage(pm, info);

        addPreferencesFromResource(R.xml.topic_notification_settings);
        mImportance = (ImportanceSeekBarPreference) findPreference(KEY_IMPORTANCE);
        mPriority = (SwitchPreference) findPreference(KEY_BYPASS_DND);
        mSensitive = (SwitchPreference) findPreference(KEY_SENSITIVE);

        mTopicRow = mBackend.loadTopicRow(pm, info.applicationInfo, topic);

        mImportance.setMax(4);
        // TODO: stop defaulting to 'normal' in the UI when there are mocks for this scenario.
        int importance =
                mTopicRow.importance == NotificationListenerService.Ranking.IMPORTANCE_UNSPECIFIED
                ? NotificationListenerService.Ranking.IMPORTANCE_DEFAULT
                        : mTopicRow.importance;
        mImportance.setProgress(importance);
        mImportance.setCallback(new ImportanceSeekBarPreference.Callback() {
            @Override
            public void onImportanceChanged(int progress) {
                mBackend.setImportance(mTopicRow.pkg, mTopicRow.uid, mTopicRow.topic, progress);
            }
        });
        mPriority.setChecked(mTopicRow.priority);
        mSensitive.setChecked(mTopicRow.sensitive);

        mPriority.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean bypassZenMode = (Boolean) newValue;
                return mBackend.setBypassZenMode(info.packageName, mUid, topic, bypassZenMode);
            }
        });

        mSensitive.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean sensitive = (Boolean) newValue;
                return mBackend.setSensitive(info.packageName, mUid, topic, sensitive);
            }
        });
        updateDependents(mTopicRow.banned);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid != -1 && getPackageManager().getPackagesForUid(mUid) == null) {
            // App isn't around anymore, must have been removed.
            finish();
        }
    }

    private void updateDependents(boolean banned) {
        final boolean lockscreenSecure = new LockPatternUtils(getActivity()).isSecure(
                UserHandle.myUserId());
        final boolean lockscreenNotificationsEnabled = getLockscreenNotificationsEnabled();
        final boolean allowPrivate = getLockscreenAllowPrivateNotifications();

        setVisible(mPriority, mIsSystemPackage || !banned);
        setVisible(mSensitive, mIsSystemPackage || !banned && lockscreenSecure
                && lockscreenNotificationsEnabled && allowPrivate);
        setVisible(mImportance, !banned);
    }

    private void setVisible(Preference p, boolean visible) {
        final boolean isVisible = getPreferenceScreen().findPreference(p.getKey()) != null;
        if (isVisible == visible) return;
        if (visible) {
            getPreferenceScreen().addPreference(p);
        } else {
            getPreferenceScreen().removePreference(p);
        }
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
    }

    private void toastAndFinish() {
        Toast.makeText(mContext, R.string.app_not_found_dlg_text, Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }
}
