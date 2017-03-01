/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.annotation.StringRes;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.PowerProfile;
import com.android.settings.R;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.applications.ManageApplications;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.display.AutoBrightnessPreferenceController;
import com.android.settings.display.TimeoutPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.BatteryInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was
 * consumed since the last time it was unplugged.
 */
public class PowerUsageSummary extends PowerUsageBase {

    static final String TAG = "PowerUsageSummary";

    private static final boolean DEBUG = false;
    private static final boolean USE_FAKE_DATA = false;
    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_BATTERY_HEADER = "battery_header";

    private static final int MIN_POWER_THRESHOLD_MILLI_AMP = 5;
    private static final int MAX_ITEMS_TO_LIST = USE_FAKE_DATA ? 30 : 10;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;
    private static final int SECONDS_IN_HOUR = 60 * 60;

    private static final String KEY_SCREEN_USAGE = "screen_usage";
    private static final String KEY_SCREEN_CONSUMPTION = "screen_consumption";
    private static final String KEY_CELLULAR_NETWORK = "cellular_network";


    private static final int MENU_STATS_TYPE = Menu.FIRST;
    private static final int MENU_HIGH_POWER_APPS = Menu.FIRST + 3;
    @VisibleForTesting
    static final int MENU_ADDITIONAL_BATTERY_INFO = Menu.FIRST + 4;
    @VisibleForTesting
    static final int MENU_TOGGLE_APPS = Menu.FIRST + 5;
    private static final int MENU_HELP = Menu.FIRST + 6;

    @VisibleForTesting
    boolean mShowAllApps = false;
    @VisibleForTesting
    Preference mScreenUsagePref;
    @VisibleForTesting
    Preference mScreenConsumptionPref;
    @VisibleForTesting
    Preference mCellularNetworkPref;
    @VisibleForTesting
    PowerUsageFeatureProvider mPowerFeatureProvider;

