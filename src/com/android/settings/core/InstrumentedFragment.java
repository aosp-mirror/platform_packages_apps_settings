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

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.survey.SurveyMixin;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.core.lifecycle.ObservableFragment;

public abstract class InstrumentedFragment extends ObservableFragment implements Instrumentable {

    protected MetricsFeatureProvider mMetricsFeatureProvider;

    private VisibilityLoggerMixin mVisibilityLoggerMixin;

    @Override
    public void onAttach(Context context) {
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mVisibilityLoggerMixin = new VisibilityLoggerMixin(getMetricsCategory(),
                mMetricsFeatureProvider);
        // Mixin that logs visibility change for activity.
        getSettingsLifecycle().addObserver(mVisibilityLoggerMixin);
        getSettingsLifecycle().addObserver(new SurveyMixin(this, getClass().getSimpleName()));
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        mVisibilityLoggerMixin.setSourceMetricsCategory(getActivity());
        super.onResume();
    }
}
