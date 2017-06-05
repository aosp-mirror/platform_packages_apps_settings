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

package com.android.settings.search.ranking;

import android.util.Pair;

import java.util.List;

public interface SearchResultsRankerCallback {

    /**
     * Called when ranker provides the ranking scores.
     * @param searchRankingScores Ordered List of Pairs of String and Float corresponding to
     *                            stableIds and ranking scores. The list must be descendingly
     *                            ordered based on scores.
     */
    public void onRankingScoresAvailable(List<Pair<String, Float>> searchRankingScores);

    /**
     * Called when for any reason ranker fails, which notifies the client to proceed
     * without ranking results.
     */
    public void onRankingFailed();
}
