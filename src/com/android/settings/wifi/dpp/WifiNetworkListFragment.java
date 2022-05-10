/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.wifi.AddNetworkFragment;
import com.android.settings.wifi.WifiEntryPreference;
import com.android.wifitrackerlib.SavedNetworkTracker;
import com.android.wifitrackerlib.WifiEntry;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public class WifiNetworkListFragment extends SettingsPreferenceFragment implements
        SavedNetworkTracker.SavedNetworkTrackerCallback, Preference.OnPreferenceClickListener {
    private static final String TAG = "WifiNetworkListFragment";

    @VisibleForTesting static final String WIFI_CONFIG_KEY = "wifi_config_key";
    private static final String PREF_KEY_ACCESS_POINTS = "access_points";

    @VisibleForTesting
    static final int ADD_NETWORK_REQUEST = 1;

    @VisibleForTesting PreferenceCategory mPreferenceGroup;
    @VisibleForTesting Preference mAddPreference;

    @VisibleForTesting WifiManager mWifiManager;

    private WifiManager.ActionListener mSaveListener;

    // Max age of tracked WifiEntries
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating SavedNetworkTracker scans
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    @VisibleForTesting SavedNetworkTracker mSavedNetworkTracker;
    @VisibleForTesting HandlerThread mWorkerThread;

    // Container Activity must implement this interface
    public interface OnChooseNetworkListener {
        void onChooseNetwork(WifiNetworkConfig wifiNetworkConfig);
    }
    @VisibleForTesting OnChooseNetworkListener mOnChooseNetworkListener;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }

    private static class DisableUnreachableWifiEntryPreference extends WifiEntryPreference {
        DisableUnreachableWifiEntryPreference(Context context, WifiEntry entry) {
            super(context, entry);
        }

        @Override
        public void onUpdated() {
            super.onUpdated();
            this.setEnabled(getWifiEntry().getLevel() != WifiEntry.WIFI_LEVEL_UNREACHABLE);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof OnChooseNetworkListener)) {
            throw new IllegalArgumentException("Invalid context type");
        }
        mOnChooseNetworkListener = (OnChooseNetworkListener) context;
    }

    @Override
    public void onDetach() {
        mOnChooseNetworkListener = null;
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = getContext();
        mWifiManager = context.getSystemService(WifiManager.class);

        mSaveListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Do nothing.
            }

            @Override
            public void onFailure(int reason) {
                final Activity activity = getActivity();
                if (activity != null && !activity.isFinishing()) {
                    Toast.makeText(activity, R.string.wifi_failed_save_message,
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        mWorkerThread = new HandlerThread(TAG
                + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
        mSavedNetworkTracker = new SavedNetworkTracker(getSettingsLifecycle(), context,
                context.getSystemService(WifiManager.class),
                context.getSystemService(ConnectivityManager.class),
                new Handler(Looper.getMainLooper()),
                mWorkerThread.getThreadHandler(),
                elapsedRealtimeClock,
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                this);
    }

    @Override
    public void onDestroyView() {
        mWorkerThread.quit();

        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_NETWORK_REQUEST && resultCode == Activity.RESULT_OK) {
            final WifiConfiguration wifiConfiguration = data.getParcelableExtra(WIFI_CONFIG_KEY);
            if (wifiConfiguration != null) {
                mWifiManager.save(wifiConfiguration, mSaveListener);
            }
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.wifi_dpp_network_list);

        mPreferenceGroup = findPreference(PREF_KEY_ACCESS_POINTS);

        mAddPreference = new Preference(getPrefContext());
        mAddPreference.setIcon(R.drawable.ic_add_24dp);
        mAddPreference.setTitle(R.string.wifi_add_network);
        mAddPreference.setOnPreferenceClickListener(this);
    }

    /** Called when the state of Wifi has changed. */
    @Override
    public void onSavedWifiEntriesChanged() {
        final List<WifiEntry> savedWifiEntries = mSavedNetworkTracker.getSavedWifiEntries().stream()
                .filter(entry -> isValidForDppConfiguration(entry))
                .collect(Collectors.toList());

        int index = 0;
        mPreferenceGroup.removeAll();
        for (WifiEntry savedEntry : savedWifiEntries) {
            final DisableUnreachableWifiEntryPreference preference =
                    new DisableUnreachableWifiEntryPreference(getContext(), savedEntry);
            preference.setOnPreferenceClickListener(this);
            preference.setEnabled(savedEntry.getLevel() != WifiEntry.WIFI_LEVEL_UNREACHABLE);
            preference.setOrder(index++);

            mPreferenceGroup.addPreference(preference);
        }

        mAddPreference.setOrder(index);
        mPreferenceGroup.addPreference(mAddPreference);
    }

    @Override
    public void onSubscriptionWifiEntriesChanged() {
        // Do nothing.
    }

    @Override
    public void onWifiStateChanged() {
        // Do nothing.
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof WifiEntryPreference) {
            final WifiEntry selectedWifiEntry = ((WifiEntryPreference) preference).getWifiEntry();

            // Launch WifiDppAddDeviceFragment to start DPP in Configurator-Initiator role.
            final WifiConfiguration wifiConfig = selectedWifiEntry.getWifiConfiguration();
            if (wifiConfig == null) {
                throw new IllegalArgumentException("Invalid access point");
            }
            final WifiNetworkConfig networkConfig = WifiNetworkConfig.getValidConfigOrNull(
                    WifiDppUtils.getSecurityString(selectedWifiEntry),
                    wifiConfig.getPrintableSsid(), wifiConfig.preSharedKey, wifiConfig.hiddenSSID,
                    wifiConfig.networkId, /* isHotspot */ false);
            if (mOnChooseNetworkListener != null) {
                mOnChooseNetworkListener.onChooseNetwork(networkConfig);
            }
        } else if (preference == mAddPreference) {
            new SubSettingLauncher(getContext())
                .setTitleRes(R.string.wifi_add_network)
                .setDestination(AddNetworkFragment.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(this, ADD_NETWORK_REQUEST)
                .launch();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private boolean isValidForDppConfiguration(WifiEntry wifiEntry) {
        final int security = wifiEntry.getSecurity();

        // DPP 1.0 only support PSK and SAE.
        return security == WifiEntry.SECURITY_PSK || security == WifiEntry.SECURITY_SAE;
    }
}
