/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.app.NotificationChannel.NEWS_ID;
import static android.app.NotificationChannel.PROMOTIONS_ID;
import static android.app.NotificationChannel.RECS_ID;
import static android.app.NotificationChannel.SOCIAL_MEDIA_ID;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import static com.android.server.notification.Flags.notificationHideUnusedChannels;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.Flags;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BundleListPreferenceController extends NotificationPreferenceController {

    private static final String KEY = "bundles";

    public BundleListPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!Flags.notificationClassification()) {
            return false;
        }
        if (mAppRow == null) {
            return false;
        }
        if (mAppRow.banned || mAppRow.lockedImportance || mAppRow.systemApp) {
            return false;
        }
        return true;
    }

    @Override
    boolean isIncludedInFilter() {
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        PreferenceCategory category = (PreferenceCategory) preference;

        createOrUpdatePrefForChannel(category,
                mBackend.getChannel(mAppRow.pkg, mAppRow.uid, PROMOTIONS_ID));
        createOrUpdatePrefForChannel(category,
                mBackend.getChannel(mAppRow.pkg, mAppRow.uid, RECS_ID));
        createOrUpdatePrefForChannel(category,
                mBackend.getChannel(mAppRow.pkg, mAppRow.uid, SOCIAL_MEDIA_ID));
        createOrUpdatePrefForChannel(category,
                mBackend.getChannel(mAppRow.pkg, mAppRow.uid, NEWS_ID));
    }

    @NonNull
    private void createOrUpdatePrefForChannel(
            @NonNull PreferenceGroup groupPrefGroup, NotificationChannel channel) {
        int preferenceCount = groupPrefGroup.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = groupPrefGroup.getPreference(i);
            if (channel.getId().equals(preference.getKey())) {
                updateSingleChannelPrefs((PrimarySwitchPreference) preference, channel);
                return;
            }
        }
        PrimarySwitchPreference channelPref = new PrimarySwitchPreference(mContext);
        channelPref.setKey(channel.getId());
        updateSingleChannelPrefs(channelPref, channel);
        groupPrefGroup.addPreference(channelPref);
    }

    /** Update the properties of the channel preference with the values from the channel object. */
    private void updateSingleChannelPrefs(@NonNull final PrimarySwitchPreference channelPref,
            @NonNull final NotificationChannel channel) {
        channelPref.setSwitchEnabled(mAdmin == null);
        if (channel.getImportance() > IMPORTANCE_LOW) {
            channelPref.setIcon(getAlertingIcon());
        } else {
            channelPref.setIcon(mContext.getDrawable(R.drawable.empty_icon));
        }
        channelPref.setIconSize(PrimarySwitchPreference.ICON_SIZE_SMALL);
        channelPref.setTitle(channel.getName());
        channelPref.setSummary(NotificationBackend.getSentSummary(
                mContext, mAppRow.sentByChannel.get(channel.getId()), false));
        channelPref.setChecked(channel.getImportance() != IMPORTANCE_NONE);
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mAppRow.uid);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mAppRow.pkg);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
        channelPref.setIntent(new SubSettingLauncher(mContext)
                .setDestination(ChannelNotificationSettings.class.getName())
                .setArguments(channelArgs)
                .setTitleRes(R.string.notification_channel_title)
                .setSourceMetricsCategory(SettingsEnums.NOTIFICATION_APP_NOTIFICATION)
                .toIntent());

        channelPref.setOnPreferenceChangeListener(
                (preference, o) -> {
                    boolean value = (Boolean) o;
                    int importance = value
                            ? Math.max(channel.getOriginalImportance(), IMPORTANCE_LOW)
                            : IMPORTANCE_NONE;
                    channel.setImportance(importance);
                    channel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                    PrimarySwitchPreference channelPref1 = (PrimarySwitchPreference) preference;
                    channelPref1.setIcon(R.drawable.empty_icon);
                    if (channel.getImportance() > IMPORTANCE_LOW) {
                        channelPref1.setIcon(getAlertingIcon());
                    }
                    mBackend.updateChannel(mAppRow.pkg, mAppRow.uid, channel);

                    return true;
                });
    }

    private Drawable getAlertingIcon() {
        Drawable icon = mContext.getDrawable(R.drawable.ic_notifications_alert);
        icon.setTintList(Utils.getColorAccent(mContext));
        return icon;
    }

}
