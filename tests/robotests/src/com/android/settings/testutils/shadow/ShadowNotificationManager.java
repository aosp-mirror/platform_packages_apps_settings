/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import android.app.NotificationManager;
import android.net.Uri;
import android.service.notification.ZenModeConfig;
import android.util.ArraySet;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Set;

@Implements(NotificationManager.class)
public class ShadowNotificationManager {

    private int mZenMode;
    private ZenModeConfig mZenModeConfig;
    private Set<String> mNotificationPolicyGrantedPackages = new ArraySet<>();

    @Implementation
    protected void setZenMode(int mode, Uri conditionId, String reason) {
        mZenMode = mode;
    }

    @Implementation
    protected int getZenMode() {
        return mZenMode;
    }

    @Implementation
    protected boolean isNotificationPolicyAccessGrantedForPackage(String pkg) {
        return mNotificationPolicyGrantedPackages.contains(pkg);
    }

    @Implementation
    public ZenModeConfig getZenModeConfig() {
        return mZenModeConfig;
    }

    public void setZenModeConfig(ZenModeConfig config) {
        mZenModeConfig = config;
    }

    public void setNotificationPolicyAccessGrantedForPackage(String pkg) {
        mNotificationPolicyGrantedPackages.add(pkg);
    }
}
