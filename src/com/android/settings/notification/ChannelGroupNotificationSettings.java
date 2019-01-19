/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.NotificationChannel;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChannelGroupNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "ChannelGroupSettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_CHANNEL_GROUP;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAppRow == null || mChannelGroup == null) {
            Log.w(TAG, "Missing package or uid or packageinfo or group");
            finish();
            return;
        }

        populateChannelList();
        for (NotificationPreferenceController controller : mControllers) {
            controller.onResume(mAppRow, mChannel, mChannelGroup, mSuspendedAppsAdmin);
            controller.displayPreference(getPreferenceScreen());
        }
        updatePreferenceStates();
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.notification_group_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new HeaderPreferenceController(context, this));
        mControllers.add(new BlockPreferenceController(context, mImportanceListener, mBackend));
        mControllers.add(new AppLinkPreferenceController(context));
        mControllers.add(new NotificationsOffPreferenceController(context));
        mControllers.add(new DescriptionPreferenceController(context));
        return new ArrayList<>(mControllers);
    }

    private void populateChannelList() {
        if (!mDynamicPreferences.isEmpty()) {
            // If there's anything in mDynamicPreferences, we've called populateChannelList twice.
            // Clear out existing channels and log.
            Log.w(TAG, "Notification channel group posted twice to settings - old size " +
                    mDynamicPreferences.size() + ", new size " + mDynamicPreferences.size());
            for (Preference p : mDynamicPreferences) {
                getPreferenceScreen().removePreference(p);
            }
        }
        if (mChannelGroup.getChannels().isEmpty()) {
            Preference empty = new Preference(getPrefContext());
            empty.setTitle(R.string.no_channels);
            empty.setEnabled(false);
            getPreferenceScreen().addPreference(empty);
            mDynamicPreferences.add(empty);

        } else {
            final List<NotificationChannel> channels = mChannelGroup.getChannels();
            Collections.sort(channels, mChannelComparator);
            for (NotificationChannel channel : channels) {
                mDynamicPreferences.add(populateSingleChannelPrefs(
                        getPreferenceScreen(), channel, mChannelGroup.isBlocked()));
            }

        }
    }
}
