/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.core.lifecycle.events.OnStart;
import com.android.settings.core.lifecycle.events.OnStop;
import com.android.settings.dashboard.conditional.BatterySaverCondition;
import com.android.settings.dashboard.conditional.ConditionManager;
import com.android.settings.widget.MasterSwitchPreference;

import static android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGING;

public class BatterySaverController extends PreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, OnStart, OnStop {
    private static final String KEY_BATTERY_SAVER = "battery_saver_summary";
    private static final String TAG = "BatterySaverController";
    private static final boolean DEBUG = false;

    private final BatteryStateChangeReceiver mBatteryStateChangeReceiver;
    private final PowerManager mPowerManager;
    private MasterSwitchPreference mBatterySaverPref;

    public BatterySaverController(Context context, Lifecycle lifecycle) {
        super(context);

        lifecycle.addObserver(this);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mBatteryStateChangeReceiver = new BatteryStateChangeReceiver();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_SAVER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatterySaverPref = (MasterSwitchPreference) screen.findPreference(KEY_BATTERY_SAVER);
    }

    @Override
    public void updateState(Preference preference) {
        mBatterySaverPref.setChecked(mPowerManager.isPowerSaveMode());
        updateSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean saverOn = (Boolean) newValue;
        if (saverOn != mPowerManager.isPowerSaveMode()
                && !mPowerManager.setPowerSaveMode(saverOn)) {
            // Do nothing if power save mode doesn't set correctly
            return false;
        }

        refreshConditionManager();
        updateSummary();
        return true;
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL)
                , true, mObserver);

        mBatteryStateChangeReceiver.setListening(true);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
        mBatteryStateChangeReceiver.setListening(false);
    }

    @VisibleForTesting
    void refreshConditionManager() {
        ConditionManager.get(mContext).getCondition(BatterySaverCondition.class).refreshState();
    }

    private void updateSummary() {
        final boolean mode = mPowerManager.isPowerSaveMode();
        final int format = mode ? R.string.battery_saver_on_summary
                : R.string.battery_saver_off_summary;
        final int percent = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        final int percentFormat = percent > 0 ? R.string.battery_saver_desc_turn_on_auto_pct
                : R.string.battery_saver_desc_turn_on_auto_never;

        final String summary = mContext.getString(format, mContext.getString(percentFormat,
                Utils.formatPercentage(percent)));

        mBatterySaverPref.setSummary(summary);
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateSummary();
        }
    };

    private final class BatteryStateChangeReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) {
                Log.d(TAG, "Received " + intent.getAction());
            }
            final String action = intent.getAction();
            if (action.equals(ACTION_POWER_SAVE_MODE_CHANGING)) {
                mBatterySaverPref.setChecked(mPowerManager.isPowerSaveMode());
                updateSummary();
            } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                // disable BSM switch if phone is plugged in or at 100% while plugged in
                final boolean enabled = !(status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL);

                mBatterySaverPref.setSwitchEnabled(enabled);
            }
        }

        public void setListening(boolean listening) {
            if (listening && !mRegistered) {
                final IntentFilter ifilter = new IntentFilter();
                ifilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                ifilter.addAction(ACTION_POWER_SAVE_MODE_CHANGING);
                mContext.registerReceiver(this, ifilter);
                mRegistered = true;
            } else if (!listening && mRegistered) {
                mContext.unregisterReceiver(this);
                mRegistered = false;
            }
        }

    }
}
