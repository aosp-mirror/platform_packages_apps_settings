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
import android.os.BatteryStats;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.format.Formatter;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.applications.manageapplications.ManageApplications;
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
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was
 * consumed since the last time it was unplugged.
 */
public class PowerUsageSummary extends PowerUsageBase implements OnLongClickListener,
        BatteryTipPreferenceController.BatteryTipListener {

    static final String TAG = "PowerUsageSummary";

    private static final boolean DEBUG = false;
    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_BATTERY_HEADER = "battery_header";
    private static final String KEY_BATTERY_TIP = "battery_tip";

    private static final String KEY_SCREEN_USAGE = "screen_usage";
    private static final String KEY_TIME_SINCE_LAST_FULL_CHARGE = "last_full_charge";
    private static final String KEY_BATTERY_SAVER_SUMMARY = "battery_saver_summary";

    @VisibleForTesting
    static final int BATTERY_INFO_LOADER = 1;
    @VisibleForTesting
    static final int BATTERY_TIP_LOADER = 2;
    private static final int MENU_STATS_TYPE = Menu.FIRST;
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

    /**
     * SparseArray that maps uid to {@link Anomaly}, so we could find {@link Anomaly} by uid
     */
    @VisibleForTesting
    SparseArray<List<Anomaly>> mAnomalySparseArray;
    @VisibleForTesting
    PreferenceGroup mAppListGroup;
    @VisibleForTesting
    BatteryHeaderPreferenceController mBatteryHeaderPreferenceController;
    private BatteryAppListPreferenceController mBatteryAppListPreferenceController;
    private BatteryTipPreferenceController mBatteryTipPreferenceController;
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
                                    PowerUtil.convertUsToMs(oldInfo.remainingTimeUs))));

                    // for this one we can just set the string directly
                    summary2.setText(mPowerFeatureProvider.getEnhancedEstimateDebugString(
                            Formatter.formatShortElapsedTime(getContext(),
                                    PowerUtil.convertUsToMs(newInfo.remainingTimeUs))));

                    batteryView.setBatteryLevel(oldInfo.batteryLevel);
                    batteryView.setCharging(!oldInfo.discharging);
                }

                @Override
                public void onLoaderReset(Loader<List<BatteryInfo>> loader) {
                }
            };

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
        mBatteryAppListPreferenceController = new BatteryAppListPreferenceController(context,
                KEY_APP_LIST, lifecycle, activity, this);
        controllers.add(mBatteryAppListPreferenceController);
        mBatteryTipPreferenceController = new BatteryTipPreferenceController(context,
                KEY_BATTERY_TIP, (SettingsActivity) getActivity(), this /* fragment */, this /*
                BatteryTipListener */);
        controllers.add(mBatteryTipPreferenceController);
        BatterySaverController batterySaverController = new BatterySaverController(context);
        controllers.add(batterySaverController);
        controllers.add(new BatteryPercentagePreferenceController(context));

        lifecycle.addObserver(batterySaverController);
        return controllers;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) {
            menu.add(Menu.NONE, MENU_STATS_TYPE, Menu.NONE, R.string.menu_stats_total)
                    .setIcon(com.android.internal.R.drawable.ic_menu_info_details)
                    .setAlphabeticShortcut('t');
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public int getHelpResource() {
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void refreshUi() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        restartBatteryTipLoader();

        // reload BatteryInfo and updateUI
        restartBatteryInfoLoader();
        final long lastFullChargeTime = mBatteryUtils.calculateLastFullChargeTime(mStatsHelper,
                System.currentTimeMillis());
        updateLastFullChargePreference(lastFullChargeTime);
        mScreenUsagePref.setSubtitle(StringUtil.formatElapsedTime(getContext(),
                mBatteryUtils.calculateScreenUsageTime(mStatsHelper), false));

        final CharSequence timeSequence = StringUtil.formatRelativeTime(context, lastFullChargeTime,
                false);
        mBatteryAppListPreferenceController.refreshAppListGroup(mStatsHelper,
                false /* showAllApps */, timeSequence);
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
    void updateLastFullChargePreference(long timeMs) {
        final CharSequence timeSequence = StringUtil.formatRelativeTime(getContext(), timeMs,
                false);
        mLastFullChargePref.setSubtitle(timeSequence);
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
            // Set long click action for summary to show debug info
            View header = mBatteryLayoutPref.findViewById(R.id.summary1);
            header.setOnLongClickListener(this);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        showBothEstimates();
        view.setOnLongClickListener(null);
        return true;
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
