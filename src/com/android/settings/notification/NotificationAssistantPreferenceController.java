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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.widget.CandidateInfo;

import com.google.common.annotations.VisibleForTesting;

public class NotificationAssistantPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    protected NotificationBackend mNotificationBackend;
    private PackageManager mPackageManager;

    public NotificationAssistantPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mNotificationBackend = new NotificationBackend();
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return BasePreferenceController.AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        CandidateInfo appSelected = new NotificationAssistantPicker.CandidateNone(mContext);
        ComponentName assistant = mNotificationBackend.getAllowedNotificationAssistant();
        if (assistant != null) {
            appSelected = createCandidateInfo(assistant);
        }
        return appSelected.loadLabel();
    }

    @VisibleForTesting
    protected CandidateInfo createCandidateInfo(ComponentName cn) {
        return new DefaultAppInfo(mContext, mPackageManager, UserHandle.myUserId(), cn);
    }
}
