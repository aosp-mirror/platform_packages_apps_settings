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

import static android.provider.Settings.Secure.NOTIFICATION_NEW_INTERRUPTION_MODEL;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

import com.google.common.annotations.VisibleForTesting;

public class SilentStatusBarPreferenceController extends TogglePreferenceController {

    private static final String KEY = "silent_icons";
    private static final int MY_USER_ID = UserHandle.myUserId();
    private NotificationBackend mBackend;
    private Listener mListener;

    public SilentStatusBarPreferenceController(Context context) {
        super(context, KEY);
        mBackend = new NotificationBackend();
    }

    @VisibleForTesting
    void setBackend(NotificationBackend backend) {
        mBackend = backend;
    }

    @Override
    public boolean isChecked() {
        return !mBackend.shouldHideSilentStatusBarIcons(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mBackend.setHideSilentStatusIcons(!isChecked);
        if (mListener != null) {
            mListener.onChange(isChecked);
        }
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(), NOTIFICATION_NEW_INTERRUPTION_MODEL, 1) != 0
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    interface Listener {
        void onChange(boolean shown);
    }

}


