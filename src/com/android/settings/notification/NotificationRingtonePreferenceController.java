/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.media.RingtoneManager;

import com.android.settings.R;

public class NotificationRingtonePreferenceController extends RingtonePreferenceControllerBase {

    private static final String KEY_NOTIFICATION_RINGTONE = "notification_ringtone";

    public NotificationRingtonePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_notification_ringtone);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NOTIFICATION_RINGTONE;
    }

    @Override
    public int getRingtoneType() {
        return RingtoneManager.TYPE_NOTIFICATION;
    }
}
