/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.core.instrumentation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Optional;

/** Class for calculating the elapsed time of the metrics. */
public class ElapsedTimeUtils {
    private static final String TAG = "ElapsedTimeUtils";
    private static final String ELAPSED_TIME_PREF_FILENAME = "elapsed_time_info";
    private static final String SUW_FINISHED_TIME_MS = "suw_finished_time_ms";
    private static Optional<Long> sSuwFinishedTimeStamp = Optional.empty();;
    @VisibleForTesting
    static final long DEFAULT_SETUP_TIME = -1L;

    /**
     * Keeps the timestamp after SetupWizard finished.
     *
     * @param timestamp The timestamp of the SetupWizard finished.
     */
    public static void storeSuwFinishedTimestamp(@NonNull Context context, long timestamp) {
        final SharedPreferences sharedPrefs = getSharedPrefs(context);
        if (!sharedPrefs.contains(SUW_FINISHED_TIME_MS)) {
            sSuwFinishedTimeStamp = Optional.of(timestamp);
            sharedPrefs.edit().putLong(SUW_FINISHED_TIME_MS, timestamp).apply();
        }
    }

    /**
     * Retrieves the preference value of SUW_FINISHED_TIME_MS and
     * assigns to sSuwFinishedTimeStamp.
     */
    public static void assignSuwFinishedTimeStamp(@NonNull Context context) {
        final SharedPreferences sharedPrefs = getSharedPrefs(context);
        if (sharedPrefs.contains(SUW_FINISHED_TIME_MS) && !sSuwFinishedTimeStamp.isPresent()) {
            sSuwFinishedTimeStamp = Optional.of(getSuwFinishedTimestamp(context));
        }
    }

    /**
     * Gets the elapsed time by (timestamp - time of SetupWizard finished).
     * @param timestamp The timestamp of the current time.
     * @return The elapsed time after device setup finished.
     */
    public static long getElapsedTime(long timestamp) {
        if (!sSuwFinishedTimeStamp.isPresent()) {
            Log.w(TAG, "getElapsedTime: sSuwFinishedTimeStamp is null");
            return DEFAULT_SETUP_TIME;
        }
        if (sSuwFinishedTimeStamp.get() != DEFAULT_SETUP_TIME) {
            final long elapsedTime = timestamp - sSuwFinishedTimeStamp.get();
            return elapsedTime > 0L ? elapsedTime : DEFAULT_SETUP_TIME;
        }
        return DEFAULT_SETUP_TIME;
    }

    @VisibleForTesting
    static long getSuwFinishedTimestamp(Context context) {
        return getSharedPrefs(context).getLong(SUW_FINISHED_TIME_MS, DEFAULT_SETUP_TIME);
    }

    private static SharedPreferences getSharedPrefs(Context context) {
        return context
            .getApplicationContext()
            .getSharedPreferences(ELAPSED_TIME_PREF_FILENAME, Context.MODE_PRIVATE);
    }

    private ElapsedTimeUtils() {}
}
