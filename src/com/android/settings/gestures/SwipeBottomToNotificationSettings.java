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

package com.android.settings.gestures;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;

/**
 * The Fragment for swipe bottom to notification gesture settings.
 */
public class SwipeBottomToNotificationSettings extends DashboardFragment {

    private static final String TAG = "SwipeBottomToNotificationSettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_SWIPE_BOTTOM_TO_NOTIFICATION;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.swipe_bottom_to_notification_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.swipe_bottom_to_notification_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return SwipeBottomToNotificationPreferenceController
                            .isGestureAvailable(context);
                }
            };
}
