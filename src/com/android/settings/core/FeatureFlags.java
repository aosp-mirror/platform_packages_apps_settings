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

package com.android.settings.core;

/**
 * This class keeps track of all feature flags in Settings.
 */
public class FeatureFlags {
    public static final String AUDIO_SWITCHER_SETTINGS = "settings_audio_switcher";
    public static final String HEARING_AID_SETTINGS = "settings_bluetooth_hearing_aid";
    public static final String CONTROLLER_ENHANCEMENT = "settings_controller_loading_enhancement";
    public static final String CONDITIONAL_CARDS = "settings_conditionals";
    public static final String TETHER_ALL_IN_ONE = "settings_tether_all_in_one";
    public static final String CONTEXTUAL_HOME = "settings_contextual_home";
    public static final String SETTINGS_SEARCH_ALWAYS_EXPAND =
            "settings_search_always_expand";
}
