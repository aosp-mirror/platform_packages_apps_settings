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
import com.android.settings.core.lifecycle.ObservablePreferenceFragment;
import com.android.settings.overlay.FeatureFactory;

/**
 * Instrumented fragment that logs visibility state.
 */
public abstract class InstrumentedFragment extends ObservablePreferenceFragment
        implements Instrumentable {

    protected MetricsFeatureProvider mMetricsFeatureProvider;

    // metrics placeholder value. Only use this for development.
    protected final int PLACEHOLDER_METRIC = 10000;
    protected final int SYSTEM_CATEGORY_FRAGMENT = PLACEHOLDER_METRIC + 1;

    public InstrumentedFragment() {
        // Mixin that logs visibility change for activity.
        getLifecycle().addObserver(new VisibilityLoggerMixin(getMetricsCategory()));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }
}
