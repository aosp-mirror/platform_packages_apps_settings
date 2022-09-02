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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.util.FeatureFlagUtils;

import com.android.settings.core.BasePreferenceController;

/**
 * A controller to update current locale information of application
 * and a entry to launch {@link ManageApplications}.
 * TODO(209775925) After feature release, this class may be removed.
 */
public class ManageAppLocalePreferenceController extends BasePreferenceController {
    public ManageAppLocalePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return FeatureFlagUtils
                .isEnabled(mContext, FeatureFlagUtils.SETTINGS_APP_LANGUAGE_SELECTION)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
