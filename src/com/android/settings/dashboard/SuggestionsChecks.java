/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.dashboard;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import com.android.settings.Settings.ZenModeAutomationSuggestionActivity;
import com.android.settingslib.drawer.Tile;

import java.util.List;

/**
 * The Home of all stupidly dynamic Settings Suggestions checks.
 */
public class SuggestionsChecks {

    private final Context mContext;

    public SuggestionsChecks(Context context) {
        mContext = context;
    }

    public boolean isSuggestionComplete(Tile suggestion) {
        if (suggestion.intent.getComponent().getClassName().equals(
                ZenModeAutomationSuggestionActivity.class.getName())) {
            return hasEnabledZenAutoRules();
        }
        return false;
    }

    private boolean hasEnabledZenAutoRules() {
        List<AutomaticZenRule> zenRules = NotificationManager.from(mContext).getAutomaticZenRules();
        final int N = zenRules.size();
        for (int i = 0; i < N; i++) {
            if (zenRules.get(i).isEnabled()) {
                return true;
            }
        }
        return false;
    }

}
