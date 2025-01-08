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

import static android.app.time.Capabilities.CAPABILITY_NOT_ALLOWED;
import static android.app.time.Capabilities.CAPABILITY_NOT_APPLICABLE;
import static android.app.time.Capabilities.CAPABILITY_NOT_SUPPORTED;
import static android.app.time.Capabilities.CAPABILITY_POSSESSED;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.concurrent.Executor;

public final class TimeZoneNotificationsPreferenceController
        extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop, TimeManager.TimeZoneDetectorListener {

    private static final String TAG = "TZNotificationsSettings";

    private final TimeManager mTimeManager;
    private @Nullable TimeZoneCapabilitiesAndConfig mTimeZoneCapabilitiesAndConfig;
    private @Nullable Preference mPreference;

    public TimeZoneNotificationsPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mTimeManager = context.getSystemService(TimeManager.class);
    }

    /**
     * Registers this controller with a category controller so that the category can be optionally
     * displayed, i.e. if all the child controllers are not available, the category heading won't be
     * available.
     */
    public void registerIn(@NonNull NotificationsPreferenceCategoryController categoryController) {
        categoryController.addChildController(this);
    }

    @Override
    public boolean isChecked() {
        if (!isAutoTimeZoneEnabled()) {
            return false;
        }

        // forceRefresh set to true as the notifications toggle may have been turned off by
        // switching off automatic time zone
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ true);
        TimeZoneConfiguration configuration = capabilitiesAndConfig.getConfiguration();
        return configuration.areNotificationsEnabled();
    }


    @Override
    public boolean setChecked(boolean isChecked) {
        TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder()
                .setNotificationsEnabled(isChecked)
                .build();
        return mTimeManager.updateTimeZoneConfiguration(configuration);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        // Register for updates to the user's time zone capabilities or configuration which could
        // require UI changes.
        Executor mainExecutor = mContext.getMainExecutor();
        mTimeManager.addTimeZoneDetectorListener(mainExecutor, this);
        // Setup the initial state.
        refreshUi();
    }

    @Override
    public void onStop() {
        mTimeManager.removeTimeZoneDetectorListener(this);
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);

        // enable / disable the toggle based on automatic time zone being enabled or not
        preference.setEnabled(isAutoTimeZoneEnabled());
    }


    @Override
    public int getAvailabilityStatus() {
        TimeZoneCapabilities timeZoneCapabilities =
                getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ false).getCapabilities();
        int capability = timeZoneCapabilities.getConfigureNotificationsEnabledCapability();

        // The preference can be present and enabled, present and disabled or not present.
        if (capability == CAPABILITY_NOT_SUPPORTED || capability == CAPABILITY_NOT_ALLOWED) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (capability == CAPABILITY_NOT_APPLICABLE || capability == CAPABILITY_POSSESSED) {
            return isAutoTimeZoneEnabled() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
        } else {
            Log.e(TAG, "Unknown capability=" + capability);
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    /**
     * Implementation of {@link TimeManager.TimeZoneDetectorListener#onChange()}. Called by the
     * system server after a change that affects {@link TimeZoneCapabilitiesAndConfig}.
     */
    @Override
    public void onChange() {
        refreshUi();
    }

    @Override
    @NonNull
    public CharSequence getSummary() {
        return mContext.getString(R.string.time_zone_change_notifications_toggle_summary);
    }

    private void refreshUi() {
        // Force a refresh of cached user capabilities and config.
        getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ true);
        refreshSummary(mPreference);
    }

    /**
     * Returns the current user capabilities and configuration. {@code forceRefresh} can be {@code
     * true} to discard any cached copy.
     */
    private TimeZoneCapabilitiesAndConfig getTimeZoneCapabilitiesAndConfig(boolean forceRefresh) {
        if (forceRefresh || mTimeZoneCapabilitiesAndConfig == null) {
            mTimeZoneCapabilitiesAndConfig = mTimeManager.getTimeZoneCapabilitiesAndConfig();
        }
        return mTimeZoneCapabilitiesAndConfig;
    }

    /**
     * Returns whether the user can select this preference or not, as it is a sub toggle of
     * automatic time zone.
     */
    private boolean isAutoTimeZoneEnabled() {
        return mTimeManager.getTimeZoneCapabilitiesAndConfig().getConfiguration()
                .isAutoDetectionEnabled();
    }
}
