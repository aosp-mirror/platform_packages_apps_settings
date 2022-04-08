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

package com.android.settings.fuelgauge.batterytip;

import android.content.Context;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.time.Duration;

/**
 * Class to store the policy for battery tips, which comes from
 * {@link Settings.Global}
 */
public class BatteryTipPolicy {
    public static final String TAG = "BatteryTipPolicy";

    private static final String KEY_BATTERY_TIP_ENABLED = "battery_tip_enabled";
    private static final String KEY_SUMMARY_ENABLED = "summary_enabled";
    private static final String KEY_BATTERY_SAVER_TIP_ENABLED = "battery_saver_tip_enabled";
    private static final String KEY_HIGH_USAGE_ENABLED = "high_usage_enabled";
    private static final String KEY_HIGH_USAGE_APP_COUNT = "high_usage_app_count";
    private static final String KEY_HIGH_USAGE_PERIOD_MS = "high_usage_period_ms";
    private static final String KEY_HIGH_USAGE_BATTERY_DRAINING = "high_usage_battery_draining";
    private static final String KEY_APP_RESTRICTION_ENABLED = "app_restriction_enabled";
    private static final String KEY_APP_RESTRICTION_ACTIVE_HOUR = "app_restriction_active_hour";
    private static final String KEY_REDUCED_BATTERY_ENABLED = "reduced_battery_enabled";
    private static final String KEY_REDUCED_BATTERY_PERCENT = "reduced_battery_percent";
    private static final String KEY_LOW_BATTERY_ENABLED = "low_battery_enabled";
    private static final String KEY_LOW_BATTERY_HOUR = "low_battery_hour";
    private static final String KEY_DATA_HISTORY_RETAIN_DAY = "data_history_retain_day";
    private static final String KEY_EXCESSIVE_BG_DRAIN_PERCENTAGE = "excessive_bg_drain_percentage";

    private static final String KEY_TEST_BATTERY_SAVER_TIP = "test_battery_saver_tip";
    private static final String KEY_TEST_HIGH_USAGE_TIP = "test_high_usage_tip";
    private static final String KEY_TEST_SMART_BATTERY_TIP = "test_smart_battery_tip";
    private static final String KEY_TEST_LOW_BATTERY_TIP = "test_low_battery_tip";

    /**
     * {@code true} if general battery tip is enabled
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_BATTERY_TIP_ENABLED
     */
    public final boolean batteryTipEnabled;

    /**
     * {@code true} if summary tip is enabled
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_SUMMARY_ENABLED
     */
    public final boolean summaryEnabled;

    /**
     * {@code true} if battery saver tip is enabled
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_BATTERY_SAVER_TIP_ENABLED
     */
    public final boolean batterySaverTipEnabled;

    /**
     * {@code true} if high usage tip is enabled
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_HIGH_USAGE_ENABLED
     */
    public final boolean highUsageEnabled;

    /**
     * The maximum number of apps shown in high usage
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_HIGH_USAGE_APP_COUNT
     */
    public final int highUsageAppCount;

    /**
     * The size of the window(milliseconds) for checking if the device is being heavily used
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_HIGH_USAGE_PERIOD_MS
     */
    public final long highUsagePeriodMs;

    /**
     * The battery draining threshold to detect whether device is heavily used.
     * If battery drains more than {@link #highUsageBatteryDraining} in last {@link
     * #highUsagePeriodMs}, treat device as heavily used.
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_HIGH_USAGE_BATTERY_DRAINING
     */
    public final int highUsageBatteryDraining;

    /**
     * {@code true} if app restriction tip is enabled
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_APP_RESTRICTION_ENABLED
     */
    public final boolean appRestrictionEnabled;

    /**
     * Period(hour) to show anomaly apps. If it is 24 hours, it means only show anomaly apps
     * happened in last 24 hours.
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_APP_RESTRICTION_ACTIVE_HOUR
     */
    public final int appRestrictionActiveHour;

    /**
     * {@code true} if reduced battery tip is enabled
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_REDUCED_BATTERY_ENABLED
     */
    public final boolean reducedBatteryEnabled;

    /**
     * The percentage of reduced battery to trigger the tip(e.g. 50%)
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_REDUCED_BATTERY_PERCENT
     */
    public final int reducedBatteryPercent;

