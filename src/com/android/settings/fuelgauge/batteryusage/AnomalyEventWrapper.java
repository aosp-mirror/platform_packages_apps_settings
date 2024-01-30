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

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.SubSettingLauncher;

import java.util.function.Function;

final class AnomalyEventWrapper {
    private static final String TAG = "AnomalyEventWrapper";

    private final Context mContext;
    private final PowerAnomalyEvent mPowerAnomalyEvent;

    private final int mCardStyleId;
    private final int mResourceIndex;

    private SubSettingLauncher mSubSettingLauncher = null;
    private Pair<Integer, Integer> mHighlightSlotPair = null;
    private BatteryDiffEntry mRelatedBatteryDiffEntry = null;

    AnomalyEventWrapper(Context context, PowerAnomalyEvent powerAnomalyEvent) {
        mContext = context;
        mPowerAnomalyEvent = powerAnomalyEvent;
        // Set basic battery tips card info
        mCardStyleId = mPowerAnomalyEvent.getType().getNumber();
        mResourceIndex = mPowerAnomalyEvent.getKey().getNumber();
    }

    private <T> T getInfo(
            Function<WarningBannerInfo, T> warningBannerInfoSupplier,
            Function<WarningItemInfo, T> warningItemInfoSupplier) {
        if (warningBannerInfoSupplier != null && mPowerAnomalyEvent.hasWarningBannerInfo()) {
            return warningBannerInfoSupplier.apply(mPowerAnomalyEvent.getWarningBannerInfo());
        } else if (warningItemInfoSupplier != null && mPowerAnomalyEvent.hasWarningItemInfo()) {
            return warningItemInfoSupplier.apply(mPowerAnomalyEvent.getWarningItemInfo());
        }
        return null;
    }

    private int getResourceId(int resourceId, int resourceIndex, String defType) {
        final String key = getStringFromArrayResource(resourceId, resourceIndex);
        return TextUtils.isEmpty(key)
                ? 0
                : mContext.getResources().getIdentifier(key, defType, mContext.getPackageName());
    }

    private String getString(
            Function<WarningBannerInfo, String> warningBannerInfoSupplier,
            Function<WarningItemInfo, String> warningItemInfoSupplier,
            int resourceId,
            int resourceIndex) {
        final String string = getInfo(warningBannerInfoSupplier, warningItemInfoSupplier);
        return (!TextUtils.isEmpty(string) || resourceId <= 0)
                ? string
                : getStringFromArrayResource(resourceId, resourceIndex);
    }

    private String getStringFromArrayResource(int resourceId, int resourceIndex) {
        if (resourceId <= 0 || resourceIndex < 0) {
            return null;
        }
        final String[] stringArray = mContext.getResources().getStringArray(resourceId);
        return (resourceIndex >= 0 && resourceIndex < stringArray.length)
                ? stringArray[resourceIndex]
                : null;
    }

    void setRelatedBatteryDiffEntry(BatteryDiffEntry batteryDiffEntry) {
        mRelatedBatteryDiffEntry = batteryDiffEntry;
    }

    String getEventId() {
        return mPowerAnomalyEvent.hasEventId() ? mPowerAnomalyEvent.getEventId() : null;
    }

    int getIconResId() {
        return getResourceId(R.array.battery_tips_card_icons, mCardStyleId, "drawable");
    }

    int getColorResId() {
        return getResourceId(R.array.battery_tips_card_colors, mCardStyleId, "color");
    }

    String getTitleString() {
        final String titleStringFromProto =
                getInfo(WarningBannerInfo::getTitleString, WarningItemInfo::getTitleString);
        if (!TextUtils.isEmpty(titleStringFromProto)) {
            return titleStringFromProto;
        }
        final int titleFormatResId =
                getResourceId(R.array.power_anomaly_title_ids, mResourceIndex, "string");
        if (mPowerAnomalyEvent.hasWarningBannerInfo()) {
            return mContext.getString(titleFormatResId);
        } else if (mPowerAnomalyEvent.hasWarningItemInfo() && mRelatedBatteryDiffEntry != null) {
            final String appLabel = mRelatedBatteryDiffEntry.getAppLabel();
            return mContext.getString(titleFormatResId, appLabel);
        }
        return null;
    }

    String getMainBtnString() {
        return getString(
                WarningBannerInfo::getMainButtonString,
                WarningItemInfo::getMainButtonString,
                R.array.power_anomaly_main_btn_strings,
                mResourceIndex);
    }

