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
package com.android.settings.system;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

public class AdditionalSystemUpdatePreferenceController extends BasePreferenceController {

    private static final String KEY_UPDATE_SETTING = "additional_system_update_settings";

    public AdditionalSystemUpdatePreferenceController(Context context) {
        super(context, KEY_UPDATE_SETTING);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.settings.R.bool.config_additional_system_update_setting_enable)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }
}