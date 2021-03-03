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

package com.android.settings.applications;

import static android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION;

import static com.android.settings.Utils.PROPERTY_APP_HIBERNATION_ENABLED;

import android.content.Context;
import android.provider.DeviceConfig;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * A preference controller handling the logic for updating summary of hibernated apps.
 * TODO(b/181172051): add intent to launch Auto Revoke UI in app_and_notification.xml
 */
public final class HibernatedAppsPreferenceController extends BasePreferenceController {
    private static final String TAG = "HibernatedAppsPrefController";

    public HibernatedAppsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return isHibernationEnabled() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final int numHibernated = getNumHibernated();
        return mContext.getResources().getQuantityString(
                R.plurals.unused_apps_summary, numHibernated, numHibernated);
    }

    private int getNumHibernated() {
        //TODO(b/181172051): hook into hibernation service to get the number of hibernated apps.
        return 0;
    }

    private static boolean isHibernationEnabled() {
        return DeviceConfig.getBoolean(
                NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED, false);
    }
}
