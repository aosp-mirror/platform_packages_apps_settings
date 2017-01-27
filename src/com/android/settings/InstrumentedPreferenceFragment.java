/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.support.v14.preference.PreferenceFragment;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SurveyFeatureProvider;

/**
 * Instrumented preference fragment that logs visibility state.
 */
public abstract class InstrumentedPreferenceFragment extends PreferenceFragment {
    /**
     * Declare the view of this category.
     *
     * Categories are defined in {@link com.android.internal.logging.MetricsProto.MetricsEvent}
     * or if there is no relevant existing category you may define one in
     * {@link com.android.settings.InstrumentedFragment}.
     */
    protected abstract int getMetricsCategory();
    private BroadcastReceiver mReceiver;

    @Override
    public void onResume() {
        super.onResume();
        MetricsLogger.visible(getActivity(), getMetricsCategory());

        Activity activity = getActivity();
        // guard against the activity not existing yet or the feature being disabled
        if (activity != null) {
            SurveyFeatureProvider provider =
                    FeatureFactory.getFactory(activity).getSurveyFeatureProvider(activity);
            if (provider != null) {
                // Try to download a survey if there is none available, show the survey otherwise
                String id = provider.getSurveyId(activity, getClass().getSimpleName());
                if (provider.getSurveyExpirationDate(activity, id) <= -1) {
                    // register the receiver to show the survey on completion.
                    mReceiver = provider.createAndRegisterReceiver(activity);
                    provider.downloadSurvey(activity, id, null /* data */);
                } else {
                    provider.showSurveyIfAvailable(activity, id);
                }
            }
        }
    }

    @Override
    public void onPause() {
        Activity activity = getActivity();
        if (mReceiver != null && activity != null) {
            SurveyFeatureProvider.unregisterReceiver(activity, mReceiver);
            mReceiver = null;
        }

        super.onPause();
        MetricsLogger.hidden(getActivity(), getMetricsCategory());
    }
}
