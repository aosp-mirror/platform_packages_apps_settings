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

package com.android.settings.privacy;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;

/** The preference controller for the top level privacy tile. */
public class TopLevelPrivacyEntryPreferenceController  extends BasePreferenceController {

    public TopLevelPrivacyEntryPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!SafetyCenterManagerWrapper.get().isEnabled(mContext)) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }
}
