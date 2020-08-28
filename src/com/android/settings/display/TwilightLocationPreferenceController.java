/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.view.View;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Controller to take the user to location settings page
 */
public class TwilightLocationPreferenceController extends BasePreferenceController {
    private final LocationManager mLocationManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public TwilightLocationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mLocationManager = context.getSystemService(LocationManager.class);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference preference = screen.findPreference(getPreferenceKey());
        final View button = preference.findViewById(R.id.go_to_location_setting);
        button.setOnClickListener(v -> {
            mMetricsFeatureProvider.logClickedPreference(preference, getMetricsCategory());
            final Intent intent = new Intent();
            intent.setClass(mContext, Settings.LocationSettingsActivity.class);
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getAvailabilityStatus() {
        return mLocationManager.isLocationEnabled() ? CONDITIONALLY_UNAVAILABLE
                : AVAILABLE_UNSEARCHABLE;
    }
}
