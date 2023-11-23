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

package com.android.settings.core.instrumentation;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class SettingsMetricsFeatureProvider extends MetricsFeatureProvider {
    private static final String TAG = "SettingsMetricsFeature";

    @Override
    protected void installLogWriters() {
        mLoggerWriters.add(new StatsLogWriter());
        mLoggerWriters.add(new SettingsEventLogWriter());
    }

    /**
     * @deprecated Use {@link #action(int, int, int, String, int)} instead.
     */
    @Deprecated
    @Override
    public void action(Context context, int category, Pair<Integer, Object>... taggedData) {
        Log.w(TAG, "action(Pair<Integer, Object>... taggedData) is deprecated, "
                + "Use action(int, int, int, String, int) instead.");
        super.action(context, category, taggedData);
    }
}
