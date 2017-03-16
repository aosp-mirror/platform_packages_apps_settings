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

import android.content.Context;

import com.android.settingslib.SuggestionParser;
import com.android.settingslib.drawer.Tile;

import java.util.List;

/** Interface should be implemented if you have added new suggestions */
public interface SuggestionFeatureProvider {

    /**
     * Returns true if smart suggestion should be used instead of xml based SuggestionParser.
     */
    boolean isSmartSuggestionEnabled(Context context);

    /** Return true if className is the name of a class of one of your newly added suggestion. */
    boolean isPresent(String className);

    /** Return true if the suggestion has already been completed and does not need to be shown */
    boolean isSuggestionCompleted(Context context);

    /**
     * Ranks the list of suggestions in place.
     *
     * @param suggestions   List of suggestion Tiles
     * @param suggestionIds List of suggestion ids corresponding to the suggestion tiles.
     */
    void rankSuggestions(final List<Tile> suggestions, List<String> suggestionIds);

    /**
     * Dismisses a suggestion.
     */
    void dismissSuggestion(Context context, SuggestionParser parser, Tile suggestion);

    /**
     * Returns an identifier for the suggestion
     */
    String getSuggestionIdentifier(Context context, Tile suggestion);
}
