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

import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;

import com.android.internal.logging.MetricsLogger;

/**
 * Instrumented fragment that logs visibility state.
 */
public abstract class InstrumentedFragment extends PreferenceFragment {
    // Declare new temporary categories here, starting after this value.
    public static final int UNDECLARED = 100000;

    // Used by PreferenceActivity for the dummy fragment it adds, no useful data here.
    public static final int PREFERENCE_ACTIVITY_FRAGMENT = UNDECLARED + 1;

    /**
     * Declare the view of this category.
     *
     * Categories are defined in {@link com.android.internal.logging.MetricsProto.MetricsEvent}
     * or if there is no relevant existing category you may define one in
     * {@link com.android.settings.InstrumentedFragment}.
     */
    protected abstract int getMetricsCategory();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onResume() {
        super.onResume();
        MetricsLogger.visible(getActivity(), getMetricsCategory());
    }

    @Override
    public void onPause() {
        super.onPause();
        MetricsLogger.hidden(getActivity(), getMetricsCategory());
    }
}
