/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.applications.specialaccess.notificationaccess;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.service.notification.NotificationListenerFilter;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.notification.NotificationBackend;

public class BridgedAppsLinkPreferenceController extends BasePreferenceController {

    private ComponentName mCn;
    private int mUserId;
    private NotificationBackend mNm;
    private NotificationListenerFilter mNlf;
    private int mTargetSdk;

    public BridgedAppsLinkPreferenceController(Context context, String key) {
        super(context, key);
    }


    public BridgedAppsLinkPreferenceController setCn(ComponentName cn) {
        mCn = cn;
        return this;
    }

    public BridgedAppsLinkPreferenceController setUserId(int userId) {
        mUserId = userId;
        return this;
    }

    public BridgedAppsLinkPreferenceController setNm(NotificationBackend nm) {
        mNm = nm;
        return this;
    }

    public BridgedAppsLinkPreferenceController setTargetSdk(int targetSdk) {
        mTargetSdk = targetSdk;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mNm.isNotificationListenerAccessGranted(mCn)) {
            if (mTargetSdk > Build.VERSION_CODES.S) {
                return AVAILABLE;
            }
            mNlf = mNm.getListenerFilter(mCn, mUserId);
            if (!mNlf.areAllTypesAllowed() || !mNlf.getDisallowedPackages().isEmpty()) {
                return AVAILABLE;
            }
        }
        return DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void updateState(Preference pref) {
        pref.setEnabled(getAvailabilityStatus() == AVAILABLE);
        super.updateState(pref);
    }
}
