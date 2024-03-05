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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Controller for battery usage breakdown preference group. */
public class BatteryUsageBreakdownController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnDestroy {
    private static final String TAG = "BatteryUsageBreakdownController";
    private static final String ROOT_PREFERENCE_KEY = "battery_usage_breakdown";
    private static final String FOOTER_PREFERENCE_KEY = "battery_usage_footer";
    private static final String SPINNER_PREFERENCE_KEY = "battery_usage_spinner";
    private static final String APP_LIST_PREFERENCE_KEY = "app_list";
    private static final String PACKAGE_NAME_NONE = "none";
    private static final String SLOT_TIMESTAMP = "slot_timestamp";
    private static final String ANOMALY_KEY = "anomaly_key";
    private static final List<BatteryDiffEntry> EMPTY_ENTRY_LIST = new ArrayList<>();

    private static int sUiMode = Configuration.UI_MODE_NIGHT_UNDEFINED;

    private final SettingsActivity mActivity;
    private final InstrumentedPreferenceFragment mFragment;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @VisibleForTesting final Map<String, Preference> mPreferenceCache = new ArrayMap<>();

    private int mSpinnerPosition;
    private String mSlotInformation;

    @VisibleForTesting Context mPrefContext;
    @VisibleForTesting PreferenceCategory mRootPreference;
    @VisibleForTesting SpinnerPreference mSpinnerPreference;
    @VisibleForTesting PreferenceGroup mAppListPreferenceGroup;
    @VisibleForTesting FooterPreference mFooterPreference;
    @VisibleForTesting BatteryDiffData mBatteryDiffData;
    @VisibleForTesting String mPercentLessThanThresholdText;
    @VisibleForTesting boolean mIsHighlightSlot;
    @VisibleForTesting int mAnomalyKeyNumber;
    @VisibleForTesting String mAnomalyEntryKey;
    @VisibleForTesting String mAnomalyHintString;
    @VisibleForTesting String mAnomalyHintPrefKey;

