/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import android.annotation.StringDef;

/** Provides necessary value of language tag to Regional Preferences. */
public class ExtensionTypes {

    public static final String CALENDAR = "ca";
    public static final String FIRST_DAY_OF_WEEK = "fw";
    public static final String NUMBERING_SYSTEM = "nu";
    public static final String TEMPERATURE_UNIT = "mu";

    @StringDef({
            FIRST_DAY_OF_WEEK,
            CALENDAR,
            TEMPERATURE_UNIT,
            NUMBERING_SYSTEM
    })
    public @interface Values {}
}
