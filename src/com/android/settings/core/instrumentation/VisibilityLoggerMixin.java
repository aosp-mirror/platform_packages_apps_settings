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

package com.android.settings.core.instrumentation;

import android.content.Context;

import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.core.lifecycle.events.OnAttach;
import com.android.settings.core.lifecycle.events.OnPause;
import com.android.settings.core.lifecycle.events.OnResume;
import com.android.settings.overlay.FeatureFactory;

/**
 * Logs visibility change of a fragment.
 */
public class VisibilityLoggerMixin implements LifecycleObserver, OnResume, OnPause, OnAttach {

    private final int mMetricsCategory;

    private MetricsFeatureProvider mMetricsFeature;

    public VisibilityLoggerMixin(int metricsCategory) {
        // MetricsFeature will be set during onAttach.
        this(metricsCategory, null /* metricsFeature */);
    }

    public VisibilityLoggerMixin(int metricsCategory, MetricsFeatureProvider metricsFeature) {
        mMetricsCategory = metricsCategory;
        mMetricsFeature = metricsFeature;
    }

    @Override
    public void onAttach(Context context) {
        mMetricsFeature = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public void onResume() {
        if (mMetricsFeature != null) {
            mMetricsFeature.visible(null /* context */, mMetricsCategory);
        }
    }

    @Override
    public void onPause() {
        if (mMetricsFeature != null) {
            mMetricsFeature.hidden(null /* context */, mMetricsCategory);
        }
    }
}
