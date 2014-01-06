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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.os.PowerProfile;
import com.android.settings.HelpUtils;
import com.android.settings.R;

import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was
 * consumed since the last time it was unplugged.
 */
public class PowerUsageSummary extends PreferenceFragment {

    private static final boolean DEBUG = false;

    private static final String TAG = "PowerUsageSummary";

    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_BATTERY_STATUS = "battery_status";

    private static final int MENU_STATS_TYPE = Menu.FIRST;
    private static final int MENU_STATS_REFRESH = Menu.FIRST + 1;
    private static final int MENU_STATS_RESET = Menu.FIRST + 2;
    private static final int MENU_HELP = Menu.FIRST + 3;

    private PreferenceGroup mAppListGroup;
    private Preference mBatteryStatusPref;

    private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;

    private static final int MIN_POWER_THRESHOLD = 5;
    private static final int MAX_ITEMS_TO_LIST = 10;

    private BatteryManager mBatteryService;
    private BatteryStatsHelper mStatsHelper;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                String batteryLevel = com.android.settings.Utils.getBatteryPercentage(intent);
                String batteryStatus = com.android.settings.Utils.getBatteryStatus(getResources(),
                        intent);
                String batterySummary = context.getResources().getString(
                        R.string.power_usage_level_and_status, batteryLevel, batteryStatus);
                mBatteryStatusPref.setTitle(batterySummary);
                mStatsHelper.clearStats();
                refreshStats();
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mStatsHelper = new BatteryStatsHelper(activity, mHandler);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mStatsHelper.create(icicle);
        mBatteryService = (BatteryManager) getActivity().getSystemService(Context.BATTERY_SERVICE);

        addPreferencesFromResource(R.xml.power_usage_summary);
        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        mBatteryStatusPref = mAppListGroup.findPreference(KEY_BATTERY_STATUS);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mBatteryInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        refreshStats();
    }

    @Override
    public void onPause() {
        mStatsHelper.pause();
        mHandler.removeMessages(BatteryStatsHelper.MSG_UPDATE_NAME_ICON);
        getActivity().unregisterReceiver(mBatteryInfoReceiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mStatsHelper.destroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof BatteryHistoryPreference) {
            Parcel hist = Parcel.obtain();
            mStatsHelper.getStats().writeToParcelWithoutUids(hist, 0);
            byte[] histData = hist.marshall();
            Bundle args = new Bundle();
            args.putByteArray(BatteryHistoryDetail.EXTRA_STATS, histData);
            if (mBatteryService.isDockBatterySupported()) {
                Parcel dockHist = Parcel.obtain();
                mStatsHelper.getDockStats(getActivity()).writeToParcelWithoutUids(dockHist, 0);
                byte[] dockHistData = dockHist.marshall();
                args.putByteArray(BatteryHistoryDetail.EXTRA_DOCK_STATS, dockHistData);
            }

            PreferenceActivity pa = (PreferenceActivity)getActivity();
            pa.startPreferencePanel(BatteryHistoryDetail.class.getName(), args,
                    R.string.history_details_title, null, null, 0);
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        if (!(preference instanceof PowerGaugePreference)) {
            return false;
        }
        PowerGaugePreference pgp = (PowerGaugePreference) preference;
        BatterySipper sipper = pgp.getInfo();
        mStatsHelper.startBatteryDetailPage((PreferenceActivity) getActivity(), sipper, true);
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) {
            menu.add(0, MENU_STATS_TYPE, 0, R.string.menu_stats_total)
                    .setIcon(com.android.internal.R.drawable.ic_menu_info_details)
                    .setAlphabeticShortcut('t');
        }
        MenuItem refresh = menu.add(0, MENU_STATS_REFRESH, 0, R.string.menu_stats_refresh)
                .setIcon(R.drawable.ic_menu_refresh_holo_dark)
                .setAlphabeticShortcut('r');
        refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        MenuItem reset = menu.add(0, MENU_STATS_RESET, 0, R.string.menu_stats_reset)
                .setIcon(R.drawable.ic_menu_delete_holo_dark)
                .setAlphabeticShortcut('d');
        reset.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        String helpUrl;
        if (!TextUtils.isEmpty(helpUrl = getResources().getString(R.string.help_url_battery))) {
            final MenuItem help = menu.add(0, MENU_HELP, 0, R.string.help_label);
            HelpUtils.prepareHelpMenuItem(getActivity(), help, helpUrl);
        }
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
                refreshStats();
                return true;
            case MENU_STATS_REFRESH:
                mStatsHelper.clearStats();
                refreshStats();
                return true;
            case MENU_STATS_RESET:
                mStatsHelper.resetStatistics();
                mStatsHelper.clearStats();
                refreshStats();
                return true;
            default:
                return false;
        }
    }

    private void addNotAvailableMessage() {
        Preference notAvailable = new Preference(getActivity());
        notAvailable.setTitle(R.string.power_usage_not_available);
        mAppListGroup.addPreference(notAvailable);
    }

    private void refreshStats() {
        mAppListGroup.removeAll();
        mAppListGroup.setOrderingAsAdded(false);

        mBatteryStatusPref.setOrder(-2);
        mAppListGroup.addPreference(mBatteryStatusPref);
        BatteryHistoryPreference hist = new BatteryHistoryPreference(
                getActivity(), mStatsHelper.getStats(), mStatsHelper.getDockStats(getActivity()));
        hist.setOrder(-1);
        mAppListGroup.addPreference(hist);

        if (mStatsHelper.getPowerProfile().getAveragePower(
                PowerProfile.POWER_SCREEN_FULL) < 10) {
            addNotAvailableMessage();
            return;
        }
        mStatsHelper.refreshStats(getActivity(), false);
        List<BatterySipper> usageList = mStatsHelper.getUsageList();
        for (BatterySipper sipper : usageList) {
            if (sipper.getSortValue() < MIN_POWER_THRESHOLD) continue;
            final double percentOfTotal =
                    ((sipper.getSortValue() / mStatsHelper.getTotalPower()) * 100);
            if (percentOfTotal < 1) continue;
            PowerGaugePreference pref =
                    new PowerGaugePreference(getActivity(), sipper.getIcon(), sipper);
            final double percentOfMax =
                    (sipper.getSortValue() * 100) / mStatsHelper.getMaxPower();
            sipper.percent = percentOfTotal;
            pref.setTitle(sipper.name);
            pref.setOrder(Integer.MAX_VALUE - (int) sipper.getSortValue()); // Invert the order
            pref.setPercent(percentOfMax, percentOfTotal);
            if (sipper.uidObj != null) {
                pref.setKey(Integer.toString(sipper.uidObj.getUid()));
            }
            mAppListGroup.addPreference(pref);
            if (mAppListGroup.getPreferenceCount() > (MAX_ITEMS_TO_LIST+1)) break;
        }
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryStatsHelper.MSG_UPDATE_NAME_ICON:
                    BatterySipper bs = (BatterySipper) msg.obj;
                    PowerGaugePreference pgp =
                            (PowerGaugePreference) findPreference(
                                    Integer.toString(bs.uidObj.getUid()));
                    if (pgp != null) {
                        pgp.setIcon(bs.icon);
                        pgp.setTitle(bs.name);
                    }
                    break;
                case BatteryStatsHelper.MSG_REPORT_FULLY_DRAWN:
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };
}
