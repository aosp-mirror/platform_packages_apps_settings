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
import com.android.settings.applications.LayoutPreference;
import com.android.settings.notification.NotificationBackend.TopicRow;
import com.android.settingslib.RestrictedSwitchPreference;

import android.app.NotificationManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService.Ranking;

/** These settings are per topic, so should not be returned in global search results. */
public class TopicNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "TopicNotiSettings";
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

        if (mTopic == null) {
            toastAndFinish();
            return;
        }

        addPreferencesFromResource(R.xml.topic_notification_settings);
        mTopicRow = mBackend.loadTopicRow(mPm, mPkgInfo, mTopic);

        mImportance = (ImportanceSeekBarPreference) findPreference(KEY_IMPORTANCE);
        mImportanceReset = (LayoutPreference) findPreference(KEY_IMPORTANCE_RESET);
        mImportanceTitle = findPreference(KEY_IMPORTANCE_TITLE);
        mPriority = (RestrictedSwitchPreference) findPreference(KEY_BYPASS_DND);
        mSensitive = (RestrictedSwitchPreference) findPreference(KEY_SENSITIVE);

        setupImportancePrefs(mTopicRow.systemApp, mTopicRow.importance);
        setupPriorityPref(mTopicRow.priority);
        setupSensitivePref(mTopicRow.sensitive);

        updateDependents(mTopicRow.importance);
    }

    @Override
    protected void updateDependents(int importance) {
        final boolean lockscreenSecure = new LockPatternUtils(getActivity()).isSecure(
                UserHandle.myUserId());
        final boolean lockscreenNotificationsEnabled = getLockscreenNotificationsEnabled();
        final boolean allowPrivate = getLockscreenAllowPrivateNotifications();

        setVisible(mPriority, checkCanBeVisible(Ranking.IMPORTANCE_DEFAULT, importance)
                && !mDndVisualEffectsSuppressed);
        setVisible(mSensitive, checkCanBeVisible(Ranking.IMPORTANCE_LOW, importance)
                && lockscreenSecure && lockscreenNotificationsEnabled && allowPrivate);
    }

    protected boolean checkCanBeVisible(int minImportanceVisible, int importance) {
        if (importance == Ranking.IMPORTANCE_UNSPECIFIED) {
            return true;
        }
        return importance > minImportanceVisible;
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
