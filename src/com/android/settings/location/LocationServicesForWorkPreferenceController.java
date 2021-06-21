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

package com.android.settings.location;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

/**
 * Preference controller for Location Services for Work Profile.
 */
public class LocationServicesForWorkPreferenceController extends BasePreferenceController {

    public LocationServicesForWorkPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public CharSequence getSummary() {
        return null;
    }

    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
