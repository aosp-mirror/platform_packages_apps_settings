/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development.storage;

import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;

import java.util.Locale;

class SharedDataUtils {
    static final String BLOB_KEY = "BLOB_KEY";

    static final int LEASE_VIEW_REQUEST_CODE = 8108;
    static final int LEASE_VIEW_RESULT_CODE_SUCCESS = 1;
    static final int LEASE_VIEW_RESULT_CODE_FAILURE = -1;

    private static final String BLOB_EXPIRY_PATTERN = "MMM dd, yyyy HH:mm:ss z";

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(BLOB_EXPIRY_PATTERN);
    private static final Calendar CALENDAR = Calendar.getInstance(
            TimeZone.getDefault(), Locale.getDefault());

    static String formatTime(long millis) {
        CALENDAR.setTimeInMillis(millis);
        return FORMATTER.format(CALENDAR.getTime());
    }

    static String formatSize(long sizeBytes) {
        final double sizeInMb = sizeBytes / (1024.0 * 1024.0);
        return String.format("%.2f MB", sizeInMb);
    }
}