    String getDismissBtnString() {
        return getString(
                WarningBannerInfo::getCancelButtonString,
                WarningItemInfo::getCancelButtonString,
                R.array.power_anomaly_dismiss_btn_strings,
                mResourceIndex);
    }

    String getAnomalyHintString() {
        final String anomalyHintStringFromProto =
                getInfo(null, WarningItemInfo::getWarningInfoString);
        return TextUtils.isEmpty(anomalyHintStringFromProto)
                ? getStringFromArrayResource(R.array.power_anomaly_hint_messages, mResourceIndex)
                : anomalyHintStringFromProto;
    }

    String getAnomalyHintPrefKey() {
        return getInfo(null, WarningItemInfo::getAnomalyHintPrefKey);
    }

    String getDismissRecordKey() {
        return mPowerAnomalyEvent.getDismissRecordKey();
    }

    boolean hasAnomalyEntryKey() {
        return getAnomalyEntryKey() != null;
    }

    String getAnomalyEntryKey() {
        return mPowerAnomalyEvent.hasWarningItemInfo()
                        && mPowerAnomalyEvent.getWarningItemInfo().hasItemKey()
                ? mPowerAnomalyEvent.getWarningItemInfo().getItemKey()
                : null;
    }

    boolean hasSubSettingLauncher() {
        if (mSubSettingLauncher == null) {
            mSubSettingLauncher = getSubSettingLauncher();
        }
        return mSubSettingLauncher != null;
    }

    SubSettingLauncher getSubSettingLauncher() {
        if (mSubSettingLauncher != null) {
            return mSubSettingLauncher;
        }
        final String destinationClassName =
                getInfo(WarningBannerInfo::getMainButtonDestination, null);
        if (!TextUtils.isEmpty(destinationClassName)) {
            final Integer sourceMetricsCategory =
                    getInfo(WarningBannerInfo::getMainButtonSourceMetricsCategory, null);
            final String preferenceHighlightKey =
                    getInfo(WarningBannerInfo::getMainButtonSourceHighlightKey, null);
            Bundle arguments = Bundle.EMPTY;
            if (!TextUtils.isEmpty(preferenceHighlightKey)) {
                arguments = new Bundle(1);
                arguments.putString(
                        SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, preferenceHighlightKey);
            }
            mSubSettingLauncher =
                    new SubSettingLauncher(mContext)
                            .setDestination(destinationClassName)
                            .setSourceMetricsCategory(sourceMetricsCategory)
                            .setArguments(arguments);
        }
        return mSubSettingLauncher;
    }

    boolean hasHighlightSlotPair(BatteryLevelData batteryLevelData) {
        if (mHighlightSlotPair == null) {
            mHighlightSlotPair = getHighlightSlotPair(batteryLevelData);
        }
        return mHighlightSlotPair != null;
    }

    Pair<Integer, Integer> getHighlightSlotPair(BatteryLevelData batteryLevelData) {
        if (mHighlightSlotPair != null) {
            return mHighlightSlotPair;
        }
        if (!mPowerAnomalyEvent.hasWarningItemInfo()) {
            return null;
        }
        final WarningItemInfo warningItemInfo = mPowerAnomalyEvent.getWarningItemInfo();
        final Long startTimestamp =
                warningItemInfo.hasStartTimestamp() ? warningItemInfo.getStartTimestamp() : null;
        final Long endTimestamp =
                warningItemInfo.hasEndTimestamp() ? warningItemInfo.getEndTimestamp() : null;
        if (startTimestamp != null && endTimestamp != null) {
            mHighlightSlotPair =
                    batteryLevelData.getIndexByTimestamps(startTimestamp, endTimestamp);
            if (mHighlightSlotPair.first == BatteryChartViewModel.SELECTED_INDEX_INVALID
                    || mHighlightSlotPair.second == BatteryChartViewModel.SELECTED_INDEX_INVALID) {
                // Drop invalid mHighlightSlotPair index
                mHighlightSlotPair = null;
            }
        }
        return mHighlightSlotPair;
    }

    boolean updateTipsCardPreference(BatteryTipsCardPreference preference) {
        final String titleString = getTitleString();
        if (TextUtils.isEmpty(titleString)) {
            return false;
        }
        preference.setTitle(titleString);
        preference.setIconResourceId(getIconResId());
        preference.setButtonColorResourceId(getColorResId());
        preference.setMainButtonLabel(getMainBtnString());
        preference.setDismissButtonLabel(getDismissBtnString());
        return true;
    }

    boolean launchSubSetting() {
        if (!hasSubSettingLauncher()) {
            return false;
        }
        // Navigate to sub setting page
        mSubSettingLauncher.launch();
        return true;
    }
}
