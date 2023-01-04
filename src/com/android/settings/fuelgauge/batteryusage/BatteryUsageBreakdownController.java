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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.FooterPreference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controller for battery usage breakdown preference group. */
public class BatteryUsageBreakdownController extends BasePreferenceController
        implements LifecycleObserver, OnDestroy  {
    private static final String TAG = "BatteryUsageBreakdownController";
    private static final String ROOT_PREFERENCE_KEY = "battery_usage_breakdown";
    private static final String FOOTER_PREFERENCE_KEY = "battery_usage_footer";
    private static final String SPINNER_PREFERENCE_KEY = "battery_usage_spinner";
    private static final String APP_LIST_PREFERENCE_KEY = "app_list";
    private static final String PACKAGE_NAME_NONE = "none";
    private static final int ENABLED_ICON_ALPHA = 255;
    private static final int DISABLED_ICON_ALPHA = 255 / 3;

    private final SettingsActivity mActivity;
    private final InstrumentedPreferenceFragment mFragment;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @VisibleForTesting
    final Map<String, Preference> mPreferenceCache = new HashMap<>();

    private int mSpinnerPosition;
    private String mSlotTimestamp;

    @VisibleForTesting
    Context mPrefContext;
    @VisibleForTesting
    PreferenceCategory mRootPreference;
    @VisibleForTesting
    SpinnerPreference mSpinnerPreference;
    @VisibleForTesting
    PreferenceGroup mAppListPreferenceGroup;
    @VisibleForTesting
    FooterPreference mFooterPreference;
    @VisibleForTesting
    BatteryDiffData mBatteryDiffData;

    public BatteryUsageBreakdownController(
            Context context, Lifecycle lifecycle, SettingsActivity activity,
            InstrumentedPreferenceFragment fragment) {
        super(context, ROOT_PREFERENCE_KEY);
        mActivity = activity;
        mFragment = fragment;
        mMetricsFeatureProvider =
                FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(/*token=*/ null);
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
                mActivity, mFragment, diffEntry, powerPref.getPercent(), mSlotTimestamp);
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

        mAppListPreferenceGroup.setOrderingAsAdded(false);
        mSpinnerPreference.initializeSpinner(
                new String[]{
                        mPrefContext.getString(R.string.battery_usage_spinner_breakdown_by_apps),
                        mPrefContext.getString(R.string.battery_usage_spinner_breakdown_by_system)
                },
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (mSpinnerPosition != position) {
                            mSpinnerPosition = position;
                            mHandler.post(() -> {
                                removeAndCacheAllPreferences();
                                addAllPreferences();
                            });
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
    }

    /**
     * Updates UI when the battery usage is updated.
     * @param slotUsageData The battery usage diff data for the selected slot. This is used in
     *                      the app list.
     * @param slotTimestamp The selected slot timestamp information. This is used in the battery
     *                      usage breakdown category.
     * @param isAllUsageDataEmpty Whether all the battery usage data is null or empty. This is
     *                            used when showing the footer.
     */
    void handleBatteryUsageUpdated(
            BatteryDiffData slotUsageData, String slotTimestamp, boolean isAllUsageDataEmpty) {
        mBatteryDiffData = slotUsageData;
        mSlotTimestamp = slotTimestamp;

        showCategoryTitle(slotTimestamp);
        showSpinnerAndAppList();
        showFooterPreference(isAllUsageDataEmpty);
    }

    // TODO: request accessibility focus on category title when slot selection updated.
    private void showCategoryTitle(String slotTimestamp) {
        mRootPreference.setTitle(slotTimestamp == null
                ? mPrefContext.getString(
                        R.string.battery_usage_breakdown_title_since_last_full_charge)
                : mPrefContext.getString(
                        R.string.battery_usage_breakdown_title_for_slot, slotTimestamp));
        mRootPreference.setVisible(true);
    }

    private void showFooterPreference(boolean isAllBatteryUsageEmpty) {
        mFooterPreference.setTitle(mPrefContext.getString(
                isAllBatteryUsageEmpty
                        ? R.string.battery_usage_screen_footer_empty
                        : R.string.battery_usage_screen_footer));
        mFooterPreference.setVisible(true);
    }

    private void showSpinnerAndAppList() {
        removeAndCacheAllPreferences();
        if (mBatteryDiffData == null) {
            return;
        }
        mSpinnerPreference.setVisible(true);
        mAppListPreferenceGroup.setVisible(true);
        mHandler.post(() -> {
            addAllPreferences();
        });
    }

    @VisibleForTesting
    void addAllPreferences() {
        if (mBatteryDiffData == null) {
            return;
        }
        final long start = System.currentTimeMillis();
        final List<BatteryDiffEntry> entries = mSpinnerPosition == 0
                ? mBatteryDiffData.getAppDiffEntryList()
                : mBatteryDiffData.getSystemDiffEntryList();
        int prefIndex = mAppListPreferenceGroup.getPreferenceCount();
        for (BatteryDiffEntry entry : entries) {
            boolean isAdded = false;
            final String appLabel = entry.getAppLabel();
            final Drawable appIcon = entry.getAppIcon();
            if (TextUtils.isEmpty(appLabel) || appIcon == null) {
                Log.w(TAG, "cannot find app resource for:" + entry.getPackageName());
                continue;
            }
            final String prefKey = entry.mBatteryHistEntry.getKey();
            PowerGaugePreference pref = mAppListPreferenceGroup.findPreference(prefKey);
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
                mAppListPreferenceGroup.addPreference(pref);
            }
            appIcon.setAlpha(pref.isEnabled() ? ENABLED_ICON_ALPHA : DISABLED_ICON_ALPHA);
            prefIndex++;
        }
        Log.d(TAG, String.format("addAllPreferences() is finished in %d/ms",
                (System.currentTimeMillis() - start)));
    }

    @VisibleForTesting
    void removeAndCacheAllPreferences() {
        final int prefsCount = mAppListPreferenceGroup.getPreferenceCount();
        for (int index = 0; index < prefsCount; index++) {
            final Preference pref = mAppListPreferenceGroup.getPreference(index);
            if (TextUtils.isEmpty(pref.getKey())) {
                continue;
            }
            mPreferenceCache.put(pref.getKey(), pref);
        }
        mAppListPreferenceGroup.removeAll();
    }

    @VisibleForTesting
    void setPreferenceSummary(
            PowerGaugePreference preference, BatteryDiffEntry entry) {
        final long foregroundUsageTimeInMs = entry.mForegroundUsageTimeInMs;
        final long backgroundUsageTimeInMs = entry.mBackgroundUsageTimeInMs;
        final long totalUsageTimeInMs = foregroundUsageTimeInMs + backgroundUsageTimeInMs;
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
                StringUtil.formatElapsedTime(mPrefContext, (double) usageTimeInMs,
                        /*withSeconds=*/ false, /*collapseTimeUnit=*/ false);
        final int resourceId =
                isBackground
                        ? R.string.battery_usage_for_background_time
                        : R.string.battery_usage_for_total_time;
        return mPrefContext.getString(resourceId, timeSequence);
    }
}
