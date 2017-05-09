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

package com.android.settings.core;

import android.content.Context;
import android.os.Bundle;

import com.android.settings.core.instrumentation.Instrumentable;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.core.instrumentation.VisibilityLoggerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.survey.SurveyMixin;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;

/**
 * Instrumented fragment that logs visibility state.
 */
public abstract class InstrumentedPreferenceFragment extends ObservablePreferenceFragment
        implements Instrumentable {

    protected MetricsFeatureProvider mMetricsFeatureProvider;

    // metrics placeholder value. Only use this for development.
    protected final int PLACEHOLDER_METRIC = 10000;

    private final VisibilityLoggerMixin mVisibilityLoggerMixin;

    public InstrumentedPreferenceFragment() {
        // Mixin that logs visibility change for activity.
        mVisibilityLoggerMixin = new VisibilityLoggerMixin(getMetricsCategory());
        getLifecycle().addObserver(mVisibilityLoggerMixin);
        getLifecycle().addObserver(new SurveyMixin(this, getClass().getSimpleName()));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public void onResume() {
        mVisibilityLoggerMixin.setSourceMetricsCategory(getActivity());
        super.onResume();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    protected final Context getPrefContext() {
        return getPreferenceManager().getContext();
    }
}
