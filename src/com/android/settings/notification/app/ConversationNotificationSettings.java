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
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ConversationNotificationSettings extends NotificationSettings {
    private static final String TAG = "ConvoSettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_CONVERSATION_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null || mChannel == null) {
            Log.w(TAG, "Missing package or uid or packageinfo or channel");
            finish();
            return;
        }

        for (NotificationPreferenceController controller : mControllers) {
            controller.onResume(mAppRow, mChannel, mChannelGroup, mConversationDrawable,
                    mConversationInfo, mSuspendedAppsAdmin);
            controller.displayPreference(getPreferenceScreen());
        }
        updatePreferenceStates();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (NotificationPreferenceController controller : mControllers) {
            if (controller instanceof PreferenceManager.OnActivityResultListener) {
                ((PreferenceManager.OnActivityResultListener) controller)
                        .onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.conversation_notification_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new ConversationHeaderPreferenceController(context, this));
        mControllers.add(new ConversationPriorityPreferenceController(
                context, mBackend, mDependentFieldListener));
        mControllers.add(new HighImportancePreferenceController(
                context, mDependentFieldListener, mBackend));
        mControllers.add(new SoundPreferenceController(context, this,
                mDependentFieldListener, mBackend));
        mControllers.add(new VibrationPreferenceController(context, mBackend));
        mControllers.add(new VisibilityPreferenceController(context, new LockPatternUtils(context),
                mBackend));
        mControllers.add(new LightsPreferenceController(context, mBackend));
        mControllers.add(new BadgePreferenceController(context, mBackend));
        mControllers.add(new NotificationsOffPreferenceController(context));
        mControllers.add(new BubblePreferenceController(context, getChildFragmentManager(),
                mBackend, false /* isAppPage */, null /* dependentFieldListener */));
        mControllers.add(new ConversationDemotePreferenceController(context, this, mBackend));
        mControllers.add(new BubbleCategoryPreferenceController(context));
        mControllers.add(new BubbleLinkPreferenceController(context));
        return new ArrayList<>(mControllers);
    }
}
