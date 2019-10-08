/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.ActivityManager;
import android.app.IActivityManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(ActivityManager.class)
public class ShadowActivityManager {
    private static int sCurrentUserId = 0;
    private static IActivityManager sService = null;

    @Implementation
    protected static int getCurrentUser() {
        return sCurrentUserId;
    }

    public static void setCurrentUser(int userId) {
        sCurrentUserId = userId;
    }

    @Implementation
    public static IActivityManager getService() {
        return sService;
    }

    public static void setService(IActivityManager service) {
        sService = service;
    }
}
