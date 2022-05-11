/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.search;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;

import com.android.settings.SettingsApplication;
import com.android.settings.core.FeatureFlags;
import com.android.settings.homepage.SettingsHomepageActivity;

/**
 * A broadcast receiver that monitors the search state to show/hide the menu highlight
 */
public class SearchStateReceiver extends BroadcastReceiver {

    private static final String TAG = "SearchStateReceiver";
    private static final String ACTION_SEARCH_START = "com.android.settings.SEARCH_START";
    private static final String ACTION_SEARCH_EXIT = "com.android.settings.SEARCH_EXIT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (FeatureFlagUtils.isEnabled(context, FeatureFlags.SETTINGS_SEARCH_ALWAYS_EXPAND)) {
            // Not needed to show/hide the highlight when search is full screen
            return;
        }

        if (intent == null) {
            Log.w(TAG, "Null intent");
            return;
        }

        final SettingsHomepageActivity homeActivity =
                ((SettingsApplication) context.getApplicationContext()).getHomeActivity();
        if (homeActivity == null) {
            return;
        }

        final String action = intent.getAction();
        Log.d(TAG, "action: " + action);
        if (TextUtils.equals(ACTION_SEARCH_START, action)) {
            homeActivity.getMainFragment().setMenuHighlightShowed(false);
        } else if (TextUtils.equals(ACTION_SEARCH_EXIT, action)) {
            homeActivity.getMainFragment().setMenuHighlightShowed(true);
        }
    }
}
