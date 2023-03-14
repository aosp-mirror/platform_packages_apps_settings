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
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controls the update for chart graph and the list items. */
public class BatteryChartPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnDestroy,
        OnSaveInstanceState, OnResume, ExpandDividerPreference.OnExpandListener {
    private static final String TAG = "BatteryChartPreferenceController";
    private static final String KEY_FOOTER_PREF = "battery_graph_footer";
    private static final String PACKAGE_NAME_NONE = "none";
    private static final int ENABLED_ICON_ALPHA = 255;
    private static final int DISABLED_ICON_ALPHA = 255 / 3;

    private static final long FADE_IN_ANIMATION_DURATION = 400L;
    private static final long FADE_OUT_ANIMATION_DURATION = 200L;

    // Keys for bundle instance to restore configurations.
    private static final String KEY_EXPAND_SYSTEM_INFO = "expand_system_info";
    private static final String KEY_DAILY_CHART_INDEX = "daily_chart_index";
    private static final String KEY_HOURLY_CHART_INDEX = "hourly_chart_index";

    private static int sUiMode = Configuration.UI_MODE_NIGHT_UNDEFINED;

    @VisibleForTesting
    Map<Integer, Map<Integer, BatteryDiffData>> mBatteryUsageMap;

    @VisibleForTesting
    Context mPrefContext;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    PreferenceGroup mAppListPrefGroup;
    @VisibleForTesting
    ExpandDividerPreference mExpandDividerPreference;
    @VisibleForTesting
    boolean mIsExpanded = false;

    @VisibleForTesting
    BatteryChartView mDailyChartView;
    @VisibleForTesting
    BatteryChartView mHourlyChartView;

    @VisibleForTesting
    int mDailyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;
    @VisibleForTesting
    int mHourlyChartIndex = BatteryChartViewModel.SELECTED_INDEX_ALL;

    private boolean mIs24HourFormat;
    private boolean mIsFooterPrefAdded = false;
    private boolean mHourlyChartVisible = true;
    private View mBatteryChartViewGroup;
    private View mCategoryTitleView;
    private PreferenceScreen mPreferenceScreen;
    private FooterPreference mFooterPreference;
    private TextView mChartSummaryTextView;
    private BatteryChartViewModel mDailyViewModel;
    private List<BatteryChartViewModel> mHourlyViewModels;

    private final String mPreferenceKey;
    private final SettingsActivity mActivity;
    private final InstrumentedPreferenceFragment mFragment;
    private final CharSequence[] mNotAllowShowSummaryPackages;
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

    // Preference cache to avoid create new instance each time.
    @VisibleForTesting
    final Map<String, Preference> mPreferenceCache = new HashMap<>();

    public BatteryChartPreferenceController(
            Context context, String preferenceKey,
            Lifecycle lifecycle, SettingsActivity activity,
            InstrumentedPreferenceFragment fragment) {
        super(context);
        mActivity = activity;
        mFragment = fragment;
        mPreferenceKey = preferenceKey;
        mIs24HourFormat = DateFormat.is24HourFormat(context);
        mMetricsFeatureProvider =
                FeatureFactory.getFactory(mContext).getMetricsFeatureProvider();
        mNotAllowShowSummaryPackages =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getHideApplicationSummary(context);
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
        mIsExpanded =
                savedInstanceState.getBoolean(KEY_EXPAND_SYSTEM_INFO, mIsExpanded);
        Log.d(TAG, String.format("onCreate() dailyIndex=%d hourlyIndex=%d isExpanded=%b",
                mDailyChartIndex, mHourlyChartIndex, mIsExpanded));
    }

    @Override
    public void onResume() {
        final int currentUiMode =
                mContext.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;
        if (sUiMode != currentUiMode) {
            sUiMode = currentUiMode;
            BatteryDiffEntry.clearCache();
            Log.d(TAG, "clear icon and label cache since uiMode is changed");
        }
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
        savedInstance.putBoolean(KEY_EXPAND_SYSTEM_INFO, mIsExpanded);
        Log.d(TAG, String.format("onSaveInstanceState() dailyIndex=%d hourlyIndex=%d isExpanded=%b",
                mDailyChartIndex, mHourlyChartIndex, mIsExpanded));
    }

    @Override
    public void onDestroy() {
        if (mActivity.isChangingConfigurations()) {
            BatteryDiffEntry.clearCache();
        }
        mHandler.removeCallbacksAndMessages(/*token=*/ null);
        mPreferenceCache.clear();
        if (mAppListPrefGroup != null) {
            mAppListPrefGroup.removeAll();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mPrefContext = screen.getContext();
        mAppListPrefGroup = screen.findPreference(mPreferenceKey);
        mAppListPrefGroup.setOrderingAsAdded(false);
        mAppListPrefGroup.setTitle("");
        mFooterPreference = screen.findPreference(KEY_FOOTER_PREF);
        // Removes footer first until usage data is loaded to avoid flashing.
        if (mFooterPreference != null) {
            screen.removePreference(mFooterPreference);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof PowerGaugePreference)) {
            return false;
        }
        final PowerGaugePreference powerPref = (PowerGaugePreference) preference;
        final BatteryDiffEntry diffEntry = powerPref.getBatteryDiffEntry();
        final BatteryHistEntry histEntry = diffEntry.mBatteryHistEntry;
        final String packageName = histEntry.mPackageName;
        final boolean isAppEntry = histEntry.isAppEntry();
        mMetricsFeatureProvider.action(
                /* attribution */ SettingsEnums.OPEN_BATTERY_USAGE,
                /* action */ isAppEntry
                        ? SettingsEnums.ACTION_BATTERY_USAGE_APP_ITEM
                        : SettingsEnums.ACTION_BATTERY_USAGE_SYSTEM_ITEM,
                /* pageId */ SettingsEnums.OPEN_BATTERY_USAGE,
                TextUtils.isEmpty(packageName) ? PACKAGE_NAME_NONE : packageName,
                (int) Math.round(diffEntry.getPercentOfTotal()));
        Log.d(TAG, String.format("handleClick() label=%s key=%s package=%s",
                diffEntry.getAppLabel(), histEntry.getKey(), histEntry.mPackageName));
        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, diffEntry, powerPref.getPercent(),
                isValidToShowSummary(packageName), getSlotInformation());
        return true;
    }

    @Override
    public void onExpand(boolean isExpanded) {
        mIsExpanded = isExpanded;
        mMetricsFeatureProvider.action(
                mPrefContext,
                SettingsEnums.ACTION_BATTERY_USAGE_EXPAND_ITEM,
                isExpanded);
        refreshExpandUi();
    }

    void setBatteryHistoryMap(
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        Log.d(TAG, "setBatteryHistoryMap() " + (batteryHistoryMap == null ? "null"
                : ("size=" + batteryHistoryMap.size())));
        // Ensure the battery chart group is visible for users.
        animateBatteryChartViewGroup();
        final BatteryLevelData batteryLevelData =
                DataProcessor.getBatteryLevelData(mContext, mHandler, batteryHistoryMap,
                        batteryUsageMap -> {
                            mBatteryUsageMap = batteryUsageMap;
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
                    mHourlyChartLabelTextGenerator));
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
            requestAccessibilityFocusForCategoryTitle(mDailyChartView);
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
            requestAccessibilityFocusForCategoryTitle(mHourlyChartView);
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

        mHandler.post(() -> {
            final long start = System.currentTimeMillis();
            removeAndCacheAllPrefs();
            addAllPreferences();
            refreshCategoryTitle();
            Log.d(TAG, String.format("refreshUi is finished in %d/ms",
                    (System.currentTimeMillis() - start)));
        });
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
            removeAndCacheAllPrefs();
            addFooterPreferenceIfNeeded(false);
            return false;
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
            hourlyViewModel.setSelectedIndex(mHourlyChartIndex);
            mHourlyChartView.setViewModel(hourlyViewModel);
        }

        if (mBatteryUsageMap == null) {
            // Battery usage data is not ready, wait for data ready to refresh UI.
            return false;
        }
        return true;
    }

    private void addAllPreferences() {
        final BatteryDiffData batteryDiffData =
                mBatteryUsageMap.get(mDailyChartIndex).get(mHourlyChartIndex);
        addFooterPreferenceIfNeeded(batteryDiffData != null
                && (!batteryDiffData.getAppDiffEntryList().isEmpty()
                || !batteryDiffData.getSystemDiffEntryList().isEmpty()));
        if (batteryDiffData == null) {
            Log.w(TAG, "cannot find BatteryDiffEntry for daily_index: " + mDailyChartIndex
                    + " hourly_index: " + mHourlyChartIndex);
            return;
        }
        // Adds app entries to the list if it is not empty.
        if (!batteryDiffData.getAppDiffEntryList().isEmpty()) {
            addPreferenceToScreen(batteryDiffData.getAppDiffEntryList());
        }
        // Adds the expandable divider if we have system entries data.
        if (!batteryDiffData.getSystemDiffEntryList().isEmpty()) {
            if (mExpandDividerPreference == null) {
                mExpandDividerPreference = new ExpandDividerPreference(mPrefContext);
                mExpandDividerPreference.setOnExpandListener(this);
                mExpandDividerPreference.setIsExpanded(mIsExpanded);
            }
            mExpandDividerPreference.setOrder(
                    mAppListPrefGroup.getPreferenceCount());
            mAppListPrefGroup.addPreference(mExpandDividerPreference);
        }
        refreshExpandUi();
    }

    @VisibleForTesting
    void addPreferenceToScreen(List<BatteryDiffEntry> entries) {
        if (mAppListPrefGroup == null || entries.isEmpty()) {
            return;
        }
        int prefIndex = mAppListPrefGroup.getPreferenceCount();
        for (BatteryDiffEntry entry : entries) {
            boolean isAdded = false;
            final String appLabel = entry.getAppLabel();
            final Drawable appIcon = entry.getAppIcon();
            if (TextUtils.isEmpty(appLabel) || appIcon == null) {
                Log.w(TAG, "cannot find app resource for:" + entry.getPackageName());
                continue;
            }
            final String prefKey = entry.mBatteryHistEntry.getKey();
            PowerGaugePreference pref = mAppListPrefGroup.findPreference(prefKey);
            if (pref != null) {
                isAdded = true;
                Log.w(TAG, "preference should be removed for:" + entry.getPackageName());
            } else {
                pref = (PowerGaugePreference) mPreferenceCache.get(prefKey);
            }
            // Creates new innstance if cached preference is not found.
            if (pref == null) {
                pref = new PowerGaugePreference(mPrefContext);
                pref.setKey(prefKey);
                mPreferenceCache.put(prefKey, pref);
            }
            pref.setIcon(appIcon);
            pref.setTitle(appLabel);
            pref.setOrder(prefIndex);
            pref.setPercent(entry.getPercentOfTotal());
            pref.setSingleLineTitle(true);
            // Sets the BatteryDiffEntry to preference for launching detailed page.
            pref.setBatteryDiffEntry(entry);
            pref.setEnabled(entry.validForRestriction());
            setPreferenceSummary(pref, entry);
            if (!isAdded) {
                mAppListPrefGroup.addPreference(pref);
            }
            appIcon.setAlpha(pref.isEnabled() ? ENABLED_ICON_ALPHA : DISABLED_ICON_ALPHA);
            prefIndex++;
        }
    }

    private void removeAndCacheAllPrefs() {
        if (mAppListPrefGroup == null
                || mAppListPrefGroup.getPreferenceCount() == 0) {
            return;
        }
        final int prefsCount = mAppListPrefGroup.getPreferenceCount();
        for (int index = 0; index < prefsCount; index++) {
            final Preference pref = mAppListPrefGroup.getPreference(index);
            if (TextUtils.isEmpty(pref.getKey())) {
                continue;
            }
            mPreferenceCache.put(pref.getKey(), pref);
        }
        mAppListPrefGroup.removeAll();
    }

    private void refreshExpandUi() {
        final List<BatteryDiffEntry> systemEntries = mBatteryUsageMap.get(mDailyChartIndex).get(
                mHourlyChartIndex).getSystemDiffEntryList();
        if (mIsExpanded) {
            addPreferenceToScreen(systemEntries);
        } else {
            // Removes and recycles all system entries to hide all of them.
            for (BatteryDiffEntry entry : systemEntries) {
                final String prefKey = entry.mBatteryHistEntry.getKey();
                final Preference pref = mAppListPrefGroup.findPreference(prefKey);
                if (pref != null) {
                    mAppListPrefGroup.removePreference(pref);
                    mPreferenceCache.put(pref.getKey(), pref);
                }
            }
        }
    }

    @VisibleForTesting
    void refreshCategoryTitle() {
        final String slotInformation = getSlotInformation();
        Log.d(TAG, String.format("refreshCategoryTitle:%s", slotInformation));
        if (mAppListPrefGroup != null) {
            mAppListPrefGroup.setTitle(
                    getSlotInformation(/*isApp=*/ true, slotInformation));
        }
        if (mExpandDividerPreference != null) {
            mExpandDividerPreference.setTitle(
                    getSlotInformation(/*isApp=*/ false, slotInformation));
        }
    }

    private void requestAccessibilityFocusForCategoryTitle(View view) {
        if (!AccessibilityManager.getInstance(mContext).isEnabled()) {
            return;
        }
        if (mCategoryTitleView == null) {
            mCategoryTitleView = view.getRootView().findViewById(com.android.internal.R.id.title);
        }
        if (mCategoryTitleView != null) {
            mCategoryTitleView.requestAccessibilityFocus();
        }
    }

    private String getSlotInformation(boolean isApp, String slotInformation) {
        // TODO: Updates the right slot information from daily and hourly chart selection.
        // Null means we show all information without a specific time slot.
        if (slotInformation == null) {
            return isApp
                    ? mPrefContext.getString(R.string.battery_app_usage)
                    : mPrefContext.getString(R.string.battery_system_usage);
        } else {
            return isApp
                    ? mPrefContext.getString(R.string.battery_app_usage_for, slotInformation)
                    : mPrefContext.getString(R.string.battery_system_usage_for, slotInformation);
        }
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

        return String.format("%s %s", selectedDayText, selectedHourText);
    }

    @VisibleForTesting
    void setPreferenceSummary(
            PowerGaugePreference preference, BatteryDiffEntry entry) {
        final long foregroundUsageTimeInMs = entry.mForegroundUsageTimeInMs;
        final long backgroundUsageTimeInMs = entry.mBackgroundUsageTimeInMs;
        final long totalUsageTimeInMs = foregroundUsageTimeInMs + backgroundUsageTimeInMs;
        // Checks whether the package is allowed to show summary or not.
        if (!isValidToShowSummary(entry.getPackageName())) {
            preference.setSummary(null);
            return;
        }
        String usageTimeSummary = null;
        // Not shows summary for some system components without usage time.
        if (totalUsageTimeInMs == 0) {
            preference.setSummary(null);
            // Shows background summary only if we don't have foreground usage time.
        } else if (foregroundUsageTimeInMs == 0 && backgroundUsageTimeInMs != 0) {
            usageTimeSummary = buildUsageTimeInfo(backgroundUsageTimeInMs, true);
            // Shows total usage summary only if total usage time is small.
        } else if (totalUsageTimeInMs < DateUtils.MINUTE_IN_MILLIS) {
            usageTimeSummary = buildUsageTimeInfo(totalUsageTimeInMs, false);
        } else {
            usageTimeSummary = buildUsageTimeInfo(totalUsageTimeInMs, false);
            // Shows background usage time if it is larger than a minute.
            if (backgroundUsageTimeInMs > 0) {
                usageTimeSummary +=
                        "\n" + buildUsageTimeInfo(backgroundUsageTimeInMs, true);
            }
        }
        preference.setSummary(usageTimeSummary);
    }

    private String buildUsageTimeInfo(long usageTimeInMs, boolean isBackground) {
        if (usageTimeInMs < DateUtils.MINUTE_IN_MILLIS) {
            return mPrefContext.getString(
                    isBackground
                            ? R.string.battery_usage_background_less_than_one_minute
                            : R.string.battery_usage_total_less_than_one_minute);
        }
        final CharSequence timeSequence =
                StringUtil.formatElapsedTime(mPrefContext, usageTimeInMs,
                        /*withSeconds=*/ false, /*collapseTimeUnit=*/ false);
        final int resourceId =
                isBackground
                        ? R.string.battery_usage_for_background_time
                        : R.string.battery_usage_for_total_time;
        return mPrefContext.getString(resourceId, timeSequence);
    }

    @VisibleForTesting
    boolean isValidToShowSummary(String packageName) {
        return !DataProcessor.contains(packageName, mNotAllowShowSummaryPackages);
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

    private void addFooterPreferenceIfNeeded(boolean containAppItems) {
        if (mIsFooterPrefAdded || mFooterPreference == null) {
            return;
        }
        mIsFooterPrefAdded = true;
        mFooterPreference.setTitle(mPrefContext.getString(
                containAppItems
                        ? R.string.battery_usage_screen_footer
                        : R.string.battery_usage_screen_footer_empty));
        mHandler.post(() -> mPreferenceScreen.addPreference(mFooterPreference));
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
        return (int) ((dailyTimestamps.get(dailyTimestamps.size() - 1) - dailyTimestamps.get(0))
                / DateUtils.HOUR_IN_MILLIS);
    }

    /** Used for {@link AppBatteryPreferenceController}. */
    public static List<BatteryDiffEntry> getAppBatteryUsageData(Context context) {
        final long start = System.currentTimeMillis();
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getBatteryHistorySinceLastFullCharge(context);
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
        @Override
        public String generateText(List<Long> timestamps, int index) {
            return ConvertUtils.utcToLocalTimeHour(mContext, timestamps.get(index),
                    mIs24HourFormat);
        }

        @Override
        public String generateFullText(List<Long> timestamps, int index) {
            return index == timestamps.size() - 1
                    ? generateText(timestamps, index)
                    : String.format("%s%s%s", generateText(timestamps, index),
                            mIs24HourFormat ? "-" : " - ", generateText(timestamps, index + 1));
        }
    }
}
