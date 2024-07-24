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

package com.android.settings.development.snooplogger;

import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.SwitchPreferenceCompat;

/**
 * Bluetooth Snoop Logger Filters Preference
 */
public class SnoopLoggerFiltersPreference extends SwitchPreferenceCompat {

    private final String mKey;
    private static final String TAG = "SnoopLoggerFiltersPreference";
    private static final String SNOOP_LOG_FILTERS_PREFIX = "persist.bluetooth.snooplogfilter.";
    private static final String SNOOP_LOG_FILTERS_SUFFIX = ".enabled";

    public SnoopLoggerFiltersPreference(Context context, String key, String entry) {
        super(context);
        mKey = key;
        setKey(key);
        setTitle(entry);

        String filterProp = SNOOP_LOG_FILTERS_PREFIX.concat(mKey).concat(SNOOP_LOG_FILTERS_SUFFIX);

        boolean isFilterEnabled = SystemProperties.get(filterProp).equals("true");

        super.setChecked(isFilterEnabled);
    }

    @Override
    public void setChecked(boolean isChecked) {
        super.setChecked(isChecked);
        String filterProp = SNOOP_LOG_FILTERS_PREFIX.concat(mKey).concat(SNOOP_LOG_FILTERS_SUFFIX);
        SystemProperties.set(filterProp, isChecked ? "true" : "false");
    }
}
