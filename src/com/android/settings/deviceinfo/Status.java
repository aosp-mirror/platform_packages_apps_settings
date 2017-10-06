/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.Arrays;
import java.util.List;

/**
 * Fragment for showing device hardware info, such as MAC addresses and serial numbers
 */
public class Status extends SettingsPreferenceFragment implements Indexable {

    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_SIM_STATUS = "sim_status";
    private static final String KEY_IMEI_INFO = "imei_info";

    private SerialNumberPreferenceController mSerialNumberPreferenceController;
    private UptimePreferenceController mUptimePreferenceController;
    private Preference mBatteryStatus;
    private Preference mBatteryLevel;
    private BluetoothAddressPreferenceController mBluetoothAddressPreferenceController;
    private IpAddressPreferenceController mIpAddressPreferenceController;
    private WifiMacAddressPreferenceController mWifiMacAddressPreferenceController;
    private ImsStatusPreferenceController mImsStatusPreferenceController;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                mBatteryStatus.setSummary(Utils.getBatteryStatus(getResources(), intent));
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getContext();
        final Lifecycle lifecycle = getLifecycle();
        mSerialNumberPreferenceController = new SerialNumberPreferenceController(context);
        mUptimePreferenceController = new UptimePreferenceController(context, lifecycle);
        mBluetoothAddressPreferenceController =
                new BluetoothAddressPreferenceController(context, lifecycle);
        mIpAddressPreferenceController = new IpAddressPreferenceController(context, lifecycle);
        mWifiMacAddressPreferenceController =
                new WifiMacAddressPreferenceController(context, lifecycle);
        mImsStatusPreferenceController = new ImsStatusPreferenceController(context, lifecycle);

        addPreferencesFromResource(R.xml.device_info_status);
        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);

        final PreferenceScreen screen = getPreferenceScreen();

        mSerialNumberPreferenceController.displayPreference(screen);
        mUptimePreferenceController.displayPreference(screen);
        mBluetoothAddressPreferenceController.displayPreference(screen);
        mIpAddressPreferenceController.displayPreference(screen);
        mWifiMacAddressPreferenceController.displayPreference(screen);
        mImsStatusPreferenceController.displayPreference(screen);

        // Remove SimStatus and Imei for Secondary user as it access Phone b/19165700
        // Also remove on Wi-Fi only devices.
        //TODO: the bug above will surface in split system user mode.
        if (!UserManager.get(getContext()).isAdminUser()
                || Utils.isWifiOnly(getContext())) {
            removePreferenceFromScreen(KEY_SIM_STATUS);
            removePreferenceFromScreen(KEY_IMEI_INFO);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_STATUS;
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().registerReceiver(mBatteryInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();

        getContext().unregisterReceiver(mBatteryInfoReceiver);
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.device_info_status;
                    return Arrays.asList(sir);
                }
            };
}
