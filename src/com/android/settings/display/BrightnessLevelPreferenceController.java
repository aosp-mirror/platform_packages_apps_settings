/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.display;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceController;
import java.text.NumberFormat;

public class BrightnessLevelPreferenceController extends PreferenceController {

    private static final String KEY_BRIGHTNESS = "brightness";

    public BrightnessLevelPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BRIGHTNESS;
    }

    @Override
    public void updateState(Preference preference) {
        final double brightness = Settings.System.getInt(mContext.getContentResolver(),
            SCREEN_BRIGHTNESS, 0);
        preference.setSummary(NumberFormat.getPercentInstance().format(brightness / 255));
    }

}
