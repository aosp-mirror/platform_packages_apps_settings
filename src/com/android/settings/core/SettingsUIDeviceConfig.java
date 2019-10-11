/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.core;

/**
 * Class to store keys for settings related features, which comes from
 * {@link android.provider.DeviceConfig}
 */
public class SettingsUIDeviceConfig {
    /**
     * {@code true} if slice settings is enabled in BT device detail page
     */
    public static final String BT_SLICE_SETTINGS_ENABLED = "bt_slice_settings_enabled";
    /**
     * {@code true} if advanced header is enabled in BT device detail page
     */
    public static final String BT_ADVANCED_HEADER_ENABLED = "bt_advanced_header_enabled";
    /**
     * {@code true} if near by device suggestion is enabled in connected device page
     */
    public static final String BT_NEAR_BY_SUGGESTION_ENABLED = "bt_near_by_suggestion_enabled";

    /**
     * {@code true} whether or not event_log for generic actions is enabled. Default is true.
     */
    public static final String GENERIC_EVENT_LOGGING_ENABLED = "event_logging_enabled";
}
