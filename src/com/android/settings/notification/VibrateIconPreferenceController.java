/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.notification.SettingPref.TYPE_SECURE;

import android.content.Context;
import android.os.Vibrator;
import android.provider.Settings.Secure;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class VibrateIconPreferenceController extends SettingPrefController {

    private static final String KEY_VIBRATE_ICON = "vibrate_icon";
    private final boolean mHasVibrator;

    public VibrateIconPreferenceController(Context context, SettingsPreferenceFragment parent,
            Lifecycle lifecycle) {
        super(context, parent, lifecycle);
        mHasVibrator = context.getSystemService(Vibrator.class).hasVibrator();
        mPreference = new SettingPref(
            TYPE_SECURE, KEY_VIBRATE_ICON, Secure.STATUS_BAR_SHOW_VIBRATE_ICON, 0 /*default off*/);
    }

    @Override
    public boolean isAvailable() {
        return mHasVibrator;
    }
}
