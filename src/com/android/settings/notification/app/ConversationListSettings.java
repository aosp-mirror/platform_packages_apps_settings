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
import android.util.Log;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ConversationListSettings extends DashboardFragment {
    private static final String TAG = "ConvoListSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    NotificationBackend mBackend = new NotificationBackend();
    protected List<AbstractPreferenceController> mControllers = new ArrayList<>();

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_CONVERSATION_LIST_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.conversation_list_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new NoConversationsPreferenceController(context, mBackend));
        mControllers.add(new PriorityConversationsPreferenceController(context, mBackend));
        mControllers.add(new AllConversationsPreferenceController(context, mBackend));
        return new ArrayList<>(mControllers);
    }
}
