/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Accessibility settings for the vibration.
 */
public class VibrationSettings extends DashboardFragment {

    private static final String TAG = "VibrationSettings";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY_VIBRATION;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_vibration_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildControllers(context, getLifecycle());
    }

    public static List<AbstractPreferenceController> buildControllers(Context context,
            Lifecycle lifecycle) {

        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final NotificationVibrationIntensityPreferenceController notifVibPrefController =
                new NotificationVibrationIntensityPreferenceController(context);
        final HapticFeedbackIntensityPreferenceController hapticPreferenceController =
                new HapticFeedbackIntensityPreferenceController(context);
        controllers.add(hapticPreferenceController);
        controllers.add(notifVibPrefController);
        if (lifecycle != null) {
            lifecycle.addObserver(hapticPreferenceController);
            lifecycle.addObserver(notifVibPrefController);
        }
        return controllers;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    List<SearchIndexableResource> indexables = new ArrayList<>();
                    SearchIndexableResource indexable = new SearchIndexableResource(context);
                    indexable.xmlResId = R.xml.accessibility_vibration_settings;
                    indexables.add(indexable);
                    return indexables;
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(
                        Context context) {
                    return buildControllers(context, null /* lifecycle */);
                }
            };
}
