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
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores suggestion related statistics.
 */
public class EventStore {

    public static final String TAG = "SuggestionEventStore";

    public static final String EVENT_SHOWN = "shown";
    public static final String EVENT_DISMISSED = "dismissed";
    public static final String EVENT_CLICKED = "clicked";
    public static final String METRIC_LAST_EVENT_TIME = "last_event_time";
    public static final String METRIC_COUNT = "count";

    private static final Set<String> EVENTS = new HashSet<String>(
            Arrays.asList(new String[] {EVENT_SHOWN, EVENT_DISMISSED, EVENT_CLICKED}));
    private static final Set<String> METRICS = new HashSet<String>(
            Arrays.asList(new String[] {METRIC_LAST_EVENT_TIME, METRIC_COUNT}));

    private final SharedPreferences mSharedPrefs;

    public EventStore(Context context) {
        mSharedPrefs = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
    }

    /**
     * Writes individual log events.
     * @param pkgName: Package for which this event is reported.
     * @param eventType: Type of event (one of {@link #EVENTS}).
     */
    public void writeEvent(String pkgName, String eventType) {
        if (!EVENTS.contains(eventType)) {
            Log.w(TAG, "Reported event type " + eventType + " is not a valid type!");
            return;
        }
        final String lastTimePrefKey = getPrefKey(pkgName, eventType, METRIC_LAST_EVENT_TIME);
        final String countPrefKey = getPrefKey(pkgName, eventType, METRIC_COUNT);
        writePref(lastTimePrefKey, System.currentTimeMillis());
        writePref(countPrefKey, readPref(countPrefKey, (long) 0) + 1);
    }

    /**
     * Reads metric of the the reported events (e.g., counts).
     * @param pkgName: Package for which this metric is queried.
     * @param eventType: Type of event (one of {@link #EVENTS}).
     * @param metricType: Type of the queried metric (one of {@link #METRICS}).
     * @return the corresponding metric.
     */
    public long readMetric(String pkgName, String eventType, String metricType) {
        if (!EVENTS.contains(eventType)) {
            Log.w(TAG, "Reported event type " + eventType + " is not a valid event!");
            return 0;
        } else if (!METRICS.contains(metricType)) {
            Log.w(TAG, "Required stat type + " + metricType + " is not a valid stat!");
            return 0;
        }
        return readPref(getPrefKey(pkgName, eventType, metricType), (long) 0);
    }

    private void writePref(String prefKey, long value) {
        mSharedPrefs.edit().putLong(prefKey, value).apply();
    }

    private long readPref(String prefKey, Long defaultValue) {
        return mSharedPrefs.getLong(prefKey, defaultValue);
    }

    private String getPrefKey(String pkgName, String eventType, String statType) {
        return new StringBuilder()
                .append("setting_suggestion_")
                .append(pkgName)
                .append("_")
                .append(eventType)
                .append("_")
                .append(statType)
                .toString();
    }
}
