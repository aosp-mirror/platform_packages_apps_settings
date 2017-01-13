/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Common base class for things that need to show the battery usage graph.
 */
public abstract class PowerUsageBase extends SettingsPreferenceFragment {

    // +1 to allow ordering for PowerUsageSummary.
    @VisibleForTesting
    static final int MENU_STATS_REFRESH = Menu.FIRST + 1;

    protected BatteryStatsHelper mStatsHelper;
    protected UserManager mUm;

    private String mBatteryLevel;
    private String mBatteryStatus;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mUm = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        mStatsHelper = new BatteryStatsHelper(activity, true);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mStatsHelper.create(icicle);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        mStatsHelper.clearStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        BatteryStatsHelper.dropFile(getActivity(), BatteryHistoryPreference.BATTERY_HISTORY_FILE);
        updateBatteryStatus(getActivity().registerReceiver(mBatteryInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
        if (mHandler.hasMessages(MSG_REFRESH_STATS)) {
            mHandler.removeMessages(MSG_REFRESH_STATS);
            mStatsHelper.clearStats();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mBatteryInfoReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeMessages(MSG_REFRESH_STATS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            mStatsHelper.storeState();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem refresh = menu.add(0, MENU_STATS_REFRESH, 0, R.string.menu_stats_refresh)
                .setIcon(com.android.internal.R.drawable.ic_menu_refresh)
                .setAlphabeticShortcut('r');
        refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_STATS_REFRESH:
                mStatsHelper.clearStats();
                refreshStats();
                mHandler.removeMessages(MSG_REFRESH_STATS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void refreshStats() {
        mStatsHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED, mUm.getUserProfiles());
    }

    protected void updatePreference(BatteryHistoryPreference historyPref) {
        historyPref.setStats(mStatsHelper);
    }

    private boolean updateBatteryStatus(Intent intent) {
        if (intent != null) {
            String batteryLevel = com.android.settings.Utils.getBatteryPercentage(intent);
            String batteryStatus = com.android.settings.Utils.getBatteryStatus(getResources(),
                    intent);
            if (!batteryLevel.equals(mBatteryLevel) || !batteryStatus.equals(mBatteryStatus)) {
                mBatteryLevel = batteryLevel;
                mBatteryStatus = batteryStatus;
                return true;
            }
        }
        return false;
    }

    static final int MSG_REFRESH_STATS = 100;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_STATS:
                    mStatsHelper.clearStats();
                    refreshStats();
                    break;
            }
        }
    };

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)
                    && updateBatteryStatus(intent)) {
                if (!mHandler.hasMessages(MSG_REFRESH_STATS)) {
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_STATS, 500);
                }
            }
        }
    };

}
