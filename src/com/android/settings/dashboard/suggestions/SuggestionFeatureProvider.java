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
 * limitations under the License
 */

package com.android.settings.dashboard.suggestions;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** Interface should be implemented if you have added new suggestions */
public interface SuggestionFeatureProvider {

    /**
     * Whether or not the whole suggestion feature is enabled.
     */
    boolean isSuggestionEnabled(Context context);

    /**
     * Returns the component name for SuggestionService.
     */
    ComponentName getSuggestionServiceComponent();

    /** Return true if the suggestion has already been completed and does not need to be shown */
    boolean isSuggestionComplete(Context context, @NonNull ComponentName suggestion);

    /**
     * Returns the {@link SharedPreferences} that holds metadata for suggestions.
     */
    SharedPreferences getSharedPrefs(Context context);

    /**
     * Returns the class of {@link Fragment} that supports contextual suggestion.
     *
     * @deprecated - use {@link SuggestionFeatureProvider#getSuggestionFragment()} instead.
     */
    @Deprecated
    @Nullable
    default Class<? extends Fragment> getContextualSuggestionFragment() {
        return null;
    }

    /**
     * Returns the class of {@link Fragment} that provides the UI for Suggestions.
     */
    @Nullable
    default Class<? extends Fragment> getSuggestionFragment() {
        return getContextualSuggestionFragment();
    }
}
