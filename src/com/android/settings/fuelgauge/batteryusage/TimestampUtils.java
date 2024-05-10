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

package com.android.settings.fuelgauge.batteryusage;

import java.time.Duration;
import java.util.Calendar;

/** A utility class for timestamp operations. */
final class TimestampUtils {

    static long getNextHourTimestamp(final long timestamp) {
        final Calendar calendar = getSharpHourCalendar(timestamp);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        return calendar.getTimeInMillis();
    }

    static long getNextEvenHourTimestamp(final long timestamp) {
        final Calendar calendar = getSharpHourCalendar(timestamp);
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.add(Calendar.HOUR_OF_DAY, hour % 2 == 0 ? 2 : 1);
        return calendar.getTimeInMillis();
    }

    static long getLastEvenHourTimestamp(final long timestamp) {
        final Calendar calendar = getSharpHourCalendar(timestamp);
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.add(Calendar.HOUR_OF_DAY, hour % 2 == 0 ? 0 : -1);
        return calendar.getTimeInMillis();
    }

    static long getNextDayTimestamp(final long timestamp) {
        final Calendar calendar = getSharpHourCalendar(timestamp);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTimeInMillis();
    }

    static long getSeconds(final long timeInMs) {
        return Duration.ofMillis(timeInMs).getSeconds();
    }

    static boolean isMidnight(final long timestamp) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar.get(Calendar.HOUR_OF_DAY) == 0
                && calendar.get(Calendar.MINUTE) == 0
                && calendar.get(Calendar.SECOND) == 0
                && calendar.get(Calendar.MILLISECOND) == 0;
    }

    private static Calendar getSharpHourCalendar(final long timestamp) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}
