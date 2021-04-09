/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.fuelgauge;

import android.annotation.IntDef;
import android.content.ContentValues;
import android.os.BatteryConsumer;
import android.os.BatteryUsageStats;
import android.os.SystemBatteryConsumer;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserHandle;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** A utility class to convert data into another types. */
public final class ConvertUtils {
    private static final String TAG = "ConvertUtils";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
        new SimpleDateFormat("MMM dd,yyyy HH:mm:ss", Locale.ENGLISH);
    /** Invalid system battery consumer drain type. */
    public static final int INVALID_DRAIN_TYPE = -1;

    @IntDef(prefix = {"CONSUMER_TYPE"}, value = {
        CONSUMER_TYPE_UNKNOWN,
        CONSUMER_TYPE_UID_BATTERY,
        CONSUMER_TYPE_USER_BATTERY,
        CONSUMER_TYPE_SYSTEM_BATTERY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public static @interface ConsumerType {}

    public static final int CONSUMER_TYPE_UNKNOWN = 0;
    public static final int CONSUMER_TYPE_UID_BATTERY = 1;
    public static final int CONSUMER_TYPE_USER_BATTERY = 2;
    public static final int CONSUMER_TYPE_SYSTEM_BATTERY = 3;

    /** Gets consumer type from {@link BatteryConsumer}. */
    @ConsumerType
    public static int getConsumerType(BatteryConsumer consumer) {
        if (consumer instanceof UidBatteryConsumer) {
            return CONSUMER_TYPE_UID_BATTERY;
        } else if (consumer instanceof UserBatteryConsumer) {
            return CONSUMER_TYPE_USER_BATTERY;
        } else if (consumer instanceof SystemBatteryConsumer) {
            return CONSUMER_TYPE_SYSTEM_BATTERY;
        } else {
          return CONSUMER_TYPE_UNKNOWN;
        }
    }

    /** Gets battery drain type for {@link SystemBatteryConsumer}. */
    public static int getDrainType(BatteryConsumer consumer) {
        if (consumer instanceof SystemBatteryConsumer) {
            return ((SystemBatteryConsumer) consumer).getDrainType();
        }
        return INVALID_DRAIN_TYPE;
    }

    public static ContentValues convert(
            BatteryEntry entry,
            BatteryUsageStats batteryUsageStats,
            int batteryLevel,
            int batteryStatus,
            int batteryHealth,
            long timestamp) {
        final ContentValues values = new ContentValues();
        values.put("uid", Long.valueOf(entry.getUid()));
        values.put("userId",
            Long.valueOf(UserHandle.getUserId(entry.getUid())));
        values.put("appLabel", entry.getLabel());
        values.put("packageName", entry.getDefaultPackageName());
        values.put("isHidden", Boolean.valueOf(entry.isHidden()));
        values.put("timestamp", Long.valueOf(timestamp));
        values.put("zoneId", TimeZone.getDefault().getID());
        values.put("totalPower",
            Double.valueOf(batteryUsageStats.getConsumedPower()));
        values.put("consumePower", Double.valueOf(entry.getConsumedPower()));
        values.put("percentOfTotal", Double.valueOf(entry.percent));
        values.put("foregroundUsageTimeInMs",
            Long.valueOf(entry.getTimeInForegroundMs()));
        values.put("backgroundUsageTimeInMs",
            Long.valueOf(entry.getTimeInBackgroundMs()));
        values.put("drainType", getDrainType(entry.getBatteryConsumer()));
        values.put("consumerType", getConsumerType(entry.getBatteryConsumer()));
        values.put("batteryLevel", Integer.valueOf(batteryLevel));
        values.put("batteryStatus", Integer.valueOf(batteryStatus));
        values.put("batteryHealth", Integer.valueOf(batteryHealth));
        return values;
    }

    /** Converts UTC timestamp to human readable local time string. */
    public static String utcToLocalTime(long timestamp) {
        return SIMPLE_DATE_FORMAT.format(new Date(timestamp));
    }

    private ConvertUtils() {}
}
