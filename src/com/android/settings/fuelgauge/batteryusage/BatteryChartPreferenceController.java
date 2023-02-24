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
        implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnDestroy,
        OnSaveInstanceState, OnResume {
    private static final String TAG = "BatteryChartPreferenceController";
    private static final String PREFERENCE_KEY = "battery_chart";

    private static final long FADE_IN_ANIMATION_DURATION = 400L;
    private static final long FADE_OUT_ANIMATION_DURATION = 200L;

    // Keys for bundle instance to restore configurations.
    private static final String KEY_DAILY_CHART_INDEX = "daily_chart_index";
    private static final String KEY_HOURLY_CHART_INDEX = "hourly_chart_index";

    /**
     * A callback listener for battery usage is updated.
     * This happens when battery usage data is ready or the selected index is changed.
     */
    public interface OnBatteryUsageUpdatedListener {
        /**
         * The callback function for battery usage is updated.
         * @param slotUsageData The battery usage diff data for the selected slot. This is used in
         *                      the app list.
         * @param slotTimestamp The selected slot timestamp information. This is used in the battery
         *                      usage breakdown category.
         * @param isAllUsageDataEmpty Whether all the battery usage data is null or empty. This is
         *                            used when showing the footer.
         */
        void onBatteryUsageUpdated(
                BatteryDiffData slotUsageData, String slotTimestamp, boolean isAllUsageDataEmpty);
    }

    /**
     * A callback listener for the device screen on time is updated.
     * This happens when screen on time data is ready or the selected index is changed.
     */
    public interface OnScreenOnTimeUpdatedListener {
        /**
         * The callback function for the device screen on time is updated.
         * @param screenOnTime The selected slot device screen on time.
         * @param slotTimestamp The selected slot timestamp information.
         */
        void onScreenOnTimeUpdated(Long screenOnTime, String slotTimestamp);
    }

    @VisibleForTesting
    Context mPrefContext;
    @VisibleForTesting
    BatteryChartView mDailyChartView;
    @VisibleForTesting
    BatteryChartView mHourlyChartView;
    @VisibleForTesting
    int mDailyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
    @VisibleForTesting
    int mHourlyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
    @VisibleForTesting
    Map<Integer, Map<Integer, BatteryDiffData>> mBatteryUsageMap;
    @VisibleForTesting
    Map<Integer, Map<Integer, Long>> mScreenOnTimeMap;

    private boolean mIs24HourFormat;
    private boolean mHourlyChartVisible = true;
    private View mBatteryChartViewGroup;
    private TextView mChartSummaryTextView;
    private BatteryChartViewModel mDailyViewModel;
    private List<BatteryChartViewModel> mHourlyViewModels;
    private OnBatteryUsageUpdatedListener mOnBatteryUsageUpdatedListener;
    private OnScreenOnTimeUpdatedListener mOnScreenOnTimeUpdatedListener;

    private final SettingsActivity mActivity;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final AnimatorListenerAdapter mHourlyChartFadeInAdapter =
            createHourlyChartAnimatorListenerAdapter(/*visible=*/ true);
    private final AnimatorListenerAdapter mHourlyChartFadeOutAdapter =
            createHourlyChartAnimatorListenerAdapter(/*visible=*/ false);

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
        mMetricsFeatureProvider =
                FeatureFactory.getFactory(mContext).getMetricsFeatureProvider();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mDailyChartIndex =
                savedInstanceState.getInt(KEY_DAILY_CHART_INDEX, mDailyChartIndex);
        mHourlyChartIndex =
                savedInstanceState.getInt(KEY_HOURLY_CHART_INDEX, mHourlyChartIndex);
        Log.d(TAG, String.format("onCreate() dailyIndex=%d hourlyIndex=%d",
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
        Log.d(TAG, String.format("onSaveInstanceState() dailyIndex=%d hourlyIndex=%d",
                mDailyChartIndex, mHourlyChartIndex));
    }

    @Override
    public void onDestroy() {
        if (mActivity == null || mActivity.isChangingConfigurations()) {
            BatteryDiffEntry.clearCache();
        }
        mHandler.removeCallbacksAndMessages(/*token=*/ null);
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

    void setOnBatteryUsageUpdatedListener(OnBatteryUsageUpdatedListener listener) {
        mOnBatteryUsageUpdatedListener = listener;
    }

    void setOnScreenOnTimeUpdatedListener(OnScreenOnTimeUpdatedListener listener) {
        mOnScreenOnTimeUpdatedListener = listener;
    }

    void setBatteryHistoryMap(
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        Log.d(TAG, "setBatteryHistoryMap() " + (batteryHistoryMap == null ? "null"
                : ("size=" + batteryHistoryMap.size())));
        // Ensure the battery chart group is visible for users.
        animateBatteryChartViewGroup();
        final BatteryLevelData batteryLevelData =
                DataProcessManager.getBatteryLevelData(mContext, mHandler, batteryHistoryMap,
                        batteryCallbackData -> {
                            mBatteryUsageMap = batteryCallbackData.getBatteryUsageMap();
                            mScreenOnTimeMap = batteryCallbackData.getDeviceScreenOnTime();
                            logScreenUsageTime();
                            refreshUi();
                        });
        Log.d(TAG, "getBatteryLevelData: " + batteryLevelData);
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
        mDailyViewModel = new BatteryChartViewModel(
                batteryLevelData.getDailyBatteryLevels().getLevels(),
                batteryLevelData.getDailyBatteryLevels().getTimestamps(),
                BatteryChartViewModel.AxisLabelPosition.CENTER_OF_TRAPEZOIDS,
                mDailyChartLabelTextGenerator);
        mHourlyViewModels = new ArrayList<>();
        for (BatteryLevelData.PeriodBatteryLevelData hourlyBatteryLevelsPerDay :
                batteryLevelData.getHourlyBatteryLevelsPerDay()) {
            mHourlyViewModels.add(new BatteryChartViewModel(
                    hourlyBatteryLevelsPerDay.getLevels(),
                    hourlyBatteryLevelsPerDay.getTimestamps(),
                    BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS,
                    mHourlyChartLabelTextGenerator.setLatestTimestamp(getLast(getLast(
                            batteryLevelData.getHourlyBatteryLevelsPerDay()).getTimestamps()))));
        }
        refreshUi();
    }

    void setBatteryChartView(@NonNull final BatteryChartView dailyChartView,
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
            mChartSummaryTextView = grandparentView != null
                    ? grandparentView.findViewById(R.id.chart_summary) : null;
        }
    }

    private void setBatteryChartViewInner(@NonNull final BatteryChartView dailyChartView,
            @NonNull final BatteryChartView hourlyChartView) {
        mDailyChartView = dailyChartView;
        mDailyChartView.setOnSelectListener(trapezoidIndex -> {
            if (mDailyChartIndex == trapezoidIndex) {
                return;
            }
            Log.d(TAG, "onDailyChartSelect:" + trapezoidIndex);
            mDailyChartIndex = trapezoidIndex;
            mHourlyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
            refreshUi();
            mHandler.post(() -> mDailyChartView.announceForAccessibility(
                    getAccessibilityAnnounceMessage()));
            mMetricsFeatureProvider.action(
                    mPrefContext,
                    trapezoidIndex == BatteryChartViewModel.SELECTED_INDEX_ALL
                            ? SettingsEnums.ACTION_BATTERY_USAGE_DAILY_SHOW_ALL
                            : SettingsEnums.ACTION_BATTERY_USAGE_DAILY_TIME_SLOT,
                    mDailyChartIndex);
        });
        mHourlyChartView = hourlyChartView;
        mHourlyChartView.setOnSelectListener(trapezoidIndex -> {
            if (mHourlyChartIndex == trapezoidIndex) {
                return;
            }
            Log.d(TAG, "onHourlyChartSelect:" + trapezoidIndex);
            mHourlyChartIndex = trapezoidIndex;
            refreshUi();
            mHandler.post(() -> mHourlyChartView.announceForAccessibility(
                    getAccessibilityAnnounceMessage()));
            mMetricsFeatureProvider.action(
                    mPrefContext,
                    trapezoidIndex == BatteryChartViewModel.SELECTED_INDEX_ALL
                            ? SettingsEnums.ACTION_BATTERY_USAGE_SHOW_ALL
                            : SettingsEnums.ACTION_BATTERY_USAGE_TIME_SLOT,
                    mHourlyChartIndex);
        });
        refreshUi();
    }

    @VisibleForTesting
    boolean refreshUi() {
        if (mDailyChartView == null || mHourlyChartView == null) {
            // Chart views are not initialized.
            return false;
        }

        // When mDailyViewModel or mHourlyViewModels is null, there is no battery level data.
        // This is mainly in 2 cases:
        // 1) battery data is within 2 hours
        // 2) no battery data in the latest 7 days (power off >= 7 days)
        final boolean refreshUiResult = mDailyViewModel == null || mHourlyViewModels == null
                ? refreshUiWithNoLevelDataCase()
                : refreshUiWithLevelDataCase();

        if (!refreshUiResult) {
            return false;
        }

        if (mOnScreenOnTimeUpdatedListener != null && mScreenOnTimeMap != null
                && mScreenOnTimeMap.get(mDailyChartIndex) != null) {
            mOnScreenOnTimeUpdatedListener.onScreenOnTimeUpdated(
                    mScreenOnTimeMap.get(mDailyChartIndex).get(mHourlyChartIndex),
                    getSlotInformation());
        }
        if (mOnBatteryUsageUpdatedListener != null && mBatteryUsageMap != null
                && mBatteryUsageMap.get(mDailyChartIndex) != null) {
            final BatteryDiffData slotUsageData =
                    mBatteryUsageMap.get(mDailyChartIndex).get(mHourlyChartIndex);
            mOnBatteryUsageUpdatedListener.onBatteryUsageUpdated(
                    slotUsageData, getSlotInformation(), isBatteryUsageMapNullOrEmpty());
        }
        return true;
    }

    private boolean refreshUiWithNoLevelDataCase() {
        setChartSummaryVisible(false);
        if (mBatteryUsageMap == null) {
            // There is no battery level data and battery usage data is not ready, wait for data
            // ready to refresh UI. Show nothing temporarily.
            mDailyChartView.setVisibility(View.GONE);
            mHourlyChartView.setVisibility(View.GONE);
            mDailyChartView.setViewModel(null);
            mHourlyChartView.setViewModel(null);
            return false;
        } else if (mBatteryUsageMap
                .get(BatteryChartViewModel.SELECTED_INDEX_ALL)
                .get(BatteryChartViewModel.SELECTED_INDEX_ALL) == null) {
            // There is no battery level data and battery usage data, show an empty hourly chart
            // view.
            mDailyChartView.setVisibility(View.GONE);
            mHourlyChartView.setVisibility(View.VISIBLE);
            mHourlyChartView.setViewModel(null);
        }
        return true;
    }

    private boolean refreshUiWithLevelDataCase() {
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
            mDailyChartView.setViewModel(mDailyViewModel);
        }

        if (mDailyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL) {
            // Multiple days are selected, hide the hourly chart view.
            animateBatteryHourlyChartView(/*visible=*/ false);
        } else {
            animateBatteryHourlyChartView(/*visible=*/ true);
            final BatteryChartViewModel hourlyViewModel =
                    mHourlyViewModels.get(mDailyChartIndex);
            if (mHourlyChartIndex >= hourlyViewModel.size()) {
                mHourlyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
            }
            hourlyViewModel.setSelectedIndex(mHourlyChartIndex);
            mHourlyChartView.setViewModel(hourlyViewModel);
        }

        if (mBatteryUsageMap == null) {
            // Battery usage data is not ready, wait for data ready to refresh UI.
            return false;
        }
        return true;
    }

    @VisibleForTesting
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

        final String selectedHourText = mHourlyViewModels.get(mDailyChartIndex).getFullText(
                mHourlyChartIndex);
        if (isBatteryLevelDataInOneDay()) {
            return selectedHourText;
        }

        return mContext.getString(
                R.string.battery_usage_day_and_hour, selectedDayText, selectedHourText);
    }

    private String getAccessibilityAnnounceMessage() {
        final String slotInformation = getSlotInformation();
        return slotInformation == null
                ? mPrefContext.getString(
                       R.string.battery_usage_breakdown_title_since_last_full_charge)
                : mPrefContext.getString(
                        R.string.battery_usage_breakdown_title_for_slot, slotInformation);
    }

    private void animateBatteryChartViewGroup() {
        if (mBatteryChartViewGroup != null && mBatteryChartViewGroup.getAlpha() == 0) {
            mBatteryChartViewGroup.animate().alpha(1f).setDuration(FADE_IN_ANIMATION_DURATION)
                    .start();
        }
    }

    private void animateBatteryHourlyChartView(final boolean visible) {
        if (mHourlyChartView == null || mHourlyChartVisible == visible) {
            return;
        }
        mHourlyChartVisible = visible;

        if (visible) {
            mHourlyChartView.setVisibility(View.VISIBLE);
            mHourlyChartView.animate()
                    .alpha(1f)
                    .setDuration(FADE_IN_ANIMATION_DURATION)
                    .setListener(mHourlyChartFadeInAdapter)
                    .start();
        } else {
            mHourlyChartView.animate()
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

    private void logScreenUsageTime() {
        if (mBatteryUsageMap == null || mScreenOnTimeMap == null) {
            return;
        }
        final long totalScreenOnTime =
                mScreenOnTimeMap
                        .get(BatteryChartViewModel.SELECTED_INDEX_ALL)
                        .get(BatteryChartViewModel.SELECTED_INDEX_ALL);
        mMetricsFeatureProvider.action(
                mPrefContext,
                SettingsEnums.ACTION_BATTERY_USAGE_SCREEN_ON_TIME,
                (int) totalScreenOnTime);
        mMetricsFeatureProvider.action(
                mPrefContext,
                SettingsEnums.ACTION_BATTERY_USAGE_FOREGROUND_USAGE_TIME,
                (int) getTotalForegroundUsageTime());
    }

    private long getTotalForegroundUsageTime() {
        if (mBatteryUsageMap == null) {
            return 0;
        }
        final BatteryDiffData totalBatteryUsageDiffData =
                mBatteryUsageMap
                        .get(BatteryChartViewModel.SELECTED_INDEX_ALL)
                        .get(BatteryChartViewModel.SELECTED_INDEX_ALL);
        if (totalBatteryUsageDiffData == null) {
            return 0;
        }
        long totalValue = 0;
        for (final BatteryDiffEntry entry : totalBatteryUsageDiffData.getAppDiffEntryList()) {
            totalValue += entry.mForegroundUsageTimeInMs;
        }
        return totalValue;
    }

    private boolean isBatteryLevelDataInOneDay() {
        return mHourlyViewModels != null && mHourlyViewModels.size() == 1;
    }

    private boolean isAllSelected() {
        return (isBatteryLevelDataInOneDay()
                || mDailyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL)
                && mHourlyChartIndex == BatteryChartViewModel.SELECTED_INDEX_ALL;
    }

    private boolean isBatteryUsageMapNullOrEmpty() {
        if (mBatteryUsageMap == null) {
            return true;
        }
        BatteryDiffData allBatteryDiffData = mBatteryUsageMap
                .get(BatteryChartViewModel.SELECTED_INDEX_ALL)
                .get(BatteryChartViewModel.SELECTED_INDEX_ALL);
        // If all data is null or empty, each slot must be null or empty.
        return allBatteryDiffData == null
                || (allBatteryDiffData.getAppDiffEntryList().isEmpty()
                && allBatteryDiffData.getSystemDiffEntryList().isEmpty());
    }

    @VisibleForTesting
    static int getTotalHours(final BatteryLevelData batteryLevelData) {
        if (batteryLevelData == null) {
            return 0;
        }
        List<Long> dailyTimestamps = batteryLevelData.getDailyBatteryLevels().getTimestamps();
        return (int) ((dailyTimestamps.get(dailyTimestamps.size() - 1) - dailyTimestamps.get(0))
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
        Log.d(TAG, String.format("getBatterySinceLastFullChargeUsageData() size=%d time=%d/ms",
                batteryHistoryMap.size(), (System.currentTimeMillis() - start)));

        final Map<Integer, Map<Integer, BatteryDiffData>> batteryUsageData =
                DataProcessor.getBatteryUsageData(context, batteryHistoryMap);
        if (batteryUsageData == null) {
            return null;
        }
        BatteryDiffData allBatteryDiffData = batteryUsageData.get(
                BatteryChartViewModel.SELECTED_INDEX_ALL).get(
                BatteryChartViewModel.SELECTED_INDEX_ALL);
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
            final BatteryHistEntry batteryHistEntry = entry.mBatteryHistEntry;
            if (batteryHistEntry != null
                    && batteryHistEntry.mConsumerType == ConvertUtils.CONSUMER_TYPE_UID_BATTERY
                    && batteryHistEntry.mUserId == userId
                    && packageName.equals(entry.getPackageName())) {
                return entry;
            }
        }
        return null;
    }

    private final class DailyChartLabelTextGenerator implements
            BatteryChartViewModel.LabelTextGenerator {
        @Override
        public String generateText(List<Long> timestamps, int index) {
            return ConvertUtils.utcToLocalTimeDayOfWeek(mContext,
                    timestamps.get(index), /* isAbbreviation= */ true);
        }

        @Override
        public String generateFullText(List<Long> timestamps, int index) {
            return ConvertUtils.utcToLocalTimeDayOfWeek(mContext,
                    timestamps.get(index), /* isAbbreviation= */ false);
        }
    }

    private final class HourlyChartLabelTextGenerator implements
            BatteryChartViewModel.LabelTextGenerator {
        private Long mLatestTimestamp;

        @Override
        public String generateText(List<Long> timestamps, int index) {
            if (Objects.equal(timestamps.get(index), mLatestTimestamp)) {
                // Replaces the latest timestamp text to "now".
                return mContext.getString(R.string.battery_usage_chart_label_now);
            }
            return ConvertUtils.utcToLocalTimeHour(mContext, timestamps.get(index),
                    mIs24HourFormat);
        }

        @Override
        public String generateFullText(List<Long> timestamps, int index) {
            if (Objects.equal(timestamps.get(index), mLatestTimestamp)) {
                // Replaces the latest timestamp text to "now".
                return mContext.getString(R.string.battery_usage_chart_label_now);
            }
            return index == timestamps.size() - 1
                    ? generateText(timestamps, index)
                    : mContext.getString(R.string.battery_usage_timestamps_hyphen,
                            generateText(timestamps, index), generateText(timestamps, index + 1));
        }

        public HourlyChartLabelTextGenerator setLatestTimestamp(Long latestTimestamp) {
            this.mLatestTimestamp = latestTimestamp;
            return this;
        }
    }
}
