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

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.notification.NotificationBackend.TopicRow;
import com.android.settingslib.RestrictedSwitchPreference;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationListenerService.Ranking;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

/** These settings are per topic, so should not be returned in global search results. */
public class TopicNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "TopicNotiSettings";

    protected static final String ARG_TOPIC = "arg_topic";

    private TopicRow mTopicRow;
    private boolean mDndVisualEffectsSuppressed;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mTopicRow == null) return;
        AppHeader.createAppHeader(
                this, mTopicRow.icon, mTopicRow.label, mTopicRow.pkg, mTopicRow.uid);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_TOPIC_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager.Policy policy =
                NotificationManager.from(mContext).getNotificationPolicy();
        mDndVisualEffectsSuppressed = policy == null ? false : policy.suppressedVisualEffects != 0;

        Bundle args = getArguments();
        final Notification.Topic topic = args != null && args.containsKey(ARG_TOPIC)
                ? (Notification.Topic) args.getParcelable(ARG_TOPIC) : null;
        if (topic == null) {
            toastAndFinish();
            return;
        }

        addPreferencesFromResource(R.xml.topic_notification_settings);
        mTopicRow = mBackend.loadTopicRow(mPm, mPkgInfo, topic);

        mImportance = (ImportanceSeekBarPreference) findPreference(KEY_IMPORTANCE);
        setupImportancePref(mTopicRow, mTopicRow.topic, mTopicRow.importance);

        mPriority = (RestrictedSwitchPreference) findPreference(KEY_BYPASS_DND);
        setupPriorityPref(mTopicRow.topic, mTopicRow.priority);

        mSensitive = (RestrictedSwitchPreference) findPreference(KEY_SENSITIVE);
        setupSensitivePref(mTopicRow.topic, mTopicRow.sensitive);

        updateDependents(mTopicRow.importance);
    }

    @Override
    protected void updateDependents(int importance) {
        final boolean lockscreenSecure = new LockPatternUtils(getActivity()).isSecure(
                UserHandle.myUserId());
        final boolean lockscreenNotificationsEnabled = getLockscreenNotificationsEnabled();
        final boolean allowPrivate = getLockscreenAllowPrivateNotifications();


        setVisible(mPriority, importance > NotificationListenerService.Ranking.IMPORTANCE_DEFAULT
                && !mDndVisualEffectsSuppressed);
        setVisible(mSensitive, (importance > NotificationListenerService.Ranking.IMPORTANCE_LOW)
                && lockscreenSecure && lockscreenNotificationsEnabled && allowPrivate);
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
    }
}
