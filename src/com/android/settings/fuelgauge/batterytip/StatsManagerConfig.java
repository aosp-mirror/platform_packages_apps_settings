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

/**
 * This class provides all the configs needed if we want to use {@link android.app.StatsManager}
 */
public class StatsManagerConfig {
    /**
     * The key that represents the anomaly config.
     * This value is used in {@link android.app.StatsManager#addConfiguration(long, byte[])}
     */
    public static final long ANOMALY_CONFIG_KEY = 1;

    /**
     * The key that represents subscriber, which is settings app.
     */
    public static final long SUBSCRIBER_ID = 1;
}
