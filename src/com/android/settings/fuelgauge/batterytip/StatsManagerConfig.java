/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterytip;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class provides all the configs needed if we want to use {@link android.app.StatsManager}
 */
public class StatsManagerConfig {
    /**
     * The key that represents the anomaly config.
     * This value is used in {@link android.app.StatsManager#addConfig(long, byte[])}
     */
    public static final long ANOMALY_CONFIG_KEY = 1;

    /**
     * The key that represents subscriber, which is settings app.
     */
    public static final long SUBSCRIBER_ID = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AnomalyType.NULL,
            AnomalyType.UNKNOWN_REASON,
            AnomalyType.EXCESSIVE_WAKELOCK_ALL_SCREEN_OFF,
            AnomalyType.EXCESSIVE_WAKEUPS_IN_BACKGROUND,
            AnomalyType.EXCESSIVE_UNOPTIMIZED_BLE_SCAN,
            AnomalyType.EXCESSIVE_BACKGROUND_SERVICE,
            AnomalyType.EXCESSIVE_WIFI_SCAN,
            AnomalyType.EXCESSIVE_FLASH_WRITES,
            AnomalyType.EXCESSIVE_MEMORY_IN_BACKGROUND,
            AnomalyType.EXCESSIVE_DAVEY_RATE,
            AnomalyType.EXCESSIVE_JANKY_FRAMES,
            AnomalyType.SLOW_COLD_START_TIME,
            AnomalyType.SLOW_HOT_START_TIME,
            AnomalyType.SLOW_WARM_START_TIME,
            AnomalyType.EXCESSIVE_BACKGROUND_SYNCS,
            AnomalyType.EXCESSIVE_GPS_SCANS_IN_BACKGROUND,
            AnomalyType.EXCESSIVE_JOB_SCHEDULING,
            AnomalyType.EXCESSIVE_MOBILE_NETWORK_IN_BACKGROUND,
            AnomalyType.EXCESSIVE_WIFI_LOCK_TIME,
            AnomalyType.JOB_TIMED_OUT,
            AnomalyType.LONG_UNOPTIMIZED_BLE_SCAN,
            AnomalyType.BACKGROUND_ANR,
            AnomalyType.BACKGROUND_CRASH_RATE,
            AnomalyType.EXCESSIVE_ANR_LOOPING,
            AnomalyType.EXCESSIVE_ANRS,
            AnomalyType.EXCESSIVE_CRASH_RATE,
            AnomalyType.EXCESSIVE_CRASH_LOOPING,
            AnomalyType.NUMBER_OF_OPEN_FILES,
            AnomalyType.EXCESSIVE_CAMERA_USAGE_IN_BACKGROUND,
            AnomalyType.EXCESSIVE_CONTACT_ACCESS,
            AnomalyType.EXCESSIVE_AUDIO_IN_BACKGROUND,
            AnomalyType.EXCESSIVE_CRASH_ANR_IN_BACKGROUND,
            AnomalyType.BATTERY_DRAIN_FROM_UNUSED_APP,
    })
    public @interface AnomalyType {
        /**
         * This represents an error condition in the anomaly detection.
         */
        int NULL = -1;

        /**
         * The anomaly type does not match any other defined type.
         */
        int UNKNOWN_REASON = 0;

        /**
         * The application held a partial (screen off) wake lock for a period of time that
         * exceeded the threshold with the screen off when not charging.
         */
        int EXCESSIVE_WAKELOCK_ALL_SCREEN_OFF = 1;

        /**
         * The application exceeded the maximum number of wakeups while in the background
         * when not charging.
         */
        int EXCESSIVE_WAKEUPS_IN_BACKGROUND = 2;

        /**
         * The application did unoptimized Bluetooth scans too frequently when not charging.
         */
        int EXCESSIVE_UNOPTIMIZED_BLE_SCAN = 3;

        /**
         * The application ran in the background for a period of time that exceeded the
         * threshold.
         */
        int EXCESSIVE_BACKGROUND_SERVICE = 4;

        /**
         * The application exceeded the maximum number of wifi scans when not charging.
         */
        int EXCESSIVE_WIFI_SCAN = 5;

        /**
         * The application exceed the maximum number of flash writes
         */
        int EXCESSIVE_FLASH_WRITES = 6;

        /**
         * The application used more than the maximum memory, while not spending any time
         * in the foreground.
         */
        int EXCESSIVE_MEMORY_IN_BACKGROUND = 7;

        /**
         * The application exceeded the maximum percentage of frames with a render rate of
         * greater than 700ms.
         */
        int EXCESSIVE_DAVEY_RATE = 8;

        /**
         * The application exceeded the maximum percentage of frames with a render rate
         * greater than 16ms.
         */
        int EXCESSIVE_JANKY_FRAMES = 9;

        /**
         * The application exceeded the maximum cold start time - the app has not been
         * launched since last system start, died or was killed.
         */
        int SLOW_COLD_START_TIME = 10;

        /**
         * The application exceeded the maximum hot start time - the app and activity are
         * already in memory.
         */
        int SLOW_HOT_START_TIME = 11;

        /**
         * The application exceeded the maximum warm start time - the app was already in
         * memory but the activity wasn’t created yet or was removed from memory.
         */
        int SLOW_WARM_START_TIME = 12;

        /**
         * The application exceeded the maximum number of syncs while in the background.
         */
        int EXCESSIVE_BACKGROUND_SYNCS = 13;

        /**
         * The application exceeded the maximum number of gps scans while in the background.
         */
        int EXCESSIVE_GPS_SCANS_IN_BACKGROUND = 14;

        /**
         * The application scheduled more than the maximum number of jobs while not charging.
         */
        int EXCESSIVE_JOB_SCHEDULING = 15;

        /**
         * The application exceeded the maximum amount of mobile network traffic while in
         * the background.
         */
        int EXCESSIVE_MOBILE_NETWORK_IN_BACKGROUND = 16;

        /**
         * The application held the WiFi lock for more than the maximum amount of time while
         * not charging.
         */
        int EXCESSIVE_WIFI_LOCK_TIME = 17;

        /**
         * The application scheduled a job that ran longer than the maximum amount of time.
         */
        int JOB_TIMED_OUT = 18;

        /**
         * The application did an unoptimized Bluetooth scan that exceeded the maximum
         * time while in the background.
         */
        int LONG_UNOPTIMIZED_BLE_SCAN = 19;

        /**
         * The application exceeded the maximum ANR rate while in the background.
         */
        int BACKGROUND_ANR = 20;

        /**
         * The application exceeded the maximum crash rate while in the background.
         */
        int BACKGROUND_CRASH_RATE = 21;

        /**
         * The application exceeded the maximum ANR-looping rate.
         */
        int EXCESSIVE_ANR_LOOPING = 22;

        /**
         * The application exceeded the maximum ANR rate.
         */
        int EXCESSIVE_ANRS = 23;

        /**
         * The application exceeded the maximum crash rate.
         */
        int EXCESSIVE_CRASH_RATE = 24;

        /**
         * The application exceeded the maximum crash-looping rate.
         */
        int EXCESSIVE_CRASH_LOOPING = 25;

        /**
         * The application crashed because no more file descriptors were available.
         */
        int NUMBER_OF_OPEN_FILES = 26;

        /**
         * The application used an excessive amount of CPU while in a
         * background process state.
         */
        int EXCESSIVE_CPU_USAGE_IN_BACKGROUND = 27;

        /**
         * The application kept the camera open for an excessive amount
         * of time while in a bckground process state.
         */
        int EXCESSIVE_CAMERA_USAGE_IN_BACKGROUND = 28;

        /**
         * The application has accessed the contacts content provider an
         * excessive amount.
         */
        int EXCESSIVE_CONTACT_ACCESS = 29;

        /**
         * The application has played too much audio while in a background
         * process state.
         */
        int EXCESSIVE_AUDIO_IN_BACKGROUND = 30;

        /**
         * The application has crashed or ANRed too many times while in a
         * background process state.
         */
        int EXCESSIVE_CRASH_ANR_IN_BACKGROUND = 31;

        /**
         * An application which has not been used by the user recently
         * was detected to cause an excessive amount of battery drain.
         */
        int BATTERY_DRAIN_FROM_UNUSED_APP = 32;
    }

}
