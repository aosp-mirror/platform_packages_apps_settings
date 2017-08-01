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

import android.app.Activity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
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
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

import com.android.internal.hardware.AmbientDisplayConfiguration;
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
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.display.AmbientDisplayPreferenceController;
import com.android.settings.display.AutoBrightnessPreferenceController;
import com.android.settings.display.BatteryPercentagePreferenceController;
import com.android.settings.display.TimeoutPreferenceController;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDetectionPolicy;
import com.android.settings.fuelgauge.anomaly.AnomalyDialogFragment.AnomalyDialogListener;
import com.android.settings.fuelgauge.anomaly.AnomalyLoader;
import com.android.settings.fuelgauge.anomaly.AnomalySummaryPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was
 * consumed since the last time it was unplugged.
 */
public class PowerUsageSummary extends PowerUsageBase implements
        AnomalyDialogListener, OnLongClickListener, OnClickListener {

    static final String TAG = "PowerUsageSummary";

    private static final boolean DEBUG = false;
    private static final boolean USE_FAKE_DATA = false;
    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_BATTERY_HEADER = "battery_header";
    private static final String KEY_SHOW_ALL_APPS = "show_all_apps";
    private static final int MAX_ITEMS_TO_LIST = USE_FAKE_DATA ? 30 : 10;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;

    private static final String KEY_SCREEN_USAGE = "screen_usage";
    private static final String KEY_TIME_SINCE_LAST_FULL_CHARGE = "last_full_charge";

    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness_battery";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout_battery";
    private static final String KEY_AMBIENT_DISPLAY = "ambient_display_battery";
    private static final String KEY_BATTERY_SAVER_SUMMARY = "battery_saver_summary";
    private static final String KEY_HIGH_USAGE = "high_usage";

    @VisibleForTesting
    static final int ANOMALY_LOADER = 1;
    @VisibleForTesting
    static final int BATTERY_INFO_LOADER = 2;
    private static final int MENU_STATS_TYPE = Menu.FIRST;
    @VisibleForTesting
    static final int MENU_HIGH_POWER_APPS = Menu.FIRST + 3;
    @VisibleForTesting
    static final int MENU_ADDITIONAL_BATTERY_INFO = Menu.FIRST + 4;
    @VisibleForTesting
    static final int MENU_TOGGLE_APPS = Menu.FIRST + 5;
    private static final int MENU_HELP = Menu.FIRST + 6;
    public static final int DEBUG_INFO_LOADER = 3;

    @VisibleForTesting
    boolean mShowAllApps = false;
    @VisibleForTesting
    PowerGaugePreference mScreenUsagePref;
    @VisibleForTesting
    PowerGaugePreference mLastFullChargePref;
    @VisibleForTesting
    PowerUsageFeatureProvider mPowerFeatureProvider;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    LayoutPreference mBatteryLayoutPref;

    /**
     * SparseArray that maps uid to {@link Anomaly}, so we could find {@link Anomaly} by uid
     */
    @VisibleForTesting
    SparseArray<List<Anomaly>> mAnomalySparseArray;
    @VisibleForTesting
    PreferenceGroup mAppListGroup;
    @VisibleForTesting
    BatteryHeaderPreferenceController mBatteryHeaderPreferenceController;
    private AnomalySummaryPreferenceController mAnomalySummaryPreferenceController;
    private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;

    private LoaderManager.LoaderCallbacks<List<Anomaly>> mAnomalyLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<Anomaly>>() {

                @Override
                public Loader<List<Anomaly>> onCreateLoader(int id, Bundle args) {
                    return new AnomalyLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<List<Anomaly>> loader, List<Anomaly> data) {
                    // show high usage preference if possible
                    mAnomalySummaryPreferenceController.updateAnomalySummaryPreference(data);

                    updateAnomalySparseArray(data);
                    refreshAnomalyIcon();
                }

                @Override
                public void onLoaderReset(Loader<List<Anomaly>> loader) {

                }
            };

    @VisibleForTesting
    LoaderManager.LoaderCallbacks<BatteryInfo> mBatteryInfoLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<BatteryInfo>() {

                @Override
                public Loader<BatteryInfo> onCreateLoader(int i, Bundle bundle) {
                    return new BatteryInfoLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<BatteryInfo> loader, BatteryInfo batteryInfo) {
                    mBatteryHeaderPreferenceController.updateHeaderPreference(batteryInfo);
                }

                @Override
                public void onLoaderReset(Loader<BatteryInfo> loader) {
                    // do nothing
                }
            };

    LoaderManager.LoaderCallbacks<List<BatteryInfo>> mBatteryInfoDebugLoaderCallbacks =
            new LoaderCallbacks<List<BatteryInfo>>() {
                @Override
                public Loader<List<BatteryInfo>> onCreateLoader(int i, Bundle bundle) {
                    return new DebugEstimatesLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<List<BatteryInfo>> loader,
                        List<BatteryInfo> batteryInfos) {
                    final BatteryMeterView batteryView = (BatteryMeterView) mBatteryLayoutPref
                            .findViewById(R.id.battery_header_icon);
                    final TextView percentRemaining =
                            mBatteryLayoutPref.findViewById(R.id.battery_percent);
                    final TextView summary1 = mBatteryLayoutPref.findViewById(R.id.summary1);
                    final TextView summary2 = mBatteryLayoutPref.findViewById(R.id.summary2);
                    BatteryInfo oldInfo = batteryInfos.get(0);
                    BatteryInfo newInfo = batteryInfos.get(1);
                    percentRemaining.setText(Utils.formatPercentage(oldInfo.batteryLevel));

                    // set the text to the old estimate (copied from battery info). Note that this
                    // can sometimes say 0 time remaining because battery stats requires the phone
                    // be unplugged for a period of time before being willing ot make an estimate.
                    summary1.setText(mPowerFeatureProvider.getOldEstimateDebugString(
                            Formatter.formatShortElapsedTime(getContext(),
                                    BatteryUtils.convertUsToMs(oldInfo.remainingTimeUs))));

                    // for this one we can just set the string directly
                    summary2.setText(mPowerFeatureProvider.getEnhancedEstimateDebugString(
                            Formatter.formatShortElapsedTime(getContext(),
                                    BatteryUtils.convertUsToMs(newInfo.remainingTimeUs))));

                    batteryView.setBatteryLevel(oldInfo.batteryLevel);
                    batteryView.setCharging(!oldInfo.discharging);
                }

                @Override
                public void onLoaderReset(Loader<List<BatteryInfo>> loader) {
                }
            };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);

        initFeatureProvider();
        mBatteryLayoutPref = (LayoutPreference) findPreference(KEY_BATTERY_HEADER);

        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        mScreenUsagePref = (PowerGaugePreference) findPreference(KEY_SCREEN_USAGE);
        mLastFullChargePref = (PowerGaugePreference) findPreference(
                KEY_TIME_SINCE_LAST_FULL_CHARGE);
        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.battery_footer_summary);
        mAnomalySummaryPreferenceController = new AnomalySummaryPreferenceController(
                (SettingsActivity) getActivity(), this, MetricsEvent.FUELGAUGE_POWER_USAGE_SUMMARY);
        mBatteryUtils = BatteryUtils.getInstance(getContext());
        mAnomalySparseArray = new SparseArray<>();

        restartBatteryInfoLoader();
        restoreSavedInstance(icicle);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FUELGAUGE_POWER_USAGE_SUMMARY;
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SHOW_ALL_APPS, mShowAllApps);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mAnomalySummaryPreferenceController.onPreferenceTreeClick(preference)) {
            return true;
        }
        if (KEY_BATTERY_HEADER.equals(preference.getKey())) {
            performBatteryHeaderClick();
            return true;
        } else if (!(preference instanceof PowerGaugePreference)) {
            return super.onPreferenceTreeClick(preference);
        }
        PowerGaugePreference pgp = (PowerGaugePreference) preference;
        BatteryEntry entry = pgp.getInfo();
        AdvancedPowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(),
                this, mStatsHelper, mStatsType, entry, pgp.getPercent(),
                mAnomalySparseArray.get(entry.sipper.getUid()));
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
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        mBatteryHeaderPreferenceController = new BatteryHeaderPreferenceController(
                context, getActivity(), this /* host */, getLifecycle());
        controllers.add(mBatteryHeaderPreferenceController);
        controllers.add(new AutoBrightnessPreferenceController(context, KEY_AUTO_BRIGHTNESS));
        controllers.add(new TimeoutPreferenceController(context, KEY_SCREEN_TIMEOUT));
        controllers.add(new BatterySaverController(context, getLifecycle()));
        controllers.add(new BatteryPercentagePreferenceController(context));
        controllers.add(new AmbientDisplayPreferenceController(
                context,
                new AmbientDisplayConfiguration(context),
                KEY_AMBIENT_DISPLAY));
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
        final Context context = getContext();
        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFactory(context).getMetricsFeatureProvider();

        switch (item.getItemId()) {
            case MENU_STATS_TYPE:
                if (mStatsType == BatteryStats.STATS_SINCE_CHARGED) {
                    mStatsType = BatteryStats.STATS_SINCE_UNPLUGGED;
                } else {
                    mStatsType = BatteryStats.STATS_SINCE_CHARGED;
                }
                refreshUi();
                return true;
            case MENU_HIGH_POWER_APPS:
                Bundle args = new Bundle();
                args.putString(ManageApplications.EXTRA_CLASSNAME,
                        HighPowerApplicationsActivity.class.getName());
                sa.startPreferencePanel(this, ManageApplications.class.getName(), args,
                        R.string.high_power_apps, null, null, 0);
                metricsFeatureProvider.action(context,
                        MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_OPTIMIZATION);
                return true;
            case MENU_ADDITIONAL_BATTERY_INFO:
                startActivity(mPowerFeatureProvider
                        .getAdditionalBatteryInfoIntent());
                metricsFeatureProvider.action(context,
                        MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_USAGE_ALERTS);
                return true;
            case MENU_TOGGLE_APPS:
                mShowAllApps = !mShowAllApps;
                item.setTitle(mShowAllApps ? R.string.hide_extra_apps : R.string.show_all_apps);
                metricsFeatureProvider.action(context,
                        MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_APPS_TOGGLE, mShowAllApps);
                restartBatteryStatsLoader(false /* clearHeader */);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @VisibleForTesting
    void restoreSavedInstance(Bundle savedInstance) {
        if (savedInstance != null) {
            mShowAllApps = savedInstance.getBoolean(KEY_SHOW_ALL_APPS, false);
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
        if (mPowerFeatureProvider.isAdvancedUiEnabled()) {
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
    private List<BatterySipper> getCoalescedUsageList(final List<BatterySipper> sippers) {
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
        mBatteryUtils.sortUsageList(results);
        return results;
    }

    protected void refreshUi() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        restartAnomalyDetectionIfPossible();

        // reload BatteryInfo and updateUI
        restartBatteryInfoLoader();
        final long lastFullChargeTime = mBatteryUtils.calculateLastFullChargeTime(mStatsHelper,
                System.currentTimeMillis());
        updateScreenPreference();
        updateLastFullChargePreference(lastFullChargeTime);

        final CharSequence timeSequence = Utils.formatElapsedTime(context, lastFullChargeTime,
                false);
        final int resId = mShowAllApps ? R.string.power_usage_list_summary_device
                : R.string.power_usage_list_summary;
        mAppListGroup.setTitle(TextUtils.expandTemplate(getText(resId), timeSequence));

        refreshAppListGroup();
    }

    private void refreshAppListGroup() {
        final Context context = getContext();
        final PowerProfile powerProfile = mStatsHelper.getPowerProfile();
        final BatteryStats stats = mStatsHelper.getStats();
        final double averagePower = powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
        boolean addedSome = false;

        TypedArray array = context.obtainStyledAttributes(
                new int[]{android.R.attr.colorControlNormal});
        final int colorControl = array.getColor(0, 0);
        array.recycle();

        final int dischargeAmount = USE_FAKE_DATA ? 5000
                : stats != null ? stats.getDischargeAmount(mStatsType) : 0;

        cacheRemoveAllPrefs(mAppListGroup);
        mAppListGroup.setOrderingAsAdded(false);

        if (averagePower >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP || USE_FAKE_DATA) {
            final List<BatterySipper> usageList = getCoalescedUsageList(
                    USE_FAKE_DATA ? getFakeStats() : mStatsHelper.getUsageList());
            double hiddenPowerMah = mShowAllApps ? 0 :
                    mBatteryUtils.removeHiddenBatterySippers(usageList);
            mBatteryUtils.sortUsageList(usageList);

            final int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                final BatterySipper sipper = usageList.get(i);
                double totalPower = USE_FAKE_DATA ? 4000 : mStatsHelper.getTotalPower();

                final double percentOfTotal = mBatteryUtils.calculateBatteryPercent(
                        sipper.totalPowerMah, totalPower, hiddenPowerMah, dischargeAmount);

                if (((int) (percentOfTotal + .5)) < 1) {
                    continue;
                }
                if (shouldHideSipper(sipper)) {
                    continue;
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
                pref.shouldShowAnomalyIcon(false);
                if (sipper.usageTimeMs == 0 && sipper.drainType == DrainType.APP) {
                    sipper.usageTimeMs = mBatteryUtils.getProcessTimeMs(
                            BatteryUtils.StatusType.FOREGROUND, sipper.uidObj, mStatsType);
                }
                setUsageSummary(pref, sipper);
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
    boolean shouldHideSipper(BatterySipper sipper) {
        // Don't show over-counted and unaccounted in any condition
        return sipper.drainType == BatterySipper.DrainType.OVERCOUNTED
                || sipper.drainType == BatterySipper.DrainType.UNACCOUNTED;
    }

    @VisibleForTesting
    void refreshAnomalyIcon() {
        for (int i = 0, size = mAnomalySparseArray.size(); i < size; i++) {
            final String key = extractKeyFromUid(mAnomalySparseArray.keyAt(i));
            final PowerGaugePreference pref = (PowerGaugePreference) mAppListGroup.findPreference(
                    key);
            if (pref != null) {
                pref.shouldShowAnomalyIcon(true);
            }
        }
    }

    @VisibleForTesting
    void restartAnomalyDetectionIfPossible() {
        if (getAnomalyDetectionPolicy().isAnomalyDetectionEnabled()) {
            getLoaderManager().restartLoader(ANOMALY_LOADER, Bundle.EMPTY, mAnomalyLoaderCallbacks);
        }
    }

    @VisibleForTesting
    AnomalyDetectionPolicy getAnomalyDetectionPolicy() {
        return new AnomalyDetectionPolicy(getContext());
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
    void updateScreenPreference() {
        final BatterySipper sipper = findBatterySipperByType(
                mStatsHelper.getUsageList(), DrainType.SCREEN);
        final long usageTimeMs = sipper != null ? sipper.usageTimeMs : 0;

        mScreenUsagePref.setSubtitle(Utils.formatElapsedTime(getContext(), usageTimeMs, false));
    }

    @VisibleForTesting
    void updateLastFullChargePreference(long timeMs) {
        final CharSequence timeSequence = Utils.formatElapsedTime(getContext(), timeMs, false);
        mLastFullChargePref.setSubtitle(
                TextUtils.expandTemplate(getText(R.string.power_last_full_charge_summary),
                        timeSequence));
    }

    @VisibleForTesting
    void showBothEstimates() {
        final Context context = getContext();
        if (context == null
                || !mPowerFeatureProvider.isEnhancedBatteryPredictionEnabled(context)) {
            return;
        }
        getLoaderManager().restartLoader(DEBUG_INFO_LOADER, Bundle.EMPTY,
                mBatteryInfoDebugLoaderCallbacks);
    }

    @VisibleForTesting
    double calculatePercentage(double powerUsage, double dischargeAmount) {
        final double totalPower = mStatsHelper.getTotalPower();
        return totalPower == 0 ? 0 :
                ((powerUsage / totalPower) * dischargeAmount);
    }

    @VisibleForTesting
    void setUsageSummary(Preference preference, BatterySipper sipper) {
        // Only show summary when usage time is longer than one minute
        final long usageTimeMs = sipper.usageTimeMs;
        if (usageTimeMs >= DateUtils.MINUTE_IN_MILLIS) {
            final CharSequence timeSequence = Utils.formatElapsedTime(getContext(), usageTimeMs,
                    false);
            preference.setSummary(
                    (sipper.drainType != DrainType.APP || mBatteryUtils.shouldHideSipper(sipper))
                            ? timeSequence
                            : TextUtils.expandTemplate(getText(R.string.battery_used_for),
                                    timeSequence));
        }
    }

    @VisibleForTesting
    String extractKeyFromSipper(BatterySipper sipper) {
        if (sipper.uidObj != null) {
            return extractKeyFromUid(sipper.getUid());
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
    String extractKeyFromUid(int uid) {
        return Integer.toString(uid);
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

    @VisibleForTesting
    void updateAnomalySparseArray(List<Anomaly> anomalies) {
        mAnomalySparseArray.clear();
        for (int i = 0, size = anomalies.size(); i < size; i++) {
            final Anomaly anomaly = anomalies.get(i);
            if (mAnomalySparseArray.get(anomaly.uid) == null) {
                mAnomalySparseArray.append(anomaly.uid, new ArrayList<>());
            }
            mAnomalySparseArray.get(anomaly.uid).add(anomaly);
        }
    }

    @VisibleForTesting
    void restartBatteryInfoLoader() {
        getLoaderManager().restartLoader(BATTERY_INFO_LOADER, Bundle.EMPTY,
                mBatteryInfoLoaderCallbacks);
        if (mPowerFeatureProvider.isEstimateDebugEnabled()) {
            // Unfortunately setting a long click listener on a view means it will no
            // longer pass the regular click event to the parent, so we have to register
            // a regular click listener as well.
            View header = mBatteryLayoutPref.findViewById(R.id.summary1);
            header.setOnLongClickListener(this);
            header.setOnClickListener(this);
        }
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

    @Override
    public void onAnomalyHandled(Anomaly anomaly) {
        mAnomalySummaryPreferenceController.hideHighUsagePreference();
    }

    @Override
    public boolean onLongClick(View view) {
        showBothEstimates();
        view.setOnLongClickListener(null);
        return true;
    }

    @Override
    public void onClick(View view) {
        performBatteryHeaderClick();
    }

    @Override
    protected void restartBatteryStatsLoader() {
        restartBatteryStatsLoader(true /* clearHeader */);
    }

    void restartBatteryStatsLoader(boolean clearHeader) {
        super.restartBatteryStatsLoader();
        if (clearHeader) {
            mBatteryHeaderPreferenceController.quickUpdateHeaderPreference();
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;
        private final BatteryBroadcastReceiver mBatteryBroadcastReceiver;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
            mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(mContext);
            mBatteryBroadcastReceiver.setBatteryChangedListener(() -> {
                BatteryInfo.getBatteryInfo(mContext, new BatteryInfo.Callback() {
                    @Override
                    public void onBatteryInfoLoaded(BatteryInfo info) {
                        mLoader.setSummary(SummaryProvider.this, info.chargeLabel);
                    }
                }, true /* shortString */);
            });
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                mBatteryBroadcastReceiver.register();
            } else {
                mBatteryBroadcastReceiver.unRegister();
            }
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.power_usage_summary;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> niks = super.getNonIndexableKeys(context);
                    niks.add(KEY_HIGH_USAGE);
                    niks.add(KEY_BATTERY_SAVER_SUMMARY);
                    // Duplicates in display
                    niks.add(KEY_AUTO_BRIGHTNESS);
                    niks.add(KEY_SCREEN_TIMEOUT);
                    niks.add(KEY_AMBIENT_DISPLAY);
                    return niks;
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
