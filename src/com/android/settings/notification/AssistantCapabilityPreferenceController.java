/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.service.notification.Adjustment;

import com.android.settings.core.TogglePreferenceController;

import com.google.common.annotations.VisibleForTesting;

import java.util.List;

public class AssistantCapabilityPreferenceController extends TogglePreferenceController {

    static final String PRIORITIZER_KEY = "asst_capability_prioritizer";
    static final String RANKING_KEY = "asst_capability_ranking";
    static final String SMART_KEY = "asst_capabilities_actions_replies";
    private NotificationBackend mBackend;

    public AssistantCapabilityPreferenceController(Context context, String key) {
        super(context, key);
        mBackend = new NotificationBackend();
    }

    @VisibleForTesting
    void setBackend(NotificationBackend backend) {
        mBackend = backend;
    }

    @Override
    public boolean isChecked() {
        List<String> capabilities = mBackend.getAssistantAdjustments(mContext.getPackageName());
        if (PRIORITIZER_KEY.equals(getPreferenceKey())) {
            return capabilities.contains(Adjustment.KEY_IMPORTANCE);
        } else if (RANKING_KEY.equals(getPreferenceKey())) {
            return capabilities.contains(Adjustment.KEY_RANKING_SCORE);
        } else if (SMART_KEY.equals(getPreferenceKey())) {
            return capabilities.contains(Adjustment.KEY_CONTEXTUAL_ACTIONS)
                    && capabilities.contains(Adjustment.KEY_TEXT_REPLIES);
        }
        return false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (PRIORITIZER_KEY.equals(getPreferenceKey())) {
            mBackend.allowAssistantAdjustment(Adjustment.KEY_IMPORTANCE, isChecked);
        } else if (RANKING_KEY.equals(getPreferenceKey())) {
            mBackend.allowAssistantAdjustment(Adjustment.KEY_RANKING_SCORE, isChecked);
        } else if (SMART_KEY.equals(getPreferenceKey())) {
            mBackend.allowAssistantAdjustment(Adjustment.KEY_CONTEXTUAL_ACTIONS, isChecked);
            mBackend.allowAssistantAdjustment(Adjustment.KEY_TEXT_REPLIES, isChecked);
        }
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return mBackend.getAllowedNotificationAssistant() != null
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }
}


