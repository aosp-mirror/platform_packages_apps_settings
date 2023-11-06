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

package com.android.settings.fuelgauge;

import static com.android.settings.fuelgauge.BatteryUtils.formatElapsedTimeWithoutComma;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class PowerUsageTimeController extends BasePreferenceController {
    private static final String TAG = "PowerUsageTimeController";

    private static final String KEY_POWER_USAGE_TIME = "battery_usage_time_category";
    private static final String KEY_SCREEN_TIME_PREF = "battery_usage_screen_time";
    private static final String KEY_BACKGROUND_TIME_PREF = "battery_usage_background_time";

    @VisibleForTesting PreferenceCategory mPowerUsageTimeCategory;
    @VisibleForTesting PowerUsageTimePreference mScreenTimePreference;
    @VisibleForTesting PowerUsageTimePreference mBackgroundTimePreference;

    public PowerUsageTimeController(Context context) {
        super(context, KEY_POWER_USAGE_TIME);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPowerUsageTimeCategory = screen.findPreference(KEY_POWER_USAGE_TIME);
        mScreenTimePreference = screen.findPreference(KEY_SCREEN_TIME_PREF);
        mBackgroundTimePreference = screen.findPreference(KEY_BACKGROUND_TIME_PREF);
        mPowerUsageTimeCategory.setVisible(false);
    }

    void handleScreenTimeUpdated(
            final String slotTime,
            final long screenOnTimeInMs,
            final long backgroundTimeInMs,
            final String anomalyHintPrefKey,
            final String anomalyHintText) {
        final boolean isShowScreenOnTime =
                showTimePreference(
                        mScreenTimePreference,
                        R.string.power_usage_detail_screen_time,
                        screenOnTimeInMs,
                        anomalyHintPrefKey,
                        anomalyHintText);
        final boolean isShowBackgroundTime =
                showTimePreference(
                        mBackgroundTimePreference,
                        R.string.power_usage_detail_background_time,
                        backgroundTimeInMs,
                        anomalyHintPrefKey,
                        anomalyHintText);
        if (isShowScreenOnTime || isShowBackgroundTime) {
            showCategoryTitle(slotTime);
        }
    }

    boolean showTimePreference(
            PowerUsageTimePreference preference,
            int titleResId,
            long summaryTimeMs,
            String anomalyHintKey,
            String anomalyHintText) {
        if (preference == null
                || (summaryTimeMs == 0 && !TextUtils.equals(anomalyHintKey, preference.getKey()))) {
            return false;
        }
        preference.setTimeTitle(mContext.getString(titleResId));
        preference.setTimeSummary(getPowerUsageTimeInfo(summaryTimeMs));
        if (TextUtils.equals(anomalyHintKey, preference.getKey())) {
            preference.setAnomalyHint(anomalyHintText);
        }
        preference.setVisible(true);
        return true;
    }

    private CharSequence getPowerUsageTimeInfo(long timeInMs) {
        if (timeInMs < DateUtils.MINUTE_IN_MILLIS) {
            return mContext.getString(R.string.power_usage_time_less_than_one_minute);
        }
        return formatElapsedTimeWithoutComma(
                mContext,
                (double) timeInMs,
                /* withSeconds= */ false,
                /* collapseTimeUnit= */ false);
    }

    @VisibleForTesting
    void showCategoryTitle(String slotTimestamp) {
        mPowerUsageTimeCategory.setTitle(
                slotTimestamp == null
                        ? mContext.getString(R.string.battery_app_usage)
                        : mContext.getString(R.string.battery_app_usage_for, slotTimestamp));
        mPowerUsageTimeCategory.setVisible(true);
    }
}
