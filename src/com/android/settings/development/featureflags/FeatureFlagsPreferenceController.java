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

package com.android.settings.development.featureflags;

import android.content.Context;
import android.os.Build;
import android.util.FeatureFlagUtils;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import java.util.Map;

/**
 * A {@link BasePreferenceController} used in {@link FeatureFlagsDashboard}
 */
public class FeatureFlagsPreferenceController extends BasePreferenceController {

    private PreferenceGroup mGroup;

    public FeatureFlagsPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return Build.IS_DEBUGGABLE ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mGroup = screen.findPreference(getPreferenceKey());
        final Map<String, String> featureMap = FeatureFlagUtils.getAllFeatureFlags();
        if (featureMap == null) {
            return;
        }
        mGroup.removeAll();
        final Context prefContext = mGroup.getContext();
        featureMap.keySet().stream().sorted().forEach(feature ->
                mGroup.addPreference(new FeatureFlagPreference(prefContext, feature)));
    }
}