    private LayoutPreference mBatteryLayoutPref;
    private PreferenceGroup mAppListGroup;
    private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);

        mBatteryLayoutPref = (LayoutPreference) findPreference(KEY_BATTERY_HEADER);
        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        mScreenUsagePref = findPreference(KEY_SCREEN_USAGE);
        mScreenConsumptionPref = findPreference(KEY_SCREEN_CONSUMPTION);
        mCellularNetworkPref = findPreference(KEY_CELLULAR_NETWORK);

        initFeatureProvider();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FUELGAUGE_POWER_USAGE_SUMMARY;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    @Override
    public void onPause() {
        BatteryEntry.stopRequestQueue();
        mHandler.removeMessages(BatteryEntry.MSG_UPDATE_NAME_ICON);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_BATTERY_HEADER.equals(preference.getKey())) {
            performBatteryHeaderClick();
            return true;
        } else if (!(preference instanceof PowerGaugePreference)) {
            return super.onPreferenceTreeClick(preference);
        }
        PowerGaugePreference pgp = (PowerGaugePreference) preference;
        BatteryEntry entry = pgp.getInfo();
        PowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(), this,
                mStatsHelper, mStatsType, entry, true, true);
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_summary;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new AutoBrightnessPreferenceController(context));
        controllers.add(new TimeoutPreferenceController(context));
        controllers.add(new BatterySaverController(context, getLifecycle()));
        return controllers;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) {
            menu.add(Menu.NONE, MENU_STATS_TYPE, Menu.NONE, R.string.menu_stats_total)
                    .setIcon(com.android.internal.R.drawable.ic_menu_info_details)
                    .setAlphabeticShortcut('t');
        }

        menu.add(Menu.NONE, MENU_HIGH_POWER_APPS, Menu.NONE, R.string.high_power_apps);

        if (mPowerFeatureProvider.isAdditionalBatteryInfoEnabled()) {
            menu.add(Menu.NONE, MENU_ADDITIONAL_BATTERY_INFO,
                    Menu.NONE, R.string.additional_battery_info);
        }
        if (mPowerFeatureProvider.isPowerAccountingToggleEnabled()) {
            menu.add(Menu.NONE, MENU_TOGGLE_APPS, Menu.NONE,
                    mShowAllApps ? R.string.hide_extra_apps : R.string.show_all_apps);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_battery;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final SettingsActivity sa = (SettingsActivity) getActivity();
        switch (item.getItemId()) {
            case MENU_STATS_TYPE:
                if (mStatsType == BatteryStats.STATS_SINCE_CHARGED) {
                    mStatsType = BatteryStats.STATS_SINCE_UNPLUGGED;
                } else {
                    mStatsType = BatteryStats.STATS_SINCE_CHARGED;
                }
                refreshStats();
                return true;
            case MENU_HIGH_POWER_APPS:
                Bundle args = new Bundle();
                args.putString(ManageApplications.EXTRA_CLASSNAME,
                        HighPowerApplicationsActivity.class.getName());
                sa.startPreferencePanel(this, ManageApplications.class.getName(), args,
                        R.string.high_power_apps, null, null, 0);
                return true;
            case MENU_ADDITIONAL_BATTERY_INFO:
                startActivity(FeatureFactory.getFactory(getContext())
                        .getPowerUsageFeatureProvider(getContext())
                        .getAdditionalBatteryInfoIntent());
                return true;
            case MENU_TOGGLE_APPS:
                mShowAllApps = !mShowAllApps;
                item.setTitle(mShowAllApps ? R.string.hide_extra_apps : R.string.show_all_apps);
                refreshStats();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addNotAvailableMessage() {
        final String NOT_AVAILABLE = "not_available";
        Preference notAvailable = getCachedPreference(NOT_AVAILABLE);
        if (notAvailable == null) {
            notAvailable = new Preference(getPrefContext());
            notAvailable.setKey(NOT_AVAILABLE);
            notAvailable.setTitle(R.string.power_usage_not_available);
            mAppListGroup.addPreference(notAvailable);
        }
    }

    private void performBatteryHeaderClick() {
        final Context context = getContext();
        final PowerUsageFeatureProvider featureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);

        if (featureProvider.isAdvancedUiEnabled()) {
            Utils.startWithFragment(getContext(), PowerUsageAdvanced.class.getName(), null,
                    null, 0, R.string.advanced_battery_title, null, getMetricsCategory());
        } else {
            mStatsHelper.storeStatsHistoryInFile(BatteryHistoryDetail.BATTERY_HISTORY_FILE);
            Bundle args = new Bundle(2);
            args.putString(BatteryHistoryDetail.EXTRA_STATS,
                    BatteryHistoryDetail.BATTERY_HISTORY_FILE);
            args.putParcelable(BatteryHistoryDetail.EXTRA_BROADCAST,
                    mStatsHelper.getBatteryBroadcast());
            Utils.startWithFragment(getContext(), BatteryHistoryDetail.class.getName(), args,
                    null, 0, R.string.history_details_title, null, getMetricsCategory());
        }
    }

    private static boolean isSharedGid(int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(int uid) {
        return uid >= Process.SYSTEM_UID && uid < Process.FIRST_APPLICATION_UID;
    }

    /**
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that
     * exists for all users of the same app. We detect this case and merge the power use
     * for dex2oat to the device OWNER's use of the app.
     *
     * @return A sorted list of apps using power.
     */
    private static List<BatterySipper> getCoalescedUsageList(final List<BatterySipper> sippers) {
        final SparseArray<BatterySipper> uidList = new SparseArray<>();

        final ArrayList<BatterySipper> results = new ArrayList<>();
        final int numSippers = sippers.size();
        for (int i = 0; i < numSippers; i++) {
            BatterySipper sipper = sippers.get(i);
            if (sipper.getUid() > 0) {
                int realUid = sipper.getUid();

                // Check if this UID is a shared GID. If so, we combine it with the OWNER's
                // actual app UID.
                if (isSharedGid(sipper.getUid())) {
                    realUid = UserHandle.getUid(UserHandle.USER_SYSTEM,
                            UserHandle.getAppIdFromSharedAppGid(sipper.getUid()));
                }

                // Check if this UID is a system UID (mediaserver, logd, nfc, drm, etc).
                if (isSystemUid(realUid)
                        && !"mediaserver".equals(sipper.packageWithHighestDrain)) {
                    // Use the system UID for all UIDs running in their own sandbox that
                    // are not apps. We exclude mediaserver because we already are expected to
                    // report that as a separate item.
                    realUid = Process.SYSTEM_UID;
                }

                if (realUid != sipper.getUid()) {
                    // Replace the BatterySipper with a new one with the real UID set.
                    BatterySipper newSipper = new BatterySipper(sipper.drainType,
                            new FakeUid(realUid), 0.0);
                    newSipper.add(sipper);
                    newSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    newSipper.mPackages = sipper.mPackages;
                    sipper = newSipper;
                }

                int index = uidList.indexOfKey(realUid);
                if (index < 0) {
                    // New entry.
                    uidList.put(realUid, sipper);
                } else {
                    // Combine BatterySippers if we already have one with this UID.
                    final BatterySipper existingSipper = uidList.valueAt(index);
                    existingSipper.add(sipper);
                    if (existingSipper.packageWithHighestDrain == null
                            && sipper.packageWithHighestDrain != null) {
                        existingSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    }

                    final int existingPackageLen = existingSipper.mPackages != null ?
                            existingSipper.mPackages.length : 0;
                    final int newPackageLen = sipper.mPackages != null ?
                            sipper.mPackages.length : 0;
                    if (newPackageLen > 0) {
                        String[] newPackages = new String[existingPackageLen + newPackageLen];
                        if (existingPackageLen > 0) {
                            System.arraycopy(existingSipper.mPackages, 0, newPackages, 0,
                                    existingPackageLen);
                        }
                        System.arraycopy(sipper.mPackages, 0, newPackages, existingPackageLen,
                                newPackageLen);
                        existingSipper.mPackages = newPackages;
                    }
                }
            } else {
                results.add(sipper);
            }
        }

        final int numUidSippers = uidList.size();
        for (int i = 0; i < numUidSippers; i++) {
            results.add(uidList.valueAt(i));
        }

        // The sort order must have changed, so re-sort based on total power use.
        Collections.sort(results, new Comparator<BatterySipper>() {
            @Override
            public int compare(BatterySipper a, BatterySipper b) {
                return Double.compare(b.totalPowerMah, a.totalPowerMah);
            }
        });
        return results;
    }

    protected void refreshStats() {
        super.refreshStats();

        BatteryInfo.getBatteryInfo(getContext(), new BatteryInfo.Callback() {
            @Override
            public void onBatteryInfoLoaded(BatteryInfo info) {
                updateHeaderPreference(info);
            }
        });

        cacheRemoveAllPrefs(mAppListGroup);
        mAppListGroup.setOrderingAsAdded(false);
        boolean addedSome = false;

        final PowerProfile powerProfile = mStatsHelper.getPowerProfile();
        final BatteryStats stats = mStatsHelper.getStats();
        final double averagePower = powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
        final Context context = getContext();

        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorControlNormal, value, true);
        final int colorControl = context.getColor(value.resourceId);
        final String usedTime = context.getString(R.string.battery_used_for);
        final int dischargeAmount = USE_FAKE_DATA ? 5000
                : stats != null ? stats.getDischargeAmount(mStatsType) : 0;

        updateScreenPreference(dischargeAmount);
        updateCellularPreference(dischargeAmount);

        if (averagePower >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP || USE_FAKE_DATA) {
            final List<BatterySipper> usageList = getCoalescedUsageList(
                    USE_FAKE_DATA ? getFakeStats() : mStatsHelper.getUsageList());

            double hiddenPowerMah = mShowAllApps ? 0 : removeHiddenBatterySippers(usageList);

            final int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                final BatterySipper sipper = usageList.get(i);
                // Deduct the power of hidden items from total power, which is used to
                // calculate percentOfTotal
                double totalPower = USE_FAKE_DATA ?
                        4000 : mStatsHelper.getTotalPower() - hiddenPowerMah;

                // With deduction in totalPower, percentOfTotal is higher because it adds the part
                // used in screen, system, etc
                final double percentOfTotal = totalPower == 0 ? 0 :
                        ((sipper.totalPowerMah / totalPower) * dischargeAmount);

                if (((int) (percentOfTotal + .5)) < 1) {
                    continue;
                }
                if (sipper.drainType == BatterySipper.DrainType.OVERCOUNTED) {
                    // Don't show over-counted unless it is at least 2/3 the size of
                    // the largest real entry, and its percent of total is more significant
                    if (sipper.totalPowerMah < ((mStatsHelper.getMaxRealPower() * 2) / 3)) {
                        continue;
                    }
                    if (percentOfTotal < 10) {
                        continue;
                    }
                    if ("user".equals(Build.TYPE)) {
                        continue;
                    }
                }
                if (sipper.drainType == BatterySipper.DrainType.UNACCOUNTED) {
                    // Don't show over-counted unless it is at least 1/2 the size of
                    // the largest real entry, and its percent of total is more significant
                    if (sipper.totalPowerMah < (mStatsHelper.getMaxRealPower() / 2)) {
                        continue;
                    }
                    if (percentOfTotal < 5) {
                        continue;
                    }
                    if ("user".equals(Build.TYPE)) {
                        continue;
                    }
                }
                final UserHandle userHandle = new UserHandle(UserHandle.getUserId(sipper.getUid()));
                final BatteryEntry entry = new BatteryEntry(getActivity(), mHandler, mUm, sipper);
                final Drawable badgedIcon = mUm.getBadgedIconForUser(entry.getIcon(),
                        userHandle);
                final CharSequence contentDescription = mUm.getBadgedLabelForUser(entry.getLabel(),
                        userHandle);

                final String key = extractKeyFromSipper(sipper);
                PowerGaugePreference pref = (PowerGaugePreference) getCachedPreference(key);
                if (pref == null) {
                    pref = new PowerGaugePreference(getPrefContext(), badgedIcon,
                            contentDescription, entry);
                    pref.setKey(key);
                }

                final double percentOfMax = (sipper.totalPowerMah * 100)
                        / mStatsHelper.getMaxPower();
                sipper.percent = percentOfTotal;
                pref.setTitle(entry.getLabel());
                pref.setOrder(i + 1);
                pref.setPercent(percentOfTotal);
                setUsageSummary(pref, usedTime, sipper.usageTimeMs);
                if ((sipper.drainType != DrainType.APP
                        || sipper.uidObj.getUid() == Process.ROOT_UID)
                        && sipper.drainType != DrainType.USER) {
                    pref.setTint(colorControl);
                }
                addedSome = true;
                mAppListGroup.addPreference(pref);
                if (mAppListGroup.getPreferenceCount() - getCachedCount()
                        > (MAX_ITEMS_TO_LIST + 1)) {
                    break;
                }
            }
        }
        if (!addedSome) {
            addNotAvailableMessage();
        }
        removeCachedPrefs(mAppListGroup);

        BatteryEntry.startRequestQueue();
    }

    @VisibleForTesting
    BatterySipper findBatterySipperByType(List<BatterySipper> usageList, DrainType type) {
        for (int i = 0, size = usageList.size(); i < size; i++) {
            final BatterySipper sipper = usageList.get(i);
            if (sipper.drainType == type) {
                return sipper;
            }
        }
        return null;
    }

    @VisibleForTesting
    void updateScreenPreference(final int dischargeAmount) {
        final BatterySipper sipper = findBatterySipperByType(
                mStatsHelper.getUsageList(), DrainType.SCREEN);
        final Context context = getContext();
        final double totalPowerMah = sipper != null ? sipper.totalPowerMah : 0;
        final long usageTimeMs = sipper != null ? sipper.usageTimeMs : 0;
        final double percentOfTotal = calculatePercentage(totalPowerMah, dischargeAmount);

        mScreenUsagePref.setSummary(getString(R.string.battery_used_for,
                Utils.formatElapsedTime(context, usageTimeMs, false)));
        mScreenConsumptionPref.setSummary(getString(R.string.battery_overall_usage,
                Utils.formatPercentage(percentOfTotal, true)));
    }

    @VisibleForTesting
    void updateCellularPreference(final int dischargeAmount) {
        final BatterySipper sipper = findBatterySipperByType(
                mStatsHelper.getUsageList(), DrainType.CELL);
        final double totalPowerMah = sipper != null ? sipper.totalPowerMah : 0;
        final double percentOfTotal = calculatePercentage(totalPowerMah, dischargeAmount);
        mCellularNetworkPref.setSummary(getString(R.string.battery_overall_usage,
                Utils.formatPercentage(percentOfTotal, true)));
    }

    @VisibleForTesting
    void updateHeaderPreference(BatteryInfo info) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final BatteryMeterView batteryView = (BatteryMeterView) mBatteryLayoutPref
                .findViewById(R.id.battery_header_icon);
        final TextView timeText = (TextView) mBatteryLayoutPref.findViewById(R.id.time);
        final TextView summary1 = (TextView) mBatteryLayoutPref.findViewById(R.id.summary1);
        final TextView summary2 = (TextView) mBatteryLayoutPref.findViewById(R.id.summary2);
        final int visible = info.remainingTimeUs != 0 ? View.VISIBLE : View.INVISIBLE;
        final int summaryResId = info.mDischarging ?
                R.string.estimated_time_left : R.string.estimated_charging_time_left;

        if (info.remainingTimeUs != 0) {
            timeText.setText(Utils.formatElapsedTime(context, info.remainingTimeUs / 1000, false));
        } else {
            timeText.setText(info.statusLabel);
        }

        summary1.setText(summaryResId);
        summary1.setVisibility(visible);
        summary2.setVisibility(visible);
        batteryView.setBatteryInfo(info.mBatteryLevel);
    }

    @VisibleForTesting
    double calculatePercentage(double powerUsage, double dischargeAmount) {
        final double totalPower = mStatsHelper.getTotalPower();
        return totalPower == 0 ? 0 :
                ((powerUsage / totalPower) * dischargeAmount);
    }

    @VisibleForTesting
    void setUsageSummary(Preference preference, String usedTimePrefix, long usageTimeMs) {
        // Only show summary when usage time is longer than one minute
        if (usageTimeMs >= DateUtils.MINUTE_IN_MILLIS) {
            preference.setSummary(String.format(usedTimePrefix,
                    Utils.formatElapsedTime(getContext(), usageTimeMs, false)));
        }
    }

    @VisibleForTesting
    boolean shouldHideSipper(BatterySipper sipper) {
        final DrainType drainType = sipper.drainType;

        return drainType == DrainType.IDLE || drainType == DrainType.CELL
                || drainType == DrainType.SCREEN
                || (sipper.totalPowerMah * SECONDS_IN_HOUR) < MIN_POWER_THRESHOLD_MILLI_AMP
                || mPowerFeatureProvider.isTypeService(sipper)
                || mPowerFeatureProvider.isTypeSystem(sipper);
    }

    @VisibleForTesting
    String extractKeyFromSipper(BatterySipper sipper) {
        if (sipper.uidObj != null) {
            return Integer.toString(sipper.getUid());
        } else if (sipper.drainType != DrainType.APP) {
            return sipper.drainType.toString();
        } else if (sipper.getPackages() != null) {
            return TextUtils.concat(sipper.getPackages()).toString();
        } else {
            Log.w(TAG, "Inappropriate BatterySipper without uid and package names: " + sipper);
            return "-1";
        }
    }

    @VisibleForTesting
    double removeHiddenBatterySippers(List<BatterySipper> sippers) {
        double totalPowerMah = 0;
        for (int i = sippers.size() - 1; i >= 0; i--) {
            final BatterySipper sipper = sippers.get(i);
            if (shouldHideSipper(sipper)) {
                sippers.remove(i);
                totalPowerMah += sipper.totalPowerMah;
            }
        }

        return totalPowerMah;
    }

    @VisibleForTesting
    void setBatteryLayoutPreference(LayoutPreference layoutPreference) {
        mBatteryLayoutPref = layoutPreference;
    }

    @VisibleForTesting
    void initFeatureProvider() {
        final Context context = getContext();
        mPowerFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
    }

    private static List<BatterySipper> getFakeStats() {
        ArrayList<BatterySipper> stats = new ArrayList<>();
        float use = 5;
        for (DrainType type : DrainType.values()) {
            if (type == DrainType.APP) {
                continue;
            }
            stats.add(new BatterySipper(type, null, use));
            use += 5;
        }
        for (int i = 0; i < 100; i++) {
            stats.add(new BatterySipper(DrainType.APP,
                    new FakeUid(Process.FIRST_APPLICATION_UID + i), use));
        }
        stats.add(new BatterySipper(DrainType.APP,
                new FakeUid(0), use));

        // Simulate dex2oat process.
        BatterySipper sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.FIRST_APPLICATION_UID)), 10.0f);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);

        sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.FIRST_APPLICATION_UID + 1)), 10.0f);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);

        sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.LOG_UID)), 9.0f);
        stats.add(sipper);

        return stats;
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryEntry.MSG_UPDATE_NAME_ICON:
                    BatteryEntry entry = (BatteryEntry) msg.obj;
                    PowerGaugePreference pgp =
                            (PowerGaugePreference) findPreference(
                                    Integer.toString(entry.sipper.uidObj.getUid()));
                    if (pgp != null) {
                        final int userId = UserHandle.getUserId(entry.sipper.getUid());
                        final UserHandle userHandle = new UserHandle(userId);
                        pgp.setIcon(mUm.getBadgedIconForUser(entry.getIcon(), userHandle));
                        pgp.setTitle(entry.name);
                        if (entry.sipper.drainType == DrainType.APP) {
                            pgp.setContentDescription(entry.name);
                        }
                    }
                    break;
                case BatteryEntry.MSG_REPORT_FULLY_DRAWN:
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                // TODO: Listen.
                BatteryInfo.getBatteryInfo(mContext, new BatteryInfo.Callback() {
                    @Override
                    public void onBatteryInfoLoaded(BatteryInfo info) {
                        mLoader.setSummary(SummaryProvider.this, info.mChargeLabelString);
                    }
                });
            }
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.power_usage_summary;
                    return Arrays.asList(sir);
                }
            };

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
}
