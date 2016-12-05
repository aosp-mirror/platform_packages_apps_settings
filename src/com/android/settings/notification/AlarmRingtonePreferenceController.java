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

public class AlarmRingtonePreferenceController extends RingtonePreferenceControllerBase {

    private static final String KEY_ALARM_RINGTONE = "alarm_ringtone";

    public AlarmRingtonePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALARM_RINGTONE;
    }

    @Override
    public int getRingtoneType() {
        return RingtoneManager.TYPE_ALARM;
    }
}
