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

package com.android.settings.wifi.tether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

public class WifiTetherPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {

    public static final IntentFilter WIFI_TETHER_INTENT_FILTER;
    private static final String WIFI_TETHER_SETTINGS = "wifi_tether";

    private final ConnectivityManager mConnectivityManager;
    private final String[] mWifiRegexs;
    private final WifiManager mWifiManager;
    private final Lifecycle mLifecycle;
    private WifiTetherSwitchBarController mSwitchController;
    private MasterSwitchPreference mPreference;

    static {
        WIFI_TETHER_INTENT_FILTER = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        WIFI_TETHER_INTENT_FILTER.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        WIFI_TETHER_INTENT_FILTER.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    }

    public WifiTetherPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiRegexs = mConnectivityManager.getTetherableWifiRegexs();
        mLifecycle = lifecycle;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return mWifiRegexs != null
                && mWifiRegexs.length != 0
                && WifiTetherSettings.isTetherSettingPageEnabled()
                && !Utils.isMonkeyRunning();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (MasterSwitchPreference) screen.findPreference(WIFI_TETHER_SETTINGS);
        if (mPreference == null) {
            // unavailable
            return;
        }
        mSwitchController = new WifiTetherSwitchBarController(
                mContext, new MasterSwitchController(mPreference));
        mLifecycle.addObserver(mSwitchController);
    }

    @Override
    public String getPreferenceKey() {
        return WIFI_TETHER_SETTINGS;
    }

    @Override
    public void onStart() {
        if (mPreference != null) {
            mContext.registerReceiver(mReceiver, WIFI_TETHER_INTENT_FILTER);
            clearSummaryForAirplaneMode();
        }
    }

    @Override
    public void onStop() {
        if (mPreference != null) {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    //
    // Everything below is copied from WifiApEnabler
    //
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED);
                int reason = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_FAILURE_REASON,
                        WifiManager.SAP_START_FAILURE_GENERAL);
                handleWifiApStateChanged(state, reason);
            } else if (ConnectivityManager.ACTION_TETHER_STATE_CHANGED.equals(action)) {
                List<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                List<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                updateTetherState(active.toArray(), errored.toArray());
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                clearSummaryForAirplaneMode();
            }
        }
    };

    private void handleWifiApStateChanged(int state, int reason) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mPreference.setSummary(R.string.wifi_tether_starting);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                /**
                 * Summary on enable is handled by tether
                 * broadcast notice
                 */
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                mPreference.setSummary(R.string.wifi_tether_stopping);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                mPreference.setSummary(R.string.wifi_hotspot_off_subtext);
                clearSummaryForAirplaneMode();
                break;
            default:
                if (reason == WifiManager.SAP_START_FAILURE_NO_CHANNEL) {
                    mPreference.setSummary(R.string.wifi_sap_no_channel_error);
                } else {
                    mPreference.setSummary(R.string.wifi_error);
                }
                clearSummaryForAirplaneMode();
        }
    }

    private void updateTetherState(Object[] tethered, Object[] errored) {
        boolean wifiTethered = matchRegex(tethered);
        boolean wifiErrored = matchRegex(errored);

        if (wifiTethered) {
            WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
            updateConfigSummary(wifiConfig);
        } else if (wifiErrored) {
            mPreference.setSummary(R.string.wifi_error);
        } else {
            mPreference.setSummary(R.string.wifi_hotspot_off_subtext);
        }
    }

    private boolean matchRegex(Object[] tethers) {
        for (Object o : tethers) {
            String s = (String) o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateConfigSummary(WifiConfiguration wifiConfig) {
        final String s = mContext.getString(
                com.android.internal.R.string.wifi_tether_configure_ssid_default);

        mPreference.setSummary(mContext.getString(R.string.wifi_tether_enabled_subtext,
                BidiFormatter.getInstance().unicodeWrap(
                        (wifiConfig == null) ? s : wifiConfig.SSID)));
    }

    private void clearSummaryForAirplaneMode() {
        boolean isAirplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if (isAirplaneMode) {
            mPreference.setSummary(R.string.summary_placeholder);
        }
    }
    //
    // Everything above is copied from WifiApEnabler
    //
}
