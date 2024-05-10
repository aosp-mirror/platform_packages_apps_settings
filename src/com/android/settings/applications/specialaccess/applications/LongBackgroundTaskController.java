/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.applications;

import android.content.Context;

import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;

/**
 * The controller for the special access to the long background task.
 */
public class LongBackgroundTaskController extends BasePreferenceController {
    private final ApplicationFeatureProvider mAppFeatureProvider;

    public LongBackgroundTaskController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAppFeatureProvider = FeatureFactory.getFeatureFactory()
                .getApplicationFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return mAppFeatureProvider.isLongBackgroundTaskPermissionToggleSupported()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
