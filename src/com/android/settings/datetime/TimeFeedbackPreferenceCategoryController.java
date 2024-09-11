/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.datetime;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A controller for the Settings category for "time feedback".
 */
public class TimeFeedbackPreferenceCategoryController extends BasePreferenceController {

    private final List<AbstractPreferenceController> mChildControllers = new ArrayList<>();

    public TimeFeedbackPreferenceCategoryController(
            Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Adds a controller whose own availability can determine the category's availability status.
     */
    void addChildController(@NonNull AbstractPreferenceController childController) {
        mChildControllers.add(Objects.requireNonNull(childController));
    }

    @Override
    public int getAvailabilityStatus() {
        // Firstly, hide the category if it is not enabled by flags.
        if (!isTimeFeedbackFeatureEnabled()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        // Secondly, only show the category if there's one or more controllers available within it.
        for (AbstractPreferenceController childController : mChildControllers) {
            if (childController.isAvailable()) {
                return AVAILABLE;
            }
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    protected boolean isTimeFeedbackFeatureEnabled() {
        return TimeFeedbackLaunchUtils.isFeedbackFeatureSupported();
    }
}
