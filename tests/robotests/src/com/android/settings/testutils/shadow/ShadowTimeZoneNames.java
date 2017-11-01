/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import android.icu.text.TimeZoneNames;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * TimeZoneNames.getDisplayName tries to access files which doesn't exist for Robolectric. Stub it
 * out for a naive implementation.
 */
@Implements(TimeZoneNames.class)
public class ShadowTimeZoneNames {

    @Implementation
    public String getDisplayName(String tzID, TimeZoneNames.NameType type, long date) {
        return "[DisplayName]" + tzID;
    }
}
