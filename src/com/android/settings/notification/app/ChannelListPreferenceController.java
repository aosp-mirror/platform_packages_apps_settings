/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChannelListPreferenceController extends NotificationPreferenceController {

    private static final String KEY = "channels";
    private static String KEY_GENERAL_CATEGORY = "categories";
    public static final String ARG_FROM_SETTINGS = "fromSettings";

    private List<NotificationChannelGroup> mChannelGroupList;
    private PreferenceCategory mPreference;

    public ChannelListPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (mAppRow == null) {
            return false;
        }
        if (mAppRow.banned) {
            return false;
        }
        if (mChannel != null) {
            if (mBackend.onlyHasDefaultChannel(mAppRow.pkg, mAppRow.uid)
                    || NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = (PreferenceCategory) preference;
        // Load channel settings
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                mChannelGroupList = mBackend.getGroups(mAppRow.pkg, mAppRow.uid).getList();
                Collections.sort(mChannelGroupList, CHANNEL_GROUP_COMPARATOR);
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
        // TODO: if preference has children, compare with newly loaded list
        mPreference.removeAll();

        if (mChannelGroupList.isEmpty()) {
            PreferenceCategory groupCategory = new PreferenceCategory(mContext);
            groupCategory.setTitle(R.string.notification_channels);
            groupCategory.setKey(KEY_GENERAL_CATEGORY);
            mPreference.addPreference(groupCategory);

            Preference empty = new Preference(mContext);
            empty.setTitle(R.string.no_channels);
            empty.setEnabled(false);
            groupCategory.addPreference(empty);
        } else {
            populateGroupList();
        }
    }

    private void populateGroupList() {
        for (NotificationChannelGroup group : mChannelGroupList) {
            PreferenceCategory groupCategory = new PreferenceCategory(mContext);
            groupCategory.setOrderingAsAdded(true);
            mPreference.addPreference(groupCategory);
            if (group.getId() == null) {
                groupCategory.setTitle(R.string.notification_channels_other);
                groupCategory.setKey(KEY_GENERAL_CATEGORY);
            } else {
                groupCategory.setTitle(group.getName());
                groupCategory.setKey(group.getId());
                populateGroupToggle(groupCategory, group);
            }
            if (!group.isBlocked()) {
                final List<NotificationChannel> channels = group.getChannels();
                Collections.sort(channels, CHANNEL_COMPARATOR);
                int N = channels.size();
                for (int i = 0; i < N; i++) {
                    final NotificationChannel channel = channels.get(i);
                    // conversations get their own section
                    if (TextUtils.isEmpty(channel.getConversationId()) || channel.isDemoted()) {
                        populateSingleChannelPrefs(groupCategory, channel, group.isBlocked());
                    }
                }
            }
        }
    }

    protected void populateGroupToggle(final PreferenceGroup parent,
            NotificationChannelGroup group) {
        RestrictedSwitchPreference preference =
                new RestrictedSwitchPreference(mContext);
        preference.setTitle(mContext.getString(
                R.string.notification_switch_label, group.getName()));
        preference.setEnabled(mAdmin == null
                && isChannelGroupBlockable(group));
        preference.setChecked(!group.isBlocked());
        preference.setOnPreferenceClickListener(preference1 -> {
            final boolean allowGroup = ((SwitchPreference) preference1).isChecked();
            group.setBlocked(!allowGroup);
            mBackend.updateChannelGroup(mAppRow.pkg, mAppRow.uid, group);

            onGroupBlockStateChanged(group);
            return true;
        });

        parent.addPreference(preference);
    }

    protected Preference populateSingleChannelPrefs(PreferenceGroup parent,
            final NotificationChannel channel, final boolean groupBlocked) {
        MasterSwitchPreference channelPref = new MasterSwitchPreference(mContext);
        channelPref.setSwitchEnabled(mAdmin == null
                && isChannelBlockable(channel)
                && isChannelConfigurable(channel)
                && !groupBlocked);
        channelPref.setIcon(null);
        if (channel.getImportance() > IMPORTANCE_LOW) {
            channelPref.setIcon(getAlertingIcon());
        }
        channelPref.setIconSize(MasterSwitchPreference.ICON_SIZE_SMALL);
        channelPref.setKey(channel.getId());
        channelPref.setTitle(channel.getName());
        channelPref.setSummary(NotificationBackend.getSentSummary(
                mContext, mAppRow.sentByChannel.get(channel.getId()), false));
        channelPref.setChecked(channel.getImportance() != IMPORTANCE_NONE);
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mAppRow.uid);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mAppRow.pkg);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
        channelArgs.putBoolean(ARG_FROM_SETTINGS, true);
        channelPref.setIntent(new SubSettingLauncher(mContext)
                .setDestination(ChannelNotificationSettings.class.getName())
                .setArguments(channelArgs)
                .setTitleRes(R.string.notification_channel_title)
                .setSourceMetricsCategory(SettingsEnums.NOTIFICATION_APP_NOTIFICATION)
                .toIntent());

        channelPref.setOnPreferenceChangeListener(
                (preference, o) -> {
                    boolean value = (Boolean) o;
                    int importance = value ? channel.getOriginalImportance() : IMPORTANCE_NONE;
                    channel.setImportance(importance);
                    channel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                    MasterSwitchPreference channelPref1 = (MasterSwitchPreference) preference;
                    channelPref1.setIcon(null);
                    if (channel.getImportance() > IMPORTANCE_LOW) {
                        channelPref1.setIcon(getAlertingIcon());
                    }
                    mBackend.updateChannel(mAppRow.pkg, mAppRow.uid, channel);

                    return true;
                });
        if (parent.findPreference(channelPref.getKey()) == null) {
            parent.addPreference(channelPref);
        }
        return channelPref;
    }

    private Drawable getAlertingIcon() {
        Drawable icon = mContext.getDrawable(R.drawable.ic_notifications_alert);
        icon.setTintList(Utils.getColorAccent(mContext));
        return icon;
    }

    protected void onGroupBlockStateChanged(NotificationChannelGroup group) {
        if (group == null) {
            return;
        }
        PreferenceGroup groupGroup = mPreference.findPreference(group.getId());

        if (groupGroup != null) {
            if (group.isBlocked()) {
                List<Preference> toRemove = new ArrayList<>();
                int childCount = groupGroup.getPreferenceCount();
                for (int i = 0; i < childCount; i++) {
                    Preference pref = groupGroup.getPreference(i);
                    if (pref instanceof MasterSwitchPreference) {
                        toRemove.add(pref);
                    }
                }
                for (Preference pref : toRemove) {
                    groupGroup.removePreference(pref);
                }
            } else {
                final List<NotificationChannel> channels = group.getChannels();
                Collections.sort(channels, CHANNEL_COMPARATOR);
                int N = channels.size();
                for (int i = 0; i < N; i++) {
                    final NotificationChannel channel = channels.get(i);
                    populateSingleChannelPrefs(groupGroup, channel, group.isBlocked());
                }
            }
        }
    }
}
