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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.wifi.AddNetworkFragment;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiSavedConfigUtils;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class WifiNetworkListFragment extends SettingsPreferenceFragment implements
        WifiTracker.WifiListener, AccessPoint.AccessPointListener {
    private static final String TAG = "WifiNetworkListFragment";

    private static final String WIFI_CONFIG_KEY = "wifi_config_key";
    private static final String PREF_KEY_ACCESS_POINTS = "access_points";

    private static final int ADD_NETWORK_REQUEST = 1;

    private PreferenceCategory mAccessPointsPreferenceCategory;
    private AccessPointPreference.UserBadgeCache mUserBadgeCache;
    private Preference mAddPreference;
    // Only shows up if mIsTest == true
    private Preference mFakeNetworkPreference;

    private WifiManager mWifiManager;
    private WifiTracker mWifiTracker;

    private WifiManager.ActionListener mSaveListener;
    private boolean mIsTest;

    // Container Activity must implement this interface
    public interface OnChooseNetworkListener {
        void onChooseNetwork(WifiNetworkConfig wifiNetworkConfig);
    }
    private OnChooseNetworkListener mOnChooseNetworkListener;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
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

        mWifiTracker = WifiTrackerFactory.create(getActivity(), this,
                getSettingsLifecycle(), /* includeSaved */true, /* includeScans */ true);
        mWifiManager = mWifiTracker.getManager();

        final Bundle args = getArguments();
        if (args != null) {
            mIsTest = args.getBoolean(WifiDppUtils.EXTRA_TEST, false);
        }

        mSaveListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Do nothing.
            }

            @Override
            public void onFailure(int reason) {
                Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity,
                            R.string.wifi_failed_save_message,
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_NETWORK_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                handleAddNetworkSubmitEvent(data);
            }
            mWifiTracker.resumeScanning();
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.wifi_dpp_network_list);

        mAccessPointsPreferenceCategory = findPreference(PREF_KEY_ACCESS_POINTS);

        mFakeNetworkPreference = new Preference(getPrefContext());
        mFakeNetworkPreference.setIcon(R.drawable.ic_wifi_signal_0);
        mFakeNetworkPreference.setKey("fake_key");
        mFakeNetworkPreference.setTitle("fake network");

        mAddPreference = new Preference(getPrefContext());
        mAddPreference.setIcon(R.drawable.ic_add_24dp);
        mAddPreference.setTitle(R.string.wifi_add_network);

        mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());
    }

    /** Called when the state of Wifi has changed. */
    @Override
    public void onWifiStateChanged(int state) {
        final int wifiState = mWifiManager.getWifiState();
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                updateAccessPointPreferences();
                break;

            case WifiManager.WIFI_STATE_ENABLING:
            case WifiManager.WIFI_STATE_DISABLING:
                removeAccessPointPreferences();
                break;
        }
    }

    /** Called when the connection state of wifi has changed. */
    @Override
    public void onConnectedChanged() {
        // Do nothing.
    }

    /**
     * Called to indicate the list of AccessPoints has been updated and
     * getAccessPoints should be called to get the latest information.
     */
    @Override
    public void onAccessPointsChanged() {
        updateAccessPointPreferences();
    }

    @Override
    public void onAccessPointChanged(final AccessPoint accessPoint) {
        Log.d(TAG, "onAccessPointChanged (singular) callback initiated");
        View view = getView();
        if (view != null) {
            view.post(() -> {
                final Object tag = accessPoint.getTag();
                if (tag != null) {
                    ((AccessPointPreference) tag).refresh();
                }
            });
        }
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
        ((AccessPointPreference) accessPoint.getTag()).onLevelChanged();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof AccessPointPreference) {
            final AccessPoint selectedAccessPoint =
                    ((AccessPointPreference) preference).getAccessPoint();
            if (selectedAccessPoint == null) {
                return false;
            }

            // Launch WifiDppAddDeviceFragment to start DPP in Configurator-Initiator role.
            final WifiConfiguration wifiConfig = selectedAccessPoint.getConfig();
            if (wifiConfig == null) {
                throw new IllegalArgumentException("Invalid access point");
            }
            final WifiNetworkConfig networkConfig = WifiNetworkConfig.getValidConfigOrNull(
                    selectedAccessPoint.getSecurityString(/* concise */ true),
                    wifiConfig.getPrintableSsid(), wifiConfig.preSharedKey, wifiConfig.hiddenSSID,
                    wifiConfig.networkId, /* isHotspot */ false);
            if (mOnChooseNetworkListener != null) {
                mOnChooseNetworkListener.onChooseNetwork(networkConfig);
            }
        } else if (preference == mAddPreference) {
            launchAddNetworkFragment();
        } else if (preference == mFakeNetworkPreference) {
            if (mOnChooseNetworkListener != null) {
                mOnChooseNetworkListener.onChooseNetwork(
                        new WifiNetworkConfig(
                                WifiQrCode.SECURITY_WPA_PSK,
                                /* ssid */ WifiNetworkConfig.FAKE_SSID,
                                /* preSharedKey */ WifiNetworkConfig.FAKE_PASSWORD,
                                /* hiddenSsid */ true,
                                /* networkId */ WifiConfiguration.INVALID_NETWORK_ID,
                                /* isHotspot*/ false));
            }
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private void handleAddNetworkSubmitEvent(Intent data) {
        final WifiConfiguration wifiConfiguration = data.getParcelableExtra(WIFI_CONFIG_KEY);
        if (wifiConfiguration != null) {
            mWifiManager.save(wifiConfiguration, mSaveListener);
        }
    }

    private boolean isValidForDppConfiguration(AccessPoint accessPoint) {
        final int security = accessPoint.getSecurity();

        // DPP 1.0 only support PSK and SAE.
        return security == AccessPoint.SECURITY_PSK || security == AccessPoint.SECURITY_SAE;
    }

    private void launchAddNetworkFragment() {
        new SubSettingLauncher(getContext())
                .setTitleRes(R.string.wifi_add_network)
                .setDestination(AddNetworkFragment.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(this, ADD_NETWORK_REQUEST)
                .launch();
    }

    private void removeAccessPointPreferences() {
        mAccessPointsPreferenceCategory.removeAll();
        mAccessPointsPreferenceCategory.setVisible(false);
    }

    private void updateAccessPointPreferences() {
        // in case state has changed
        if (!mWifiManager.isWifiEnabled()) {
            return;
        }

        List<AccessPoint> savedAccessPoints =
                WifiSavedConfigUtils.getAllConfigs(getContext(), mWifiManager);

        savedAccessPoints = savedAccessPoints.stream()
                .filter(accessPoint -> isValidForDppConfiguration(accessPoint))
                .map(accessPoint -> getScannedAccessPointIfAvailable(accessPoint))
                .sorted((ap1, ap2) -> {
                    // orders reachable Wi-Fi networks on top
                    if (ap1.isReachable() && !ap2.isReachable()) {
                        return -1;
                    } else if (!ap1.isReachable() && ap2.isReachable()) {
                        return 1;
                    }

                    String ap1Title = nullToEmpty(ap1.getTitle());
                    String ap2Title = nullToEmpty(ap2.getTitle());

                    return ap1Title.compareToIgnoreCase(ap2Title);
                }).collect(Collectors.toList());

        int index = 0;
        mAccessPointsPreferenceCategory.removeAll();
        for (AccessPoint savedAccessPoint : savedAccessPoints) {
            final AccessPointPreference preference =
                    createAccessPointPreference(savedAccessPoint);

            preference.setOrder(index++);
            preference.setEnabled(savedAccessPoint.isReachable());
            savedAccessPoint.setListener(this);

            preference.refresh();
            mAccessPointsPreferenceCategory.addPreference(preference);
        }
        mAddPreference.setOrder(index);
        mAccessPointsPreferenceCategory.addPreference(mAddPreference);

        if (mIsTest) {
            mAccessPointsPreferenceCategory.addPreference(mFakeNetworkPreference);
        }
    }

    private String nullToEmpty(String string) {
        return (string == null) ? "" : string;
    }

    // Replaces with an AccessPoint from scanned result for signal information
    private AccessPoint getScannedAccessPointIfAvailable(AccessPoint savedAccessPoint) {
        final List<AccessPoint> scannedAccessPoints = mWifiTracker.getAccessPoints();
        final WifiConfiguration savedWifiConfiguration = savedAccessPoint.getConfig();
        for (AccessPoint scannedAccessPoint : scannedAccessPoints) {
            if (scannedAccessPoint.matches(savedWifiConfiguration)) {
                return scannedAccessPoint;
            }
        }
        return savedAccessPoint;
    }

    private AccessPointPreference createAccessPointPreference(AccessPoint accessPoint) {
        return new AccessPointPreference(accessPoint, getPrefContext(), mUserBadgeCache,
                R.drawable.ic_wifi_signal_0, /* forSavedNetworks */ false);
    }
}
