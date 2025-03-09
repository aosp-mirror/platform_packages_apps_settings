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

package com.android.settings.notification;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.server.notification.Flags;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Fragment for notifications on lock screen preference page.
 */
@SearchIndexable
public class LockScreenNotificationsPreferencePageFragment extends DashboardFragment {

    @Override
    public int getMetricsCategory() {
        //TODO(b/367455695): create a new metrics category
        return SettingsEnums.SETTINGS_LOCK_SCREEN_PREFERENCES;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.lock_screen_notifications_settings;
    }
    @Override
    protected String getLogTag() {
        return "LockScreenNotificationsPreferenceFragment";
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.lock_screen_notifications_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return Flags.notificationLockScreenSettings();
                }
            };
}
