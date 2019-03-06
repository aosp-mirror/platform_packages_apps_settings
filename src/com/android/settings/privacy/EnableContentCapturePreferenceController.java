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

package com.android.settings.privacy;

import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.contentcapture.ContentCaptureManager;

import com.android.settings.core.TogglePreferenceController;

public class EnableContentCapturePreferenceController extends TogglePreferenceController {

    private static final String KEY_SHOW_PASSWORD = "content_capture";
    private static final int MY_USER_ID = UserHandle.myUserId();

    public EnableContentCapturePreferenceController(Context context) {
        super(context, KEY_SHOW_PASSWORD);
    }

    @Override
    public boolean isChecked() {
        boolean enabled = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.CONTENT_CAPTURE_ENABLED, 1, MY_USER_ID) == 1;
        return enabled;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.CONTENT_CAPTURE_ENABLED, isChecked ? 1 : 0, MY_USER_ID);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        // We cannot look for ContentCaptureManager, because it's not available if the service
        // didn't whitelist Settings
        IBinder service = ServiceManager.checkService(Context.CONTENT_CAPTURE_MANAGER_SERVICE);
        return service != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
