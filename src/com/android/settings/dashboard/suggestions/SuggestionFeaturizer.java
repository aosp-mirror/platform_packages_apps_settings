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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates a set of interaction features (i.e., metrics) to represent each setting suggestion. These
 * features currently include normalized time from previous events (shown, dismissed and clicked)
 * for any particular suggestion and also counts of these events. These features are used as signals
 * to find the best ranking for suggestion items.
 */
public class SuggestionFeaturizer {

    // Key of the features used for ranking.
    public static final String FEATURE_IS_SHOWN = "is_shown";
    public static final String FEATURE_IS_DISMISSED = "is_dismissed";
    public static final String FEATURE_IS_CLICKED = "is_clicked";
    public static final String FEATURE_TIME_FROM_LAST_SHOWN = "time_from_last_shown";
    public static final String FEATURE_TIME_FROM_LAST_DISMISSED = "time_from_last_dismissed";
    public static final String FEATURE_TIME_FROM_LAST_CLICKED = "time_from_last_clicked";
    public static final String FEATURE_SHOWN_COUNT = "shown_count";
    public static final String FEATURE_DISMISSED_COUNT = "dismissed_count";
    public static final String FEATURE_CLICKED_COUNT = "clicked_count";

    // The following numbers are estimated from histograms.
    public static final double TIME_NORMALIZATION_FACTOR = 2e10;
    public static final double COUNT_NORMALIZATION_FACTOR = 500;

    private final EventStore mEventStore;

    /**
     * Constructor
     *
     * @param eventStore An instance of {@code EventStore} which maintains the recorded suggestion
     * events.
     */
    public SuggestionFeaturizer(EventStore eventStore) {
        mEventStore = eventStore;
    }

    /**
     * Extracts the features for each package name.
     *
     * @param pkgNames: List of package names for which features are queried.
     * @return A Map containing the features, keyed by the package names. Each map value contains
     * another map with key-value pairs of the features.
     */
    public Map<String, Map<String, Double>> featurize(List<String> pkgNames) {
        Map<String, Map<String, Double>> features = new HashMap<>();
        Long curTimeMs = System.currentTimeMillis();
        for (String pkgName : pkgNames) {
            Map<String, Double> featureMap = new HashMap<>();
            features.put(pkgName, featureMap);
            Long lastShownTime = mEventStore
                    .readMetric(pkgName, EventStore.EVENT_SHOWN, EventStore.METRIC_LAST_EVENT_TIME);
            Long lastDismissedTime = mEventStore.readMetric(pkgName, EventStore.EVENT_DISMISSED,
                    EventStore.METRIC_LAST_EVENT_TIME);
            Long lastClickedTime = mEventStore.readMetric(pkgName, EventStore.EVENT_CLICKED,
                    EventStore.METRIC_LAST_EVENT_TIME);
            featureMap.put(FEATURE_IS_SHOWN, booleanToDouble(lastShownTime > 0));
            featureMap.put(FEATURE_IS_DISMISSED, booleanToDouble(lastDismissedTime > 0));
            featureMap.put(FEATURE_IS_CLICKED, booleanToDouble(lastClickedTime > 0));
            featureMap.put(FEATURE_TIME_FROM_LAST_SHOWN,
                    normalizedTimeDiff(curTimeMs, lastShownTime));
            featureMap.put(FEATURE_TIME_FROM_LAST_DISMISSED,
                    normalizedTimeDiff(curTimeMs, lastDismissedTime));
            featureMap.put(FEATURE_TIME_FROM_LAST_CLICKED,
                    normalizedTimeDiff(curTimeMs, lastClickedTime));
            featureMap.put(FEATURE_SHOWN_COUNT, normalizedCount(mEventStore
                    .readMetric(pkgName, EventStore.EVENT_SHOWN, EventStore.METRIC_COUNT)));
            featureMap.put(FEATURE_DISMISSED_COUNT, normalizedCount(mEventStore
                    .readMetric(pkgName, EventStore.EVENT_DISMISSED, EventStore.METRIC_COUNT)));
            featureMap.put(FEATURE_CLICKED_COUNT, normalizedCount(mEventStore
                    .readMetric(pkgName, EventStore.EVENT_CLICKED, EventStore.METRIC_COUNT)));
        }
        return features;
    }

    private static double booleanToDouble(boolean bool) {
        return bool ? 1 : 0;
    }

    private static double normalizedTimeDiff(long curTimeMs, long preTimeMs) {
        return Math.min(1, (curTimeMs - preTimeMs) / TIME_NORMALIZATION_FACTOR);
    }

    private static double normalizedCount(long count) {
        return Math.min(1, count / COUNT_NORMALIZATION_FACTOR);
    }
}
