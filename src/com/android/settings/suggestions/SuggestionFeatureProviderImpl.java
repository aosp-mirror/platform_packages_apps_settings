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

package com.android.settings.suggestions;

import android.content.Context;

import com.android.settingslib.drawer.Tile;

import java.util.List;

public class SuggestionFeatureProviderImpl implements SuggestionFeatureProvider {

    private final SuggestionRanker mSuggestionRanker;

    @Override
    public boolean isSmartSuggestionEnabled(Context context) {
        return false;
    }

    @Override
    public boolean isPresent(String className) {
        return false;
    }

    @Override
    public boolean isSuggestionCompleted(Context context) {
        return false;
    }


    public SuggestionFeatureProviderImpl(Context context) {
        mSuggestionRanker = new SuggestionRanker(
                new SuggestionFeaturizer(new EventStore(context.getApplicationContext())));
    }

    @Override
    public void rankSuggestions(final List<Tile> suggestions, List<String> suggestionIds) {
        mSuggestionRanker.rankSuggestions(suggestions, suggestionIds);
    }

}
