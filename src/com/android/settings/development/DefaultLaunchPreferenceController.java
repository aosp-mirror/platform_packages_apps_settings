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

package com.android.settings.development;

import android.content.Context;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * This controller is used for preference that has fragment or launch intent defined in the
 * preference xml, and do not need any handling to update the preference state, except when the
 * master developer options switch is turned on/off, the preference needs to be enabled/disabled.
 *
 */
public class DefaultLaunchPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private final String mPreferenceKey;

    public DefaultLaunchPreferenceController(Context context, String preferenceKey) {
        super(context);

        mPreferenceKey = preferenceKey;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }
}
