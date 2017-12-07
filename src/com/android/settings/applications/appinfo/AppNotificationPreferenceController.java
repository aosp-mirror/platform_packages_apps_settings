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

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;

public class AppNotificationPreferenceController extends AppInfoPreferenceControllerBase {

    private static final String KEY_NOTIFICATION = "notification_settings";

    // Used for updating notification preference.
    private final NotificationBackend mBackend = new NotificationBackend();

    public AppNotificationPreferenceController(Context context, AppInfoDashboardFragment parent) {
        super(context, parent, KEY_NOTIFICATION);
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getNotificationSummary(mParent.getAppEntry(), mContext, mBackend));
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppNotificationSettings.class;
    }

    private CharSequence getNotificationSummary(ApplicationsState.AppEntry appEntry,
            Context context, NotificationBackend backend) {
        NotificationBackend.AppRow appRow =
                backend.loadAppRow(context, context.getPackageManager(), appEntry.info);
        return getNotificationSummary(appRow, context);
    }

    public static CharSequence getNotificationSummary(NotificationBackend.AppRow appRow,
            Context context) {
        // TODO: implement summary when it is known what it should say
        return "";
    }
}
