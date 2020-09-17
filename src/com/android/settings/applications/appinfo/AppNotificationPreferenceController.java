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

package com.android.settings.applications.appinfo;

import static com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.app.AppNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;

public class AppNotificationPreferenceController extends AppInfoPreferenceControllerBase {

    private String mChannelId = null;

    // Used for updating notification preference.
    private final NotificationBackend mBackend = new NotificationBackend();

    public AppNotificationPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void setParentFragment(AppInfoDashboardFragment parent) {
        super.setParentFragment(parent);
        if (parent != null && parent.getActivity() != null
                && parent.getActivity().getIntent() != null) {
            mChannelId = parent.getActivity().getIntent().getStringExtra(EXTRA_FRAGMENT_ARG_KEY);
        }
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getNotificationSummary(mParent.getAppEntry(), mContext, mBackend));
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppNotificationSettings.class;
    }

    @Override
    protected Bundle getArguments() {
        Bundle bundle = null;
        if (mChannelId != null) {
            bundle = new Bundle();
            bundle.putString(EXTRA_FRAGMENT_ARG_KEY, mChannelId);
        }
        return bundle;
    }


    private CharSequence getNotificationSummary(ApplicationsState.AppEntry appEntry,
            Context context, NotificationBackend backend) {
        NotificationBackend.AppRow appRow =
                backend.loadAppRow(context, context.getPackageManager(), appEntry.info);
        return getNotificationSummary(appRow, context);
    }

    public static CharSequence getNotificationSummary(NotificationBackend.AppRow appRow,
            Context context) {
        if (appRow == null) {
            return "";
        }
        if (appRow.banned) {
            return context.getText(R.string.notifications_disabled);
        } else if (appRow.channelCount == 0) {
            return NotificationBackend.getSentSummary(context, appRow.sentByApp, false);
        } else if (appRow.channelCount == appRow.blockedChannelCount) {
            return context.getText(R.string.notifications_disabled);
        } else {
            if (appRow.blockedChannelCount == 0) {
                return NotificationBackend.getSentSummary(context, appRow.sentByApp, false);
            }
            return context.getString(R.string.notifications_enabled_with_info,
                    NotificationBackend.getSentSummary(context, appRow.sentByApp, false),
                    context.getResources().getQuantityString(R.plurals.notifications_categories_off,
                            appRow.blockedChannelCount, appRow.blockedChannelCount));
        }
    }
}