    /**
     * {@code true} if low battery tip is enabled
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_LOW_BATTERY_ENABLED
     */
    public final boolean lowBatteryEnabled;

    /**
     * Remaining battery hour to trigger the tip(e.g. 16 hours)
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_LOW_BATTERY_HOUR
     */
    public final int lowBatteryHour;

    /**
     * TTL day for anomaly data stored in database
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_DATA_HISTORY_RETAIN_DAY
     */
    public final int dataHistoryRetainDay;

    /**
     * Battery drain percentage threshold for excessive background anomaly(i.e. 10%)
     *
     * This is an additional check for excessive background, to check whether battery drain
     * for an app is larger than x%
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_EXCESSIVE_BG_DRAIN_PERCENTAGE
     */
    public final int excessiveBgDrainPercentage;

    /**
     * {@code true} if we want to test battery saver tip.
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_TEST_BATTERY_SAVER_TIP
     */
    public final boolean testBatterySaverTip;

    /**
     * {@code true} if we want to test high usage tip.
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_TEST_HIGH_USAGE_TIP
     */
    public final boolean testHighUsageTip;

    /**
     * {@code true} if we want to test smart battery tip.
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_TEST_SMART_BATTERY_TIP
     */
    public final boolean testSmartBatteryTip;

    /**
     * {@code true} if we want to test low battery tip.
     *
     * @see Settings.Global#BATTERY_TIP_CONSTANTS
     * @see #KEY_TEST_LOW_BATTERY_TIP
     */
    public final boolean testLowBatteryTip;

    private final KeyValueListParser mParser;

    public BatteryTipPolicy(Context context) {
        this(context, new KeyValueListParser(','));
    }

    @VisibleForTesting
    BatteryTipPolicy(Context context, KeyValueListParser parser) {
        mParser = parser;
        final String value = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.BATTERY_TIP_CONSTANTS);

        try {
            mParser.setString(value);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Bad battery tip constants");
        }

        batteryTipEnabled = mParser.getBoolean(KEY_BATTERY_TIP_ENABLED, true);
        summaryEnabled = mParser.getBoolean(KEY_SUMMARY_ENABLED, true);
        batterySaverTipEnabled = mParser.getBoolean(KEY_BATTERY_SAVER_TIP_ENABLED, true);
        highUsageEnabled = mParser.getBoolean(KEY_HIGH_USAGE_ENABLED, true);
        highUsageAppCount = mParser.getInt(KEY_HIGH_USAGE_APP_COUNT, 3);
        highUsagePeriodMs = mParser.getLong(KEY_HIGH_USAGE_PERIOD_MS,
                Duration.ofHours(2).toMillis());
        highUsageBatteryDraining = mParser.getInt(KEY_HIGH_USAGE_BATTERY_DRAINING, 25);
        appRestrictionEnabled = mParser.getBoolean(KEY_APP_RESTRICTION_ENABLED, true);
        appRestrictionActiveHour = mParser.getInt(KEY_APP_RESTRICTION_ACTIVE_HOUR, 24);
        reducedBatteryEnabled = mParser.getBoolean(KEY_REDUCED_BATTERY_ENABLED, false);
        reducedBatteryPercent = mParser.getInt(KEY_REDUCED_BATTERY_PERCENT, 50);
        lowBatteryEnabled = mParser.getBoolean(KEY_LOW_BATTERY_ENABLED, true);
        lowBatteryHour = mParser.getInt(KEY_LOW_BATTERY_HOUR, 3);
        dataHistoryRetainDay = mParser.getInt(KEY_DATA_HISTORY_RETAIN_DAY, 30);
        excessiveBgDrainPercentage = mParser.getInt(KEY_EXCESSIVE_BG_DRAIN_PERCENTAGE, 10);

        testBatterySaverTip = mParser.getBoolean(KEY_TEST_BATTERY_SAVER_TIP, false);
        testHighUsageTip = mParser.getBoolean(KEY_TEST_HIGH_USAGE_TIP, false);
        testSmartBatteryTip = mParser.getBoolean(KEY_TEST_SMART_BATTERY_TIP, false);
        testLowBatteryTip = mParser.getBoolean(KEY_TEST_LOW_BATTERY_TIP, false);
    }

}
