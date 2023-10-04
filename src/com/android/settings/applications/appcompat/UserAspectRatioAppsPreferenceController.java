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

package com.android.settings.applications.appcompat;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Preference controller for
 * {@link com.android.settings.spa.app.appcompat.UserAspectRatioAppsPageProvider}
 */
public class UserAspectRatioAppsPreferenceController extends BasePreferenceController {

    public UserAspectRatioAppsPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return UserAspectRatioManager.isFeatureEnabled(mContext)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getResources().getString(R.string.aspect_ratio_summary_text, Build.MODEL);
    }
}
