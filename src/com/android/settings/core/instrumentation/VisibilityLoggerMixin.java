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

import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.core.lifecycle.events.OnPause;
import com.android.settings.core.lifecycle.events.OnResume;

/**
 * Logs visibility change of a fragment.
 */
public class VisibilityLoggerMixin implements LifecycleObserver, OnResume, OnPause {

    private final int mMetricsCategory;
    private final LogWriter mLogWriter;

    public VisibilityLoggerMixin(int metricsCategory) {
        this(metricsCategory, MetricsFactory.get().getLogger());
    }

    public VisibilityLoggerMixin(int metricsCategory, LogWriter logWriter) {
        mMetricsCategory = metricsCategory;
        mLogWriter = logWriter;
    }

    @Override
    public void onResume() {
        mLogWriter.visible(null /* context */, mMetricsCategory);
    }

    @Override
    public void onPause() {
        mLogWriter.hidden(null /* context */, mMetricsCategory);
    }
}
