/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.display;

import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

/**
 * Only show the "When to show" Doze preferences if there's an ambient display available.
 */
public class AmbientDisplayWhenToShowPreferenceController extends
        BasePreferenceController implements PreferenceControllerMixin {
    private final AmbientDisplayConfiguration mConfig;

    public AmbientDisplayWhenToShowPreferenceController(Context context, String key) {
        super(context, key);
        mConfig = new AmbientDisplayConfiguration(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mConfig.ambientDisplayAvailable() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
