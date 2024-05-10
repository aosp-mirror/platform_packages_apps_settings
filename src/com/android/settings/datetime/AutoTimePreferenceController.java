/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.app.time.Capabilities.CAPABILITY_NOT_ALLOWED;
import static android.app.time.Capabilities.CAPABILITY_NOT_APPLICABLE;
import static android.app.time.Capabilities.CAPABILITY_NOT_SUPPORTED;
import static android.app.time.Capabilities.CAPABILITY_POSSESSED;

import android.app.time.TimeCapabilities;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.app.time.TimeManager;
import android.content.Context;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class AutoTimePreferenceController extends TogglePreferenceController {

    private UpdateTimeAndDateCallback mCallback;
    private final TimeManager mTimeManager;

    public AutoTimePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mTimeManager = context.getSystemService(TimeManager.class);
    }

    public void setDateAndTimeCallback(UpdateTimeAndDateCallback callback) {
        mCallback = callback;
    }

    @Override
    public int getAvailabilityStatus() {
        TimeCapabilities timeCapabilities =
                getTimeCapabilitiesAndConfig().getCapabilities();
        int capability = timeCapabilities.getConfigureAutoDetectionEnabledCapability();

        // The preference has three states: visible, not visible, and visible but disabled.
        // This method handles the "is visible?" check.
        switch (capability) {
            case CAPABILITY_NOT_SUPPORTED:
                return DISABLED_DEPENDENT_SETTING;
            case CAPABILITY_POSSESSED:
            case CAPABILITY_NOT_ALLOWED:
                // This case is expected for enterprise restrictions, where the toggle should be
                // present but disabled. Disabling is handled declaratively via the
                // settings:userRestriction attribute in .xml. The client-side logic is expected to
                // concur with the capabilities logic in the system server.
            case CAPABILITY_NOT_APPLICABLE:
                // CAPABILITY_NOT_APPLICABLE is not currently expected, so this is return value is
                // arbitrary.
                return AVAILABLE;
            default:
                throw new IllegalStateException("Unknown capability=" + capability);
        }
    }

    @Override
    public boolean isChecked() {
        return isEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        TimeConfiguration configuration = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(isChecked)
                .build();
        boolean result = mTimeManager.updateTimeConfiguration(configuration);

        mCallback.updateTimeAndDateDisplay(mContext);
        return result;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    /** Returns whether the preference should be "checked", i.e. set to the "on" position. */
    @VisibleForTesting
    public boolean isEnabled() {
        TimeConfiguration config = getTimeCapabilitiesAndConfig().getConfiguration();
        return config.isAutoDetectionEnabled();
    }

    private TimeCapabilitiesAndConfig getTimeCapabilitiesAndConfig() {
        return mTimeManager.getTimeCapabilitiesAndConfig();
    }
}
