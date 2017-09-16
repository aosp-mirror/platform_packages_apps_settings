/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.dashboard.suggestions;

import android.content.ComponentName;
import android.content.Context;
import android.service.settings.suggestions.Suggestion;
import android.support.annotation.VisibleForTesting;
import android.util.FeatureFlagUtils;
import android.util.Log;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

/**
 * Manages IPC communication to SettingsIntelligence for suggestion related services.
 */
public class SuggestionControllerMixin implements SuggestionController.ServiceConnectionListener,
        LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    static final String FEATURE_FLAG = "new_settings_suggestion";
    private static final String TAG = "SuggestionCtrlMixin";
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final SuggestionController mSuggestionController;

    public static boolean isEnabled() {
        return FeatureFlagUtils.isEnabled(FEATURE_FLAG);
    }

    public SuggestionControllerMixin(Context context, Lifecycle lifecycle) {
        mContext = context.getApplicationContext();
        mSuggestionController = new SuggestionController(context,
                new ComponentName(
                        "com.android.settings.intelligence",
                        "com.android.settings.intelligence.suggestions.SuggestionService"),
                this /* serviceConnectionListener */);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onStart() {
        if (!isEnabled()) {
            Log.w(TAG, "Feature not enabled, skipping");
            return;
        }
        mSuggestionController.start();
    }

    @Override
    public void onStop() {
        mSuggestionController.stop();
    }

    @Override
    public void onServiceConnected() {
        // TODO: Call API to get data from a loader instead of in current thread.
        final List<Suggestion> data = mSuggestionController.getSuggestions();
        if (DEBUG) {
            Log.d(TAG, "data size " + (data == null ? 0 : data.size()));
        }
    }

    @Override
    public void onServiceDisconnected() {
        if (DEBUG) {
            Log.d(TAG, "SuggestionService disconnected");
        }
    }
}
