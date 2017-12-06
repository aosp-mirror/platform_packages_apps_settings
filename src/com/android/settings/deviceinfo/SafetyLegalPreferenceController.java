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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class SafetyLegalPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_SAFETY_LEGAL = "safetylegal";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";


    public SafetyLegalPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(SystemProperties.get(PROPERTY_URL_SAFETYLEGAL));
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SAFETY_LEGAL;
    }
}
