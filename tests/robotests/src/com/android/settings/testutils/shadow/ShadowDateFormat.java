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

import android.content.Context;
import android.text.format.DateFormat;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Locale;

@Implements(DateFormat.class)
public class ShadowDateFormat {

    @Implementation
    protected static java.text.DateFormat getDateFormat(Context context) {
        return new java.text.SimpleDateFormat("MMM-dd-yyyy", Locale.ROOT);
    }

    @Implementation
    protected static java.text.DateFormat getLongDateFormat(Context context) {
        return new java.text.SimpleDateFormat("MMMM dd, yyyy", Locale.ROOT);
    }

    @Implementation
    protected static java.text.DateFormat getTimeFormat(Context context) {
        return new java.text.SimpleDateFormat("HH:mm:ss", Locale.ROOT);
    }
}
