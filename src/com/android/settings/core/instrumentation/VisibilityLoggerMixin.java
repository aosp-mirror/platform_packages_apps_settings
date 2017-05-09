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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnAttach;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import static com.android.settings.core.instrumentation.Instrumentable.METRICS_CATEGORY_UNKNOWN;

/**
 * Logs visibility change of a fragment.
 */
public class VisibilityLoggerMixin implements LifecycleObserver, OnResume, OnPause, OnAttach {

    private static final String TAG = "VisibilityLoggerMixin";

    private final int mMetricsCategory;

    private MetricsFeatureProvider mMetricsFeature;
    private int mSourceMetricsCategory = MetricsProto.MetricsEvent.VIEW_UNKNOWN;

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
        if (mMetricsFeature != null && mMetricsCategory != METRICS_CATEGORY_UNKNOWN) {
            mMetricsFeature.visible(null /* context */, mSourceMetricsCategory, mMetricsCategory);
        }
    }

    @Override
    public void onPause() {
        if (mMetricsFeature != null && mMetricsCategory != METRICS_CATEGORY_UNKNOWN) {
            mMetricsFeature.hidden(null /* context */, mMetricsCategory);
        }
    }

    /**
     * Sets source metrics category for this logger. Source is the caller that opened this UI.
     */
    public void setSourceMetricsCategory(Activity activity) {
        if (mSourceMetricsCategory != MetricsProto.MetricsEvent.VIEW_UNKNOWN || activity == null) {
            return;
        }
        final Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        mSourceMetricsCategory = intent.getIntExtra(SettingsActivity.EXTRA_SOURCE_METRICS_CATEGORY,
                MetricsProto.MetricsEvent.VIEW_UNKNOWN);
    }
}
