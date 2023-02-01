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

import android.app.people.IPeopleManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.ServiceManager;
import android.service.notification.ConversationChannelWrapper;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ConversationListSettings extends DashboardFragment {
    private static final String TAG = "ConvoListSettings";

    NotificationBackend mBackend = new NotificationBackend();
    IPeopleManager mPs;

    protected List<AbstractPreferenceController> mControllers = new ArrayList<>();
    private NoConversationsPreferenceController mNoConversationsController;
    private PriorityConversationsPreferenceController mPriorityConversationsController;
    private AllConversationsPreferenceController mAllConversationsController;
    private RecentConversationsPreferenceController mRecentConversationsController;
    private boolean mUpdatedInOnCreate = false;

    public ConversationListSettings() {
        mPs = IPeopleManager.Stub.asInterface(
                ServiceManager.getService(Context.PEOPLE_SERVICE));
    }

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
        mNoConversationsController = new NoConversationsPreferenceController(context);
        mControllers.add(mNoConversationsController);
        mPriorityConversationsController =
                new PriorityConversationsPreferenceController(context, mBackend);
        mControllers.add(mPriorityConversationsController);
        mAllConversationsController = new AllConversationsPreferenceController(context, mBackend);
        mControllers.add(mAllConversationsController);
        mRecentConversationsController =
                new RecentConversationsPreferenceController(context, mBackend, mPs);
        mControllers.add(mRecentConversationsController);
        return new ArrayList<>(mControllers);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        update();
        mUpdatedInOnCreate = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mUpdatedInOnCreate) {
            mUpdatedInOnCreate = false;
        } else {
            update();
        }
    }

    private void update() {
        List<ConversationChannelWrapper> conversationList =
                mBackend.getConversations(false).getList();
        boolean hasContent = mPriorityConversationsController.updateList(conversationList)
                | mAllConversationsController.updateList(conversationList)
                | mRecentConversationsController.updateList();
        mNoConversationsController.setAvailable(!hasContent);
        mNoConversationsController.displayPreference(getPreferenceScreen());
    }
}
