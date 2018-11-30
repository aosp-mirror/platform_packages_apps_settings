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

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.os.BatteryStats;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.display.BatteryPercentagePreferenceController;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDetectionPolicy;
import com.android.settings.fuelgauge.batterytip.BatteryTipLoader;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was
 * consumed since the last time it was unplugged.
 */
public class PowerUsageSummary extends PowerUsageBase implements OnLongClickListener,
        BatteryTipPreferenceController.BatteryTipListener {

    static final String TAG = "PowerUsageSummary";

    private static final boolean DEBUG = false;
    private static final String KEY_BATTERY_HEADER = "battery_header";
    private static final String KEY_BATTERY_TIP = "battery_tip";

    private static final String KEY_SCREEN_USAGE = "screen_usage";
    private static final String KEY_TIME_SINCE_LAST_FULL_CHARGE = "last_full_charge";
    private static final String KEY_BATTERY_SAVER_SUMMARY = "battery_saver_summary";

    @VisibleForTesting
    static final int BATTERY_INFO_LOADER = 1;
    @VisibleForTesting
    static final int BATTERY_TIP_LOADER = 2;
    @VisibleForTesting
    static final int MENU_STATS_TYPE = Menu.FIRST;
    @VisibleForTesting
    static final int MENU_ADVANCED_BATTERY = Menu.FIRST + 1;
    public static final int DEBUG_INFO_LOADER = 3;

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
    @VisibleForTesting
    BatteryInfo mBatteryInfo;

    /**
     * SparseArray that maps uid to {@link Anomaly}, so we could find {@link Anomaly} by uid
     */
    @VisibleForTesting
    SparseArray<List<Anomaly>> mAnomalySparseArray;
    @VisibleForTesting
    BatteryHeaderPreferenceController mBatteryHeaderPreferenceController;
    @VisibleForTesting
    boolean mNeedUpdateBatteryTip;
    @VisibleForTesting
    BatteryTipPreferenceController mBatteryTipPreferenceController;
    private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;

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
                    mBatteryInfo = batteryInfo;
                    updateLastFullChargePreference();
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
                    updateViews(batteryInfos);
                }

                @Override
                public void onLoaderReset(Loader<List<BatteryInfo>> loader) {
                }
            };

    protected void updateViews(List<BatteryInfo> batteryInfos) {
        final BatteryMeterView batteryView = mBatteryLayoutPref
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
                PowerUtil.convertUsToMs(oldInfo.remainingTimeUs))));

        // for this one we can just set the string directly
        summary2.setText(mPowerFeatureProvider.getEnhancedEstimateDebugString(
            Formatter.formatShortElapsedTime(getContext(),
                PowerUtil.convertUsToMs(newInfo.remainingTimeUs))));

        batteryView.setBatteryLevel(oldInfo.batteryLevel);
        batteryView.setCharging(!oldInfo.discharging);
    }

    private LoaderManager.LoaderCallbacks<List<BatteryTip>> mBatteryTipsCallbacks =
            new LoaderManager.LoaderCallbacks<List<BatteryTip>>() {

                @Override
                public Loader<List<BatteryTip>> onCreateLoader(int id, Bundle args) {
                    return new BatteryTipLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<List<BatteryTip>> loader,
                        List<BatteryTip> data) {
                    mBatteryTipPreferenceController.updateBatteryTips(data);
                }

                @Override
                public void onLoaderReset(Loader<List<BatteryTip>> loader) {

                }
            };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);

        initFeatureProvider();
        mBatteryLayoutPref = (LayoutPreference) findPreference(KEY_BATTERY_HEADER);

        mScreenUsagePref = (PowerGaugePreference) findPreference(KEY_SCREEN_USAGE);
        mLastFullChargePref = (PowerGaugePreference) findPreference(
                KEY_TIME_SINCE_LAST_FULL_CHARGE);
        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.battery_footer_summary);
        mBatteryUtils = BatteryUtils.getInstance(getContext());
        mAnomalySparseArray = new SparseArray<>();

        restartBatteryInfoLoader();
        mBatteryTipPreferenceController.restoreInstanceState(icicle);
        updateBatteryTipFlag(icicle);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FUELGAUGE_POWER_USAGE_SUMMARY_V2;
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final Lifecycle lifecycle = getLifecycle();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        mBatteryHeaderPreferenceController = new BatteryHeaderPreferenceController(
                context, activity, this /* host */, lifecycle);
        controllers.add(mBatteryHeaderPreferenceController);
        mBatteryTipPreferenceController = new BatteryTipPreferenceController(context,
                KEY_BATTERY_TIP, (SettingsActivity) getActivity(), this /* fragment */, this /*
                BatteryTipListener */);
        controllers.add(mBatteryTipPreferenceController);
        controllers.add(new BatteryPercentagePreferenceController(context));
        return controllers;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) {
            menu.add(Menu.NONE, MENU_STATS_TYPE, Menu.NONE, R.string.menu_stats_total)
                    .setIcon(com.android.internal.R.drawable.ic_menu_info_details)
                    .setAlphabeticShortcut('t');
        }

        menu.add(Menu.NONE, MENU_ADVANCED_BATTERY, Menu.NONE, R.string.advanced_battery_title);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_battery;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_STATS_TYPE:
                if (mStatsType == BatteryStats.STATS_SINCE_CHARGED) {
                    mStatsType = BatteryStats.STATS_SINCE_UNPLUGGED;
                } else {
                    mStatsType = BatteryStats.STATS_SINCE_CHARGED;
                }
                refreshUi(BatteryUpdateType.MANUAL);
                return true;
            case MENU_ADVANCED_BATTERY:
                new SubSettingLauncher(getContext())
                        .setDestination(PowerUsageAdvanced.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setTitle(R.string.advanced_battery_title)
                        .launch();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void refreshUi(@BatteryUpdateType int refreshType) {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        // Skip BatteryTipLoader if device is rotated or only battery level change
        if (mNeedUpdateBatteryTip
                && refreshType != BatteryUpdateType.BATTERY_LEVEL) {
            restartBatteryTipLoader();
        } else {
            mNeedUpdateBatteryTip = true;
        }

        // reload BatteryInfo and updateUI
        restartBatteryInfoLoader();
        updateLastFullChargePreference();
        mScreenUsagePref.setSubtitle(StringUtil.formatElapsedTime(getContext(),
                mBatteryUtils.calculateScreenUsageTime(mStatsHelper), false));
    }

    @VisibleForTesting
    void restartBatteryTipLoader() {
        getLoaderManager().restartLoader(BATTERY_TIP_LOADER, Bundle.EMPTY, mBatteryTipsCallbacks);
    }

    @VisibleForTesting
    void setBatteryLayoutPreference(LayoutPreference layoutPreference) {
        mBatteryLayoutPref = layoutPreference;
    }

    @VisibleForTesting
    AnomalyDetectionPolicy getAnomalyDetectionPolicy() {
        return new AnomalyDetectionPolicy(getContext());
    }

    @VisibleForTesting
    void updateLastFullChargePreference() {
        if (mBatteryInfo != null && mBatteryInfo.averageTimeToDischarge
                != Estimate.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN) {
            mLastFullChargePref.setTitle(R.string.battery_full_charge_last);
            mLastFullChargePref.setSubtitle(
                    StringUtil.formatElapsedTime(getContext(), mBatteryInfo.averageTimeToDischarge,
                            false /* withSeconds */));
        } else {
            final long lastFullChargeTime = mBatteryUtils.calculateLastFullChargeTime(mStatsHelper,
                    System.currentTimeMillis());
            mLastFullChargePref.setTitle(R.string.battery_last_full_charge);
            mLastFullChargePref.setSubtitle(
                    StringUtil.formatRelativeTime(getContext(), lastFullChargeTime,
                            false /* withSeconds */));
        }
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
    void initFeatureProvider() {
        final Context context = getContext();
        mPowerFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
    }

    @VisibleForTesting
    void updateAnomalySparseArray(List<Anomaly> anomalies) {
        mAnomalySparseArray.clear();
        for (final Anomaly anomaly : anomalies) {
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
            // Set long click action for summary to show debug info
            View header = mBatteryLayoutPref.findViewById(R.id.summary1);
            header.setOnLongClickListener(this);
        }
    }

    @VisibleForTesting
    void updateBatteryTipFlag(Bundle icicle) {
        mNeedUpdateBatteryTip = icicle == null || mBatteryTipPreferenceController.needUpdate();
    }

    @Override
    public boolean onLongClick(View view) {
        showBothEstimates();
        view.setOnLongClickListener(null);
        return true;
    }

    @Override
    protected void restartBatteryStatsLoader(@BatteryUpdateType int refreshType) {
        super.restartBatteryStatsLoader(refreshType);
        mBatteryHeaderPreferenceController.quickUpdateHeaderPreference();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mBatteryTipPreferenceController.saveInstanceState(outState);
    }

    @Override
    public void onBatteryTipHandled(BatteryTip batteryTip) {
        restartBatteryTipLoader();
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;
        private final BatteryBroadcastReceiver mBatteryBroadcastReceiver;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
            mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(mContext);
            mBatteryBroadcastReceiver.setBatteryChangedListener(type -> {
                BatteryInfo.getBatteryInfo(mContext, new BatteryInfo.Callback() {
                    @Override
                    public void onBatteryInfoLoaded(BatteryInfo info) {
                        mLoader.setSummary(SummaryProvider.this, getDashboardLabel(mContext, info));
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

    @VisibleForTesting
    static CharSequence getDashboardLabel(Context context, BatteryInfo info) {
        CharSequence label;
        final BidiFormatter formatter = BidiFormatter.getInstance();
        if (info.remainingLabel == null) {
            label = info.batteryPercentString;
        } else {
            label = context.getString(R.string.power_remaining_settings_home_page,
                    formatter.unicodeWrap(info.batteryPercentString),
                    formatter.unicodeWrap(info.remainingLabel));
        }
        return label;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.power_usage_summary;
                    return Collections.singletonList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> niks = super.getNonIndexableKeys(context);

                    final BatteryPercentagePreferenceController controller =
                            new BatteryPercentagePreferenceController(context);
                    if (!controller.isAvailable()) {
                        niks.add(controller.getPreferenceKey());
                    }
                    niks.add(KEY_BATTERY_SAVER_SUMMARY);
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
