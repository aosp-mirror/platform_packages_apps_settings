/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.core.text.BidiFormatter;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.app.ChannelNotificationSettings;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.apppreference.AppPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a preference to the PreferenceScreen for each conversation notification channel that can
 * bypass DND.
 */
public class ZenModeAllBypassingConversationsPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin {

    private final String KEY = "zen_mode_settings_category_conversations";

    @VisibleForTesting ApplicationsState mApplicationsState;
    @VisibleForTesting PreferenceCategory mCategory;
    @VisibleForTesting Context mPrefContext;

    private ApplicationsState.Session mAppSession;
    private NotificationBackend mNotificationBackend = new NotificationBackend();
    private Fragment mHostFragment;

    public ZenModeAllBypassingConversationsPreferenceController(Context context, Application app,
            Fragment host) {

        this(context, app == null ? null : ApplicationsState.getInstance(app), host);
    }

    private ZenModeAllBypassingConversationsPreferenceController(Context context,
            ApplicationsState appState, Fragment host) {
        super(context);
        mApplicationsState = appState;
        mHostFragment = host;

        if (mApplicationsState != null && host != null) {
            mAppSession = mApplicationsState.newSession(mAppSessionCallbacks, host.getLifecycle());
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mCategory = screen.findPreference(KEY);
        mPrefContext = screen.getContext();
        updateNotificationChannelList();
        super.displayPreference(screen);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    /**
     * Call this method to trigger the notification channels list to refresh.
     */
    public void updateNotificationChannelList() {
        if (mAppSession == null) {
            return;
        }

        ApplicationsState.AppFilter filter = ApplicationsState.FILTER_ALL_ENABLED;
        List<ApplicationsState.AppEntry> apps = mAppSession.rebuild(filter,
                ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            updateNotificationChannelList(apps);
        }
    }

    @VisibleForTesting
    void updateNotificationChannelList(List<ApplicationsState.AppEntry> apps) {
        if (mCategory == null || apps == null) {
            return;
        }

        List<Preference> channelsBypassingDnd = new ArrayList<>();
        for (ApplicationsState.AppEntry entry : apps) {
            String pkg = entry.info.packageName;
            mApplicationsState.ensureIcon(entry);
            for (NotificationChannel channel : mNotificationBackend
                    .getNotificationChannelsBypassingDnd(pkg, entry.info.uid).getList()) {
                if (TextUtils.isEmpty(channel.getConversationId())) {
                    // only conversation channels
                    continue;
                }
                Preference pref = new AppPreference(mPrefContext);
                pref.setKey(pkg + "|" + channel.getId());
                pref.setTitle(BidiFormatter.getInstance().unicodeWrap(entry.label));
                // TODO: use badged shortcut icon instead of app icon
                pref.setIcon(entry.icon);
                pref.setSummary(BidiFormatter.getInstance().unicodeWrap(channel.getName()));

                pref.setOnPreferenceClickListener(preference -> {
                    Bundle args = new Bundle();
                    args.putString(AppInfoBase.ARG_PACKAGE_NAME, entry.info.packageName);
                    args.putInt(AppInfoBase.ARG_PACKAGE_UID, entry.info.uid);
                    args.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
                    new SubSettingLauncher(mContext)
                            .setDestination(ChannelNotificationSettings.class.getName())
                            .setArguments(args)
                            .setTitleRes(R.string.notification_channel_title)
                            .setResultListener(mHostFragment, 0)
                            .setSourceMetricsCategory(
                                    SettingsEnums.NOTIFICATION_ZEN_MODE_OVERRIDING_APP)
                            .launch();
                    return true;
                });
                channelsBypassingDnd.add(pref);
            }

            mCategory.removeAll();
            if (channelsBypassingDnd.size() > 0) {
                mCategory.setVisible(true);
                for (Preference prefToAdd : channelsBypassingDnd) {
                    mCategory.addPreference(prefToAdd);
                }
            } else {
                mCategory.setVisible(false);
            }
        }
    }

    private final ApplicationsState.Callbacks mAppSessionCallbacks =
            new ApplicationsState.Callbacks() {

                @Override
                public void onRunningStateChanged(boolean running) {
                    updateNotificationChannelList();
                }

                @Override
                public void onPackageListChanged() {
                    updateNotificationChannelList();
                }

                @Override
                public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                    updateNotificationChannelList(apps);
                }

                @Override
                public void onPackageIconChanged() {
                    updateNotificationChannelList();
                }

                @Override
                public void onPackageSizeChanged(String packageName) {
                    updateNotificationChannelList();
                }

                @Override
                public void onAllSizesComputed() { }

                @Override
                public void onLauncherInfoChanged() {
                    updateNotificationChannelList();
                }

                @Override
                public void onLoadEntriesCompleted() {
                    // Add shortcut info
                    updateNotificationChannelList();
                }
            };
}
