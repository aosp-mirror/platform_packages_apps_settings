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

package com.android.settings.security;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class ScreenPinningPreferenceController extends BasePreferenceController {

    private static final String KEY_SCREEN_PINNING = "screen_pinning_settings";

    public ScreenPinningPreferenceController(Context context) {
        super(context, KEY_SCREEN_PINNING);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_screen_pinning_settings)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_TO_APP_ENABLED, 0) != 0
                ? mContext.getText(R.string.switch_on_text)
                : mContext.getText(R.string.switch_off_text);
    }
}
