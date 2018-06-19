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

package com.android.settings.notification;

import static com.android.settings.notification.SettingPref.TYPE_GLOBAL;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class EmergencyTonePreferenceController extends SettingPrefController {

    private static final String KEY_EMERGENCY_TONE = "emergency_tone";
    private static final int EMERGENCY_TONE_SILENT = 0;
    private static final int EMERGENCY_TONE_ALERT = 1;
    private static final int EMERGENCY_TONE_VIBRATE = 2;
    private static final int DEFAULT_EMERGENCY_TONE = EMERGENCY_TONE_SILENT;

    public EmergencyTonePreferenceController(Context context, SettingsPreferenceFragment parent,
            Lifecycle lifecycle) {
        super(context, parent, lifecycle);
        mPreference = new SettingPref(
            TYPE_GLOBAL, KEY_EMERGENCY_TONE, Global.EMERGENCY_TONE, DEFAULT_EMERGENCY_TONE,
            EMERGENCY_TONE_ALERT, EMERGENCY_TONE_VIBRATE, EMERGENCY_TONE_SILENT) {

            @Override
            public boolean isApplicable(Context context) {
                final TelephonyManager telephony =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                return telephony != null
                    && telephony.getCurrentPhoneType() == TelephonyManager.PHONE_TYPE_CDMA;
            }

            @Override
            protected String getCaption(Resources res, int value) {
                switch(value) {
                    case EMERGENCY_TONE_SILENT:
                        return res.getString(R.string.emergency_tone_silent);
                    case EMERGENCY_TONE_ALERT:
                        return res.getString(R.string.emergency_tone_alert);
                    case EMERGENCY_TONE_VIBRATE:
                        return res.getString(R.string.emergency_tone_vibrate);
                    default:
                        throw new IllegalArgumentException();
                }
            }
        };
    }

}
