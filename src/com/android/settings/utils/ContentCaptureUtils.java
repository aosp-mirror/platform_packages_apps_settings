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

package com.android.settings.utils;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.contentcapture.ContentCaptureManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ContentCaptureUtils {

    private static final String TAG = ContentCaptureUtils.class.getSimpleName();
    private static final int MY_USER_ID = UserHandle.myUserId();

    public static boolean isEnabledForUser(@NonNull Context context) {
        boolean enabled = Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.CONTENT_CAPTURE_ENABLED, 1, MY_USER_ID) == 1;
        return enabled;
    }

    public static void setEnabledForUser(@NonNull Context context, boolean enabled) {
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.CONTENT_CAPTURE_ENABLED, enabled ? 1 : 0, MY_USER_ID);
    }

    public static boolean isFeatureAvailable() {
        // We cannot look for ContentCaptureManager, because it's not available if the service
        // didn't allowlist Settings
        IBinder service = ServiceManager.checkService(Context.CONTENT_CAPTURE_MANAGER_SERVICE);
        return service != null;
    }

    @Nullable
    public static ComponentName getServiceSettingsComponentName() {
        try {
            return ContentCaptureManager.getServiceSettingsComponentName();
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not get service settings: " + e);
            return null;
        }
    }

    private ContentCaptureUtils() {
        throw new UnsupportedOperationException("contains only static methods");
    }
}