    public BatteryUsageBreakdownController(
            Context context,
            Lifecycle lifecycle,
            SettingsActivity activity,
            InstrumentedPreferenceFragment fragment) {
        super(context, ROOT_PREFERENCE_KEY);
        mActivity = activity;
        mFragment = fragment;
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onResume() {
        final int currentUiMode =
                mContext.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;
        if (sUiMode != currentUiMode) {
            sUiMode = currentUiMode;
            BatteryDiffEntry.clearCache();
            mPreferenceCache.clear();
            Log.d(TAG, "clear icon and label cache since uiMode is changed");
        }
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(/* token= */ null);
        mPreferenceCache.clear();
        mAppListPreferenceGroup.removeAll();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    private boolean isAnomalyBatteryDiffEntry(BatteryDiffEntry entry) {
        return mIsHighlightSlot
                && mAnomalyEntryKey != null
                && mAnomalyEntryKey.equals(entry.getKey());
    }

    private void logPreferenceClickedMetrics(BatteryDiffEntry entry) {
        final int attribution = SettingsEnums.OPEN_BATTERY_USAGE;
        final int action = entry.isSystemEntry()
                ? SettingsEnums.ACTION_BATTERY_USAGE_SYSTEM_ITEM
                : SettingsEnums.ACTION_BATTERY_USAGE_APP_ITEM;
        final int pageId = SettingsEnums.OPEN_BATTERY_USAGE;
        final String packageName =
                TextUtils.isEmpty(entry.getPackageName())
                        ? PACKAGE_NAME_NONE
                        : entry.getPackageName();
        final int percentage = (int) Math.round(entry.getPercentage());
        final int slotTimestamp = (int) (mBatteryDiffData.getStartTimestamp() / 1000);
        mMetricsFeatureProvider.action(attribution, action, pageId, packageName, percentage);
        mMetricsFeatureProvider.action(attribution, action, pageId, SLOT_TIMESTAMP, slotTimestamp);

        if (isAnomalyBatteryDiffEntry(entry)) {
            mMetricsFeatureProvider.action(
                    attribution, action, pageId, ANOMALY_KEY, mAnomalyKeyNumber);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof PowerGaugePreference)) {
            return false;
        }
        final PowerGaugePreference powerPref = (PowerGaugePreference) preference;
        final BatteryDiffEntry diffEntry = powerPref.getBatteryDiffEntry();
        logPreferenceClickedMetrics(diffEntry);
        Log.d(
                TAG,
                String.format(
                        "handleClick() label=%s key=%s package=%s",
                        diffEntry.getAppLabel(), diffEntry.getKey(), diffEntry.getPackageName()));
        final String anomalyHintPrefKey =
                isAnomalyBatteryDiffEntry(diffEntry) ? mAnomalyHintPrefKey : null;
        final String anomalyHintText =
                isAnomalyBatteryDiffEntry(diffEntry) ? mAnomalyHintString : null;
        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity,
                mFragment.getMetricsCategory(),
                diffEntry,
                powerPref.getPercentage(),
                mSlotInformation,
                /* showTimeInformation= */ true,
                anomalyHintPrefKey,
                anomalyHintText);
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrefContext = screen.getContext();
        mRootPreference = screen.findPreference(ROOT_PREFERENCE_KEY);
        mSpinnerPreference = screen.findPreference(SPINNER_PREFERENCE_KEY);
        mAppListPreferenceGroup = screen.findPreference(APP_LIST_PREFERENCE_KEY);
        mFooterPreference = screen.findPreference(FOOTER_PREFERENCE_KEY);
        mPercentLessThanThresholdText =
                mPrefContext.getString(
                        R.string.battery_usage_less_than_percent,
                        Utils.formatPercentage(BatteryDiffData.SMALL_PERCENTAGE_THRESHOLD, false));

        mAppListPreferenceGroup.setOrderingAsAdded(false);
        mSpinnerPreference.initializeSpinner(
                new String[] {
                    mPrefContext.getString(R.string.battery_usage_spinner_view_by_apps),
                    mPrefContext.getString(R.string.battery_usage_spinner_view_by_systems)
                },
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (mSpinnerPosition != position) {
                            mSpinnerPosition = position;
                            mHandler.post(
                                    () -> {
                                        removeAndCacheAllUnusedPreferences();
                                        addAllPreferences();
                                        mMetricsFeatureProvider.action(
                                                mPrefContext,
                                                SettingsEnums.ACTION_BATTERY_USAGE_SPINNER,
                                                mSpinnerPosition);
                                    });
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
    }

    /**
     * Updates UI when the battery usage is updated.
     *
     * @param slotUsageData The battery usage diff data for the selected slot. This is used in the
     *     app list.
     * @param slotTimestamp The selected slot timestamp information. This is used in the battery
     *     usage breakdown category.
     * @param isAllUsageDataEmpty Whether all the battery usage data is null or empty. This is used
     *     when showing the footer.
     */
    void handleBatteryUsageUpdated(
            BatteryDiffData slotUsageData,
            String slotTimestamp,
            boolean isAllUsageDataEmpty,
            boolean isHighlightSlot,
            Optional<AnomalyEventWrapper> optionalAnomalyEventWrapper) {
        mBatteryDiffData = slotUsageData;
        mSlotInformation = slotTimestamp;
        mIsHighlightSlot = isHighlightSlot;

        if (optionalAnomalyEventWrapper != null) {
            final AnomalyEventWrapper anomalyEventWrapper =
                    optionalAnomalyEventWrapper.orElse(null);
            mAnomalyKeyNumber =
                    anomalyEventWrapper != null ? anomalyEventWrapper.getAnomalyKeyNumber() : -1;
            mAnomalyEntryKey =
                    anomalyEventWrapper != null ? anomalyEventWrapper.getAnomalyEntryKey() : null;
            mAnomalyHintString =
                    anomalyEventWrapper != null ? anomalyEventWrapper.getAnomalyHintString() : null;
            mAnomalyHintPrefKey =
                    anomalyEventWrapper != null
                            ? anomalyEventWrapper.getAnomalyHintPrefKey()
                            : null;
        }

        showCategoryTitle(slotTimestamp);
        showSpinnerAndAppList();
        showFooterPreference(isAllUsageDataEmpty);
    }

    private void showCategoryTitle(String slotTimestamp) {
        mRootPreference.setTitle(
                slotTimestamp == null
                        ? mPrefContext.getString(
                                R.string.battery_usage_breakdown_title_since_last_full_charge)
                        : mPrefContext.getString(
                                R.string.battery_usage_breakdown_title_for_slot, slotTimestamp));
        mRootPreference.setVisible(true);
    }

    private void showFooterPreference(boolean isAllBatteryUsageEmpty) {
        mFooterPreference.setTitle(
                mPrefContext.getString(
                        isAllBatteryUsageEmpty
                                ? R.string.battery_usage_screen_footer_empty
                                : R.string.battery_usage_screen_footer));
        mFooterPreference.setVisible(true);
    }

    private void showSpinnerAndAppList() {
        if (mBatteryDiffData == null) {
            mHandler.post(
                    () -> {
                        removeAndCacheAllUnusedPreferences();
                    });
            return;
        }
        mSpinnerPreference.setVisible(true);
        mAppListPreferenceGroup.setVisible(true);
        mHandler.post(
                () -> {
                    removeAndCacheAllUnusedPreferences();
                    addAllPreferences();
                });
    }

    private List<BatteryDiffEntry> getBatteryDiffEntries() {
        if (mBatteryDiffData == null) {
            return EMPTY_ENTRY_LIST;
        }
        return mSpinnerPosition == 0
                ? mBatteryDiffData.getAppDiffEntryList()
                : mBatteryDiffData.getSystemDiffEntryList();
    }

    @VisibleForTesting
    void addAllPreferences() {
        if (mBatteryDiffData == null) {
            return;
        }
        final long start = System.currentTimeMillis();
        final List<BatteryDiffEntry> entries = getBatteryDiffEntries();
        int prefIndex = mAppListPreferenceGroup.getPreferenceCount();
        for (BatteryDiffEntry entry : entries) {
            boolean isAdded = false;
            final String appLabel = entry.getAppLabel();
            final Drawable appIcon = entry.getAppIcon();
            if (TextUtils.isEmpty(appLabel) || appIcon == null) {
                Log.w(TAG, "cannot find app resource for:" + entry.getPackageName());
                continue;
            }
            final String prefKey = entry.getKey();
            AnomalyAppItemPreference pref = mAppListPreferenceGroup.findPreference(prefKey);
            if (pref != null) {
                isAdded = true;
            } else {
                pref = (AnomalyAppItemPreference) mPreferenceCache.get(prefKey);
            }
            // Creates new instance if cached preference is not found.
            if (pref == null) {
                pref = new AnomalyAppItemPreference(mPrefContext);
                pref.setKey(prefKey);
                mPreferenceCache.put(prefKey, pref);
            }
            pref.setIcon(appIcon);
            pref.setTitle(appLabel);
            pref.setOrder(prefIndex);
            pref.setSingleLineTitle(true);
            // Updates App item preference style
            pref.setAnomalyHint(isAnomalyBatteryDiffEntry(entry) ? mAnomalyHintString : null);
            // Sets the BatteryDiffEntry to preference for launching detailed page.
            pref.setBatteryDiffEntry(entry);
            pref.setSelectable(entry.validForRestriction());
            setPreferencePercentage(pref, entry);
            setPreferenceSummary(pref, entry);
            if (!isAdded) {
                mAppListPreferenceGroup.addPreference(pref);
            }
            prefIndex++;
        }
        Log.d(
                TAG,
                String.format(
                        "addAllPreferences() is finished in %d/ms",
                        (System.currentTimeMillis() - start)));
    }

    @VisibleForTesting
    void removeAndCacheAllUnusedPreferences() {
        List<BatteryDiffEntry> entries = getBatteryDiffEntries();
        Set<String> entryKeySet = new ArraySet<>(entries.size());
        entries.forEach(entry -> entryKeySet.add(entry.getKey()));
        final int prefsCount = mAppListPreferenceGroup.getPreferenceCount();
        for (int index = prefsCount - 1; index >= 0; index--) {
            final Preference pref = mAppListPreferenceGroup.getPreference(index);
            if (entryKeySet.contains(pref.getKey())) {
                // The pref is still used, don't remove.
                continue;
            }
            if (!TextUtils.isEmpty(pref.getKey())) {
                mPreferenceCache.put(pref.getKey(), pref);
            }
            mAppListPreferenceGroup.removePreference(pref);
        }
    }

    @VisibleForTesting
    void setPreferencePercentage(PowerGaugePreference preference, BatteryDiffEntry entry) {
        preference.setPercentage(
                entry.getPercentage() < BatteryDiffData.SMALL_PERCENTAGE_THRESHOLD
                        ? mPercentLessThanThresholdText
                        : Utils.formatPercentage(
                                entry.getPercentage() + entry.getAdjustPercentageOffset(),
                                /* round= */ true));
    }

    @VisibleForTesting
    void setPreferenceSummary(PowerGaugePreference preference, BatteryDiffEntry entry) {
        preference.setSummary(
                BatteryUtils.buildBatteryUsageTimeSummary(
                        mPrefContext,
                        entry.isSystemEntry(),
                        entry.mForegroundUsageTimeInMs,
                        entry.mBackgroundUsageTimeInMs + entry.mForegroundServiceUsageTimeInMs,
                        entry.mScreenOnTimeInMs));
    }
}
