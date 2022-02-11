/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.dream;

import android.content.Context;
import android.widget.Button;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Controller that used to enable screen saver
 */
public class StartNowPreferenceController extends BasePreferenceController {

    private final DreamBackend mBackend;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public StartNowPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBackend = DreamBackend.getInstance(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        final LayoutPreference pref = screen.findPreference(getPreferenceKey());
        final Button startButton = pref.findViewById(R.id.dream_start_now_button);
        startButton.setOnClickListener(v -> {
            mMetricsFeatureProvider.logClickedPreference(pref,
                    pref.getExtras().getInt(DashboardFragment.CATEGORY));
            mBackend.startDreaming();
        });
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final Button startButton = ((LayoutPreference) preference)
                .findViewById(R.id.dream_start_now_button);
        startButton.setEnabled(mBackend.getWhenToDreamSetting() != DreamBackend.NEVER);
    }
}
