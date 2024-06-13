/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/** Controls the update for chart graph and the list items. */
public class BatteryChartPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin,
                LifecycleObserver,
                OnCreate,
                OnDestroy,
                OnSaveInstanceState,
                OnResume {
    private static final String TAG = "BatteryChartPreferenceController";
    private static final String PREFERENCE_KEY = "battery_chart";

    private static final long FADE_IN_ANIMATION_DURATION = 400L;
    private static final long FADE_OUT_ANIMATION_DURATION = 200L;

    // Keys for bundle instance to restore configurations.
    private static final String KEY_DAILY_CHART_INDEX = "daily_chart_index";
    private static final String KEY_HOURLY_CHART_INDEX = "hourly_chart_index";

    /** A callback listener for the selected index is updated. */
    interface OnSelectedIndexUpdatedListener {
        /** The callback function for the selected index is updated. */
        void onSelectedIndexUpdated();
    }

    @VisibleForTesting Context mPrefContext;
    @VisibleForTesting TextView mChartSummaryTextView;
    @VisibleForTesting BatteryChartView mDailyChartView;
    @VisibleForTesting BatteryChartView mHourlyChartView;
    @VisibleForTesting int mDailyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
    @VisibleForTesting int mHourlyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
    @VisibleForTesting int mDailyHighlightSlotIndex = BatteryChartViewModel.SELECTED_INDEX_INVALID;
    @VisibleForTesting int mHourlyHighlightSlotIndex = BatteryChartViewModel.SELECTED_INDEX_INVALID;

    private boolean mIs24HourFormat;
    private View mBatteryChartViewGroup;
    private BatteryChartViewModel mDailyViewModel;
    private List<BatteryChartViewModel> mHourlyViewModels;
    private OnSelectedIndexUpdatedListener mOnSelectedIndexUpdatedListener;

    private final SettingsActivity mActivity;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final AnimatorListenerAdapter mHourlyChartFadeInAdapter =
            createHourlyChartAnimatorListenerAdapter(/* visible= */ true);
    private final AnimatorListenerAdapter mHourlyChartFadeOutAdapter =
            createHourlyChartAnimatorListenerAdapter(/* visible= */ false);

    @VisibleForTesting
    final DailyChartLabelTextGenerator mDailyChartLabelTextGenerator =
            new DailyChartLabelTextGenerator();

    @VisibleForTesting
    final HourlyChartLabelTextGenerator mHourlyChartLabelTextGenerator =
            new HourlyChartLabelTextGenerator();

    public BatteryChartPreferenceController(
            Context context, Lifecycle lifecycle, SettingsActivity activity) {
        super(context);
        mActivity = activity;
        mIs24HourFormat = DateFormat.is24HourFormat(context);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mDailyChartIndex = savedInstanceState.getInt(KEY_DAILY_CHART_INDEX, mDailyChartIndex);
        mHourlyChartIndex = savedInstanceState.getInt(KEY_HOURLY_CHART_INDEX, mHourlyChartIndex);
        Log.d(
                TAG,
                String.format(
                        "onCreate() dailyIndex=%d hourlyIndex=%d",
                        mDailyChartIndex, mHourlyChartIndex));
    }

    @Override
    public void onResume() {
        mIs24HourFormat = DateFormat.is24HourFormat(mContext);
        mMetricsFeatureProvider.action(mPrefContext, SettingsEnums.OPEN_BATTERY_USAGE);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstance) {
        if (savedInstance == null) {
            return;
        }
        savedInstance.putInt(KEY_DAILY_CHART_INDEX, mDailyChartIndex);
        savedInstance.putInt(KEY_HOURLY_CHART_INDEX, mHourlyChartIndex);
        Log.d(
                TAG,
                String.format(
                        "onSaveInstanceState() dailyIndex=%d hourlyIndex=%d",
                        mDailyChartIndex, mHourlyChartIndex));
    }

    @Override
    public void onDestroy() {
        if (mActivity == null || mActivity.isChangingConfigurations()) {
            BatteryDiffEntry.clearCache();
        }
        mHandler.removeCallbacksAndMessages(/* token= */ null);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrefContext = screen.getContext();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    int getDailyChartIndex() {
        return mDailyChartIndex;
    }

    int getHourlyChartIndex() {
        return mHourlyChartIndex;
    }

    void setOnSelectedIndexUpdatedListener(OnSelectedIndexUpdatedListener listener) {
        mOnSelectedIndexUpdatedListener = listener;
    }

    void onBatteryLevelDataUpdate(final BatteryLevelData batteryLevelData) {
        Log.d(TAG, "onBatteryLevelDataUpdate: " + batteryLevelData);
        mMetricsFeatureProvider.action(
                mPrefContext,
                SettingsEnums.ACTION_BATTERY_HISTORY_LOADED,
                getTotalHours(batteryLevelData));

        if (batteryLevelData == null) {
            mDailyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
            mHourlyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
            mDailyViewModel = null;
            mHourlyViewModels = null;
            refreshUi();
            return;
        }
        mDailyViewModel =
                new BatteryChartViewModel(
                        batteryLevelData.getDailyBatteryLevels().getLevels(),
                        batteryLevelData.getDailyBatteryLevels().getTimestamps(),
                        BatteryChartViewModel.AxisLabelPosition.CENTER_OF_TRAPEZOIDS,
                        mDailyChartLabelTextGenerator);
        mHourlyViewModels = new ArrayList<>();
        for (BatteryLevelData.PeriodBatteryLevelData hourlyBatteryLevelsPerDay :
                batteryLevelData.getHourlyBatteryLevelsPerDay()) {
            mHourlyViewModels.add(
                    new BatteryChartViewModel(
                            hourlyBatteryLevelsPerDay.getLevels(),
                            hourlyBatteryLevelsPerDay.getTimestamps(),
                            BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS,
                            mHourlyChartLabelTextGenerator.updateSpecialCaseContext(
                                    batteryLevelData)));
        }
        refreshUi();
    }

    boolean isHighlightSlotFocused() {
        return (mDailyHighlightSlotIndex != BatteryChartViewModel.SELECTED_INDEX_INVALID
                && mDailyHighlightSlotIndex == mDailyChartIndex
                && mHourlyHighlightSlotIndex != BatteryChartViewModel.SELECTED_INDEX_INVALID
                && mHourlyHighlightSlotIndex == mHourlyChartIndex);
    }

    void onHighlightSlotIndexUpdate(int dailyHighlightSlotIndex, int hourlyHighlightSlotIndex) {
        mDailyHighlightSlotIndex = dailyHighlightSlotIndex;
        mHourlyHighlightSlotIndex = hourlyHighlightSlotIndex;
        refreshUi();
        if (mOnSelectedIndexUpdatedListener != null) {
            mOnSelectedIndexUpdatedListener.onSelectedIndexUpdated();
        }
    }

    void selectHighlightSlotIndex() {
        if (mDailyHighlightSlotIndex == BatteryChartViewModel.SELECTED_INDEX_INVALID
                || mHourlyHighlightSlotIndex == BatteryChartViewModel.SELECTED_INDEX_INVALID) {
            return;
        }
        if (mDailyHighlightSlotIndex == mDailyChartIndex
                && mHourlyHighlightSlotIndex == mHourlyChartIndex) {
            return;
        }
        mDailyChartIndex = mDailyHighlightSlotIndex;
        mHourlyChartIndex = mHourlyHighlightSlotIndex;
        Log.d(
                TAG,
                String.format(
                        "onDailyChartSelect:%d, onHourlyChartSelect:%d",
                        mDailyChartIndex, mHourlyChartIndex));
        refreshUi();
        mHandler.post(
                () -> mDailyChartView.announceForAccessibility(getAccessibilityAnnounceMessage()));
        if (mOnSelectedIndexUpdatedListener != null) {
            mOnSelectedIndexUpdatedListener.onSelectedIndexUpdated();
        }
    }

    void setBatteryChartView(
            @NonNull final BatteryChartView dailyChartView,
            @NonNull final BatteryChartView hourlyChartView) {
        final View parentView = (View) dailyChartView.getParent();
        if (parentView != null && parentView.getId() == R.id.battery_chart_group) {
            mBatteryChartViewGroup = (View) dailyChartView.getParent();
        }
        if (mDailyChartView != dailyChartView || mHourlyChartView != hourlyChartView) {
            mHandler.post(() -> setBatteryChartViewInner(dailyChartView, hourlyChartView));
            animateBatteryChartViewGroup();
        }
        if (mBatteryChartViewGroup != null) {
            final View grandparentView = (View) mBatteryChartViewGroup.getParent();
            mChartSummaryTextView =
                    grandparentView != null
                            ? grandparentView.findViewById(R.id.chart_summary)
                            : null;
        }
    }

    private void setBatteryChartViewInner(
            @NonNull final BatteryChartView dailyChartView,
            @NonNull final BatteryChartView hourlyChartView) {
        mDailyChartView = dailyChartView;
        mDailyChartView.setOnSelectListener(
                trapezoidIndex -> {
                    if (mDailyChartIndex == trapezoidIndex) {
                        return;
                    }
                    Log.d(TAG, "onDailyChartSelect:" + trapezoidIndex);
                    mDailyChartIndex = trapezoidIndex;
                    mHourlyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
                    refreshUi();
                    mHandler.post(
                            () ->
                                    mDailyChartView.announceForAccessibility(
                                            getAccessibilityAnnounceMessage()));
                    mMetricsFeatureProvider.action(
                            mPrefContext,
                            trapezoidIndex == BatteryChartViewModel.SELECTED_INDEX_ALL
                                    ? SettingsEnums.ACTION_BATTERY_USAGE_DAILY_SHOW_ALL
                                    : SettingsEnums.ACTION_BATTERY_USAGE_DAILY_TIME_SLOT,
                            mDailyChartIndex);
                    if (mOnSelectedIndexUpdatedListener != null) {
                        mOnSelectedIndexUpdatedListener.onSelectedIndexUpdated();
                    }
                });
        mHourlyChartView = hourlyChartView;
        mHourlyChartView.setOnSelectListener(
                trapezoidIndex -> {
                    if (mDailyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL) {
                        // This will happen when a daily slot and an hour slot are clicked together.
                        return;
                    }
                    if (mHourlyChartIndex == trapezoidIndex) {
                        return;
                    }
                    Log.d(TAG, "onHourlyChartSelect:" + trapezoidIndex);
                    mHourlyChartIndex = trapezoidIndex;
                    refreshUi();
                    mHandler.post(
                            () ->
                                    mHourlyChartView.announceForAccessibility(
                                            getAccessibilityAnnounceMessage()));
                    mMetricsFeatureProvider.action(
                            mPrefContext,
                            trapezoidIndex == BatteryChartViewModel.SELECTED_INDEX_ALL
                                    ? SettingsEnums.ACTION_BATTERY_USAGE_SHOW_ALL
                                    : SettingsEnums.ACTION_BATTERY_USAGE_TIME_SLOT,
                            mHourlyChartIndex);
                    if (mOnSelectedIndexUpdatedListener != null) {
                        mOnSelectedIndexUpdatedListener.onSelectedIndexUpdated();
                    }
                });
        refreshUi();
    }

    // Show empty hourly chart view only if there is no valid battery usage data.
    void showEmptyChart() {
        if (mDailyChartView == null || mHourlyChartView == null) {
            // Chart views are not initialized.
            return;
        }
        setChartSummaryVisible(true);
        mDailyChartView.setVisibility(View.GONE);
        mHourlyChartView.setVisibility(View.VISIBLE);
        mHourlyChartView.setViewModel(null);
    }

    @VisibleForTesting
    void refreshUi() {
        if (mDailyChartView == null || mHourlyChartView == null) {
            // Chart views are not initialized.
            return;
        }

        if (mDailyViewModel == null || mHourlyViewModels == null) {
            setChartSummaryVisible(false);
            mDailyChartView.setVisibility(View.GONE);
            mHourlyChartView.setVisibility(View.GONE);
            mDailyChartView.setViewModel(null);
            mHourlyChartView.setViewModel(null);
            return;
        }

        setChartSummaryVisible(true);
        // Gets valid battery level data.
        if (isBatteryLevelDataInOneDay()) {
            // Only 1 day data, hide the daily chart view.
            mDailyChartView.setVisibility(View.GONE);
            mDailyChartIndex = 0;
        } else {
            mDailyChartView.setVisibility(View.VISIBLE);
            if (mDailyChartIndex >= mDailyViewModel.size()) {
                mDailyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
            }
            mDailyViewModel.setSelectedIndex(mDailyChartIndex);
            mDailyViewModel.setHighlightSlotIndex(mDailyHighlightSlotIndex);
            mDailyChartView.setViewModel(mDailyViewModel);
        }

        if (mDailyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL) {
            // Multiple days are selected, hide the hourly chart view.
            animateBatteryHourlyChartView(/* visible= */ false);
        } else {
            animateBatteryHourlyChartView(/* visible= */ true);
            final BatteryChartViewModel hourlyViewModel = mHourlyViewModels.get(mDailyChartIndex);
            if (mHourlyChartIndex >= hourlyViewModel.size()) {
                mHourlyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
            }
            hourlyViewModel.setSelectedIndex(mHourlyChartIndex);
            hourlyViewModel.setHighlightSlotIndex(
                    (mDailyChartIndex == mDailyHighlightSlotIndex)
                            ? mHourlyHighlightSlotIndex
                            : BatteryChartViewModel.SELECTED_INDEX_INVALID);
            mHourlyChartView.setViewModel(hourlyViewModel);
        }
    }

    String getSlotInformation() {
        if (mDailyViewModel == null || mHourlyViewModels == null) {
            // No data
            return null;
        }
        if (isAllSelected()) {
            return null;
        }

        final String selectedDayText = mDailyViewModel.getFullText(mDailyChartIndex);
        if (mHourlyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL) {
            return selectedDayText;
        }

        final String selectedHourText =
                mHourlyViewModels.get(mDailyChartIndex).getFullText(mHourlyChartIndex);
        if (isBatteryLevelDataInOneDay()) {
            return selectedHourText;
        }

        return mContext.getString(
                R.string.battery_usage_day_and_hour, selectedDayText, selectedHourText);
    }

    @VisibleForTesting
    String getBatteryLevelPercentageInfo() {
        if (mDailyViewModel == null || mHourlyViewModels == null) {
            // No data
            return "";
        }

        if (mDailyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL
                || mHourlyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL) {
            return mDailyViewModel.getSlotBatteryLevelText(mDailyChartIndex);
        }

        return mHourlyViewModels.get(mDailyChartIndex).getSlotBatteryLevelText(mHourlyChartIndex);
    }

    private String getAccessibilityAnnounceMessage() {
        final String slotInformation = getSlotInformation();
        final String slotInformationMessage =
                slotInformation == null
                        ? mPrefContext.getString(
                                R.string.battery_usage_breakdown_title_since_last_full_charge)
                        : mPrefContext.getString(
                                R.string.battery_usage_breakdown_title_for_slot, slotInformation);
        final String batteryLevelPercentageMessage = getBatteryLevelPercentageInfo();

        return mPrefContext.getString(
                R.string.battery_usage_time_info_and_battery_level,
                slotInformationMessage,
                batteryLevelPercentageMessage);
    }

    private void animateBatteryChartViewGroup() {
        if (mBatteryChartViewGroup != null && mBatteryChartViewGroup.getAlpha() == 0) {
            mBatteryChartViewGroup
                    .animate()
                    .alpha(1f)
                    .setDuration(FADE_IN_ANIMATION_DURATION)
                    .start();
        }
    }

    private void animateBatteryHourlyChartView(final boolean visible) {
        if (mHourlyChartView == null
                || (mHourlyChartView.getVisibility() == View.VISIBLE) == visible) {
            return;
        }

        if (visible) {
            mHourlyChartView.setVisibility(View.VISIBLE);
            mHourlyChartView
                    .animate()
                    .alpha(1f)
                    .setDuration(FADE_IN_ANIMATION_DURATION)
                    .setListener(mHourlyChartFadeInAdapter)
                    .start();
        } else {
            mHourlyChartView
                    .animate()
                    .alpha(0f)
                    .setDuration(FADE_OUT_ANIMATION_DURATION)
                    .setListener(mHourlyChartFadeOutAdapter)
                    .start();
        }
    }

    private void setChartSummaryVisible(final boolean visible) {
        if (mChartSummaryTextView != null) {
            mChartSummaryTextView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private AnimatorListenerAdapter createHourlyChartAnimatorListenerAdapter(
            final boolean visible) {
        final int visibility = visible ? View.VISIBLE : View.GONE;

        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mHourlyChartView != null) {
                    mHourlyChartView.setVisibility(visibility);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                if (mHourlyChartView != null) {
                    mHourlyChartView.setVisibility(visibility);
                }
            }
        };
    }

    private boolean isBatteryLevelDataInOneDay() {
        return mHourlyViewModels != null && mHourlyViewModels.size() == 1;
    }

    private boolean isAllSelected() {
        return (isBatteryLevelDataInOneDay()
                        || mDailyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL)
                && mHourlyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL;
    }

    @VisibleForTesting
    static int getTotalHours(final BatteryLevelData batteryLevelData) {
        if (batteryLevelData == null) {
            return 0;
        }
        List<Long> dailyTimestamps = batteryLevelData.getDailyBatteryLevels().getTimestamps();
        return (int)
                ((dailyTimestamps.get(dailyTimestamps.size() - 1) - dailyTimestamps.get(0))
                        / DateUtils.HOUR_IN_MILLIS);
    }

    /** Used for {@link AppBatteryPreferenceController}. */
    public static List<BatteryDiffEntry> getAppBatteryUsageData(Context context) {
        final long start = System.currentTimeMillis();
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                DatabaseUtils.getHistoryMapSinceLastFullCharge(context, Calendar.getInstance());
        if (batteryHistoryMap == null || batteryHistoryMap.isEmpty()) {
            return null;
        }
        Log.d(
                TAG,
                String.format(
                        "getBatterySinceLastFullChargeUsageData() size=%d time=%d/ms",
                        batteryHistoryMap.size(), (System.currentTimeMillis() - start)));

        final Map<Integer, Map<Integer, BatteryDiffData>> batteryUsageData =
                DataProcessor.getBatteryUsageData(context, batteryHistoryMap);
        if (batteryUsageData == null) {
            return null;
        }
        BatteryDiffData allBatteryDiffData =
                batteryUsageData
                        .get(BatteryChartViewModel.SELECTED_INDEX_ALL)
                        .get(BatteryChartViewModel.SELECTED_INDEX_ALL);
        return allBatteryDiffData == null ? null : allBatteryDiffData.getAppDiffEntryList();
    }

    private static <T> T getLast(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /** Used for {@link AppBatteryPreferenceController}. */
    public static BatteryDiffEntry getAppBatteryUsageData(
            Context context, String packageName, int userId) {
        if (packageName == null) {
            return null;
        }
        final List<BatteryDiffEntry> entries = getAppBatteryUsageData(context);
        if (entries == null) {
            return null;
        }
        for (BatteryDiffEntry entry : entries) {
            if (!entry.isSystemEntry()
                    && entry.mUserId == userId
                    && packageName.equals(entry.getPackageName())) {
                return entry;
            }
        }
        return null;
    }

    private abstract class BaseLabelTextGenerator
            implements BatteryChartViewModel.LabelTextGenerator {
        @Override
        public String generateSlotBatteryLevelText(List<Integer> levels, int index) {
            final int fromBatteryLevelIndex =
                    index == BatteryChartViewModel.SELECTED_INDEX_ALL ? 0 : index;
            final int toBatteryLevelIndex =
                    index == BatteryChartViewModel.SELECTED_INDEX_ALL
                            ? levels.size() - 1
                            : index + 1;
            return mPrefContext.getString(
                    R.string.battery_level_percentage,
                    generateBatteryLevelText(levels.get(fromBatteryLevelIndex)),
                    generateBatteryLevelText(levels.get(toBatteryLevelIndex)));
        }

        @VisibleForTesting
        private static String generateBatteryLevelText(Integer level) {
            return Utils.formatPercentage(level);
        }
    }

    private final class DailyChartLabelTextGenerator extends BaseLabelTextGenerator
            implements BatteryChartViewModel.LabelTextGenerator {
        @Override
        public String generateText(List<Long> timestamps, int index) {
            return ConvertUtils.utcToLocalTimeDayOfWeek(
                    mContext, timestamps.get(index), /* isAbbreviation= */ true);
        }

        @Override
        public String generateFullText(List<Long> timestamps, int index) {
            return ConvertUtils.utcToLocalTimeDayOfWeek(
                    mContext, timestamps.get(index), /* isAbbreviation= */ false);
        }
    }

    private final class HourlyChartLabelTextGenerator extends BaseLabelTextGenerator
            implements BatteryChartViewModel.LabelTextGenerator {
        private static final int FULL_CHARGE_BATTERY_LEVEL = 100;

        private boolean mIsFromFullCharge;
        private long mFistTimestamp;
        private long mLatestTimestamp;

        @Override
        public String generateText(List<Long> timestamps, int index) {
            if (Objects.equal(timestamps.get(index), mLatestTimestamp)) {
                // Replaces the latest timestamp text to "now".
                return mContext.getString(R.string.battery_usage_chart_label_now);
            }
            long timestamp = timestamps.get(index);
            boolean showMinute = false;
            if (Objects.equal(timestamp, mFistTimestamp)) {
                if (mIsFromFullCharge) {
                    showMinute = true;
                } else {
                    // starts from 7 days ago
                    timestamp = TimestampUtils.getLastEvenHourTimestamp(timestamp);
                }
            }
            return ConvertUtils.utcToLocalTimeHour(
                    mContext, timestamp, mIs24HourFormat, showMinute);
        }

        @Override
        public String generateFullText(List<Long> timestamps, int index) {
            return index == timestamps.size() - 1
                    ? generateText(timestamps, index)
                    : mContext.getString(
                            R.string.battery_usage_timestamps_hyphen,
                            generateText(timestamps, index),
                            generateText(timestamps, index + 1));
        }

        HourlyChartLabelTextGenerator updateSpecialCaseContext(
                @NonNull final BatteryLevelData batteryLevelData) {
            BatteryLevelData.PeriodBatteryLevelData firstDayLevelData =
                    batteryLevelData.getHourlyBatteryLevelsPerDay().get(0);
            this.mIsFromFullCharge =
                    firstDayLevelData.getLevels().get(0) == FULL_CHARGE_BATTERY_LEVEL;
            this.mFistTimestamp = firstDayLevelData.getTimestamps().get(0);
            this.mLatestTimestamp =
                    getLast(
                            getLast(batteryLevelData.getHourlyBatteryLevelsPerDay())
                                    .getTimestamps());
            return this;
        }
    }
}
