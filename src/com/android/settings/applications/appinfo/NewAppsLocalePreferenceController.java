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

package com.android.settings.applications.appinfo;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;

/**
 * A controller to update current locale information of application
 * and a entry to launch {@link ManageApplications}.
 */
public class NewAppsLocalePreferenceController extends BasePreferenceController {

    public NewAppsLocalePreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO(b/381011808) After feature release, this class may be renamed.
        if (Flags.regionalPreferencesApiEnabled()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }
}
