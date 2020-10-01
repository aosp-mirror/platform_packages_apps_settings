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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-app Settings page that shows a list of notification channels that a user can toggle for
 * the channel to bypass DND.
 *
 * This can be found at:
 * Settings > Sound > Do Not Disturb > Apps > (Choose app)
 */
public class AppChannelsBypassingDndSettings extends NotificationSettings {
    private static final String TAG = "AppChannelsBypassingDndSettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DND_APPS_BYPASSING;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            finish();
            return;
        }

        for (NotificationPreferenceController controller : mControllers) {
            controller.onResume(mAppRow, null, null, null, null, mSuspendedAppsAdmin);
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
        return  R.xml.app_channels_bypassing_dnd_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new HeaderPreferenceController(context, this));
        mControllers.add(new AppChannelsBypassingDndPreferenceController(context,
                new NotificationBackend()));
        return new ArrayList<>(mControllers);
    }
}
