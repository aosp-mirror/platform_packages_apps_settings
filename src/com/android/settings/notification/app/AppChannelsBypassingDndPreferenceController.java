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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.core.text.BidiFormatter;
import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Populates the PreferenceCategory with notification channels associated with the given app.
 * Users can allow/disallow notification channels from bypassing DND on a single settings
 * page.
 */
public class AppChannelsBypassingDndPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver {

    private static final String KEY = "zen_mode_bypassing_app_channels_list";
    private static final String ARG_FROM_SETTINGS = "fromSettings";

    private RestrictedSwitchPreference mAllNotificationsToggle;
    private PreferenceCategory mPreferenceCategory;
    private final List<NotificationChannel> mChannels = new ArrayList<>();

    public AppChannelsBypassingDndPreferenceController(
            Context context,
            NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceCategory = screen.findPreference(KEY);

        mAllNotificationsToggle = new RestrictedSwitchPreference(mPreferenceCategory.getContext());
        mAllNotificationsToggle.setTitle(R.string.zen_mode_bypassing_app_channels_toggle_all);
        mAllNotificationsToggle.setDisabledByAdmin(mAdmin);
        mAllNotificationsToggle.setEnabled(
                (mAdmin == null || !mAllNotificationsToggle.isDisabledByAdmin()));
        mAllNotificationsToggle.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference pref) {
                        SwitchPreference preference = (SwitchPreference) pref;
                        final boolean bypassDnd = preference.isChecked();
                        for (NotificationChannel channel : mChannels) {
                            if (showNotification(channel) && isChannelConfigurable(channel)) {
                                channel.setBypassDnd(bypassDnd);
                                channel.lockFields(NotificationChannel.USER_LOCKED_PRIORITY);
                                mBackend.updateChannel(mAppRow.pkg, mAppRow.uid, channel);
                            }
                        }
                        // the 0th index is the mAllNotificationsToggle which allows users to
                        // toggle all notifications from this app to bypass DND
                        for (int i = 1; i < mPreferenceCategory.getPreferenceCount(); i++) {
                            MasterSwitchPreference childPreference =
                                    (MasterSwitchPreference) mPreferenceCategory.getPreference(i);
                            childPreference.setChecked(showNotificationInDnd(mChannels.get(i - 1)));
                        }
                        return true;
                    }
                });

        loadAppChannels();
        super.displayPreference(screen);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return mAppRow != null;
    }

    @Override
    public void updateState(Preference preference) {
        if (mAppRow != null) {
            loadAppChannels();
        }
    }

    private void loadAppChannels() {
        // Load channel settings
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                List<NotificationChannelGroup> mChannelGroupList = mBackend.getGroups(mAppRow.pkg,
                        mAppRow.uid).getList();
                mChannels.clear();
                for (NotificationChannelGroup channelGroup : mChannelGroupList) {
                    for (NotificationChannel channel : channelGroup.getChannels()) {
                        if (!isConversation(channel)) {
                            mChannels.add(channel);
                        }
                    }
                }
                Collections.sort(mChannels, CHANNEL_COMPARATOR);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                if (mContext == null) {
                    return;
                }
                populateList();
            }
        }.execute();
    }

    private void populateList() {
        if (mPreferenceCategory == null) {
            return;
        }

        mPreferenceCategory.removeAll();
        mPreferenceCategory.addPreference(mAllNotificationsToggle);
        for (NotificationChannel channel : mChannels) {
            MasterSwitchPreference channelPreference = new MasterSwitchPreference(mContext);
            channelPreference.setDisabledByAdmin(mAdmin);
            channelPreference.setSwitchEnabled(
                    (mAdmin == null || !channelPreference.isDisabledByAdmin())
                            && isChannelConfigurable(channel)
                            && showNotification(channel));
            channelPreference.setTitle(BidiFormatter.getInstance().unicodeWrap(channel.getName()));
            channelPreference.setChecked(showNotificationInDnd(channel));
            channelPreference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference pref, Object val) {
                            boolean switchOn = (Boolean) val;
                            channel.setBypassDnd(switchOn);
                            channel.lockFields(NotificationChannel.USER_LOCKED_PRIORITY);
                            mBackend.updateChannel(mAppRow.pkg, mAppRow.uid, channel);
                            mAllNotificationsToggle.setChecked(areAllChannelsBypassing());
                            return true;
                        }
                    });

            Bundle channelArgs = new Bundle();
            channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mAppRow.uid);
            channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mAppRow.pkg);
            channelArgs.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
            channelArgs.putBoolean(ARG_FROM_SETTINGS, true);
            channelPreference.setOnPreferenceClickListener(preference -> {
                new SubSettingLauncher(mContext)
                        .setDestination(ChannelNotificationSettings.class.getName())
                        .setArguments(channelArgs)
                        .setUserHandle(UserHandle.of(mAppRow.userId))
                        .setTitleRes(com.android.settings.R.string.notification_channel_title)
                        .setSourceMetricsCategory(SettingsEnums.DND_APPS_BYPASSING)
                        .launch();
                return true;
            });
            mPreferenceCategory.addPreference(channelPreference);
        }
        mAllNotificationsToggle.setChecked(areAllChannelsBypassing());
    }

    private boolean areAllChannelsBypassing() {
        boolean allChannelsBypassing = true;
        for (NotificationChannel channel : mChannels) {
            if (showNotification(channel)) {
                allChannelsBypassing &= showNotificationInDnd(channel);
            }
        }
        return allChannelsBypassing;
    }

    /**
     * Whether notifications from this channel would show if DND were on.
     */
    private boolean showNotificationInDnd(NotificationChannel channel) {
        return channel.canBypassDnd() && showNotification(channel);
    }

    /**
     * Whether notifications from this channel would show if DND weren't on.
     */
    private boolean showNotification(NotificationChannel channel) {
        return channel.getImportance() != IMPORTANCE_NONE;
    }

    /**
     * Whether this notification channel is representing a conversation.
     */
    private boolean isConversation(NotificationChannel channel) {
        return channel.getConversationId() != null && !channel.isDemoted();
    }
}
