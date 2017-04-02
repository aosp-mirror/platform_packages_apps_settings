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
package com.android.settings.wifi.details;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkBadging;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.core.lifecycle.events.OnResume;
import com.android.settings.wifi.WifiDetailPreference;
import com.android.settingslib.wifi.AccessPoint;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

/**
 * Controller for logic pertaining to displaying Wifi information for the
 * {@link WifiNetworkDetailsFragment}.
 */
public class WifiDetailPreferenceController extends PreferenceController implements
        LifecycleObserver, OnResume {
    private static final String TAG = "WifiDetailsPrefCtrl";

    @VisibleForTesting
    static final String KEY_CONNECTION_DETAIL_PREF = "connection_detail";
    @VisibleForTesting
    static final String KEY_SIGNAL_STRENGTH_PREF = "signal_strength";
    @VisibleForTesting
    static final String KEY_LINK_SPEED = "link_speed";
    @VisibleForTesting
    static final String KEY_FREQUENCY_PREF = "frequency";
    @VisibleForTesting
    static final String KEY_SECURITY_PREF = "security";
    @VisibleForTesting
    static final String KEY_IP_ADDRESS_PREF = "ip_address";
    @VisibleForTesting
    static final String KEY_ROUTER_PREF = "router";
    @VisibleForTesting
    static final String KEY_SUBNET_MASK_PREF = "subnet_mask";
    @VisibleForTesting
    static final String KEY_DNS_PREF = "dns";
    @VisibleForTesting
    static final String KEY_IPV6_ADDRESS_CATEGORY = "ipv6_details_category";

    private AccessPoint mAccessPoint;
    private NetworkInfo mNetworkInfo;
    private Context mPrefContext;
    private int mRssi;
    private String[] mSignalStr;
    private WifiConfiguration mWifiConfig;
    private WifiInfo mWifiInfo;
    private final WifiManager mWifiManager;

    // Preferences - in order of appearance
    private Preference mConnectionDetailPref;
    private WifiDetailPreference mSignalStrengthPref;
    private WifiDetailPreference mLinkSpeedPref;
    private WifiDetailPreference mFrequencyPref;
    private WifiDetailPreference mSecurityPref;
    private WifiDetailPreference mIpAddressPref;
    private WifiDetailPreference mRouterPref;
    private WifiDetailPreference mSubnetPref;
    private WifiDetailPreference mDnsPref;
    private PreferenceCategory mIpv6AddressCategory;

    public WifiDetailPreferenceController(AccessPoint accessPoint, Context context,
            Lifecycle lifecycle, WifiManager wifiManager) {
        super(context);

        mAccessPoint = accessPoint;
        mNetworkInfo = accessPoint.getNetworkInfo();
        mRssi = accessPoint.getRssi();
        mSignalStr = context.getResources().getStringArray(R.array.wifi_signal);
        mWifiConfig = accessPoint.getConfig();
        mWifiManager = wifiManager;
        mWifiInfo = wifiManager.getConnectionInfo();

        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        // Returns null since this controller contains more than one Preference
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPrefContext = screen.getPreferenceManager().getContext();

        mConnectionDetailPref = screen.findPreference(KEY_CONNECTION_DETAIL_PREF);

        mSignalStrengthPref =
                (WifiDetailPreference) screen.findPreference(KEY_SIGNAL_STRENGTH_PREF);
        mLinkSpeedPref = (WifiDetailPreference) screen.findPreference(KEY_LINK_SPEED);
        mFrequencyPref = (WifiDetailPreference) screen.findPreference(KEY_FREQUENCY_PREF);
        mSecurityPref = (WifiDetailPreference) screen.findPreference(KEY_SECURITY_PREF);

        mIpAddressPref = (WifiDetailPreference) screen.findPreference(KEY_IP_ADDRESS_PREF);
        mRouterPref = (WifiDetailPreference) screen.findPreference(KEY_ROUTER_PREF);
        mSubnetPref = (WifiDetailPreference) screen.findPreference(KEY_SUBNET_MASK_PREF);
        mDnsPref = (WifiDetailPreference) screen.findPreference(KEY_DNS_PREF);

        mIpv6AddressCategory =
                (PreferenceCategory) screen.findPreference(KEY_IPV6_ADDRESS_CATEGORY);

        mSecurityPref.setDetailText(mAccessPoint.getSecurityString(false /* concise */));
    }

    public WifiInfo getWifiInfo() {
        return mWifiInfo;
    }

    @Override
    public void onResume() {
        mWifiInfo = mWifiManager.getConnectionInfo();

        refreshFromWifiInfo();
        setIpText();
    }

    private void refreshFromWifiInfo() {
        if (mWifiInfo == null) {
            return;
        }
        mAccessPoint.update(mWifiConfig, mWifiInfo, mNetworkInfo);

        int iconSignalLevel = WifiManager.calculateSignalLevel(
                mWifiInfo.getRssi(), WifiManager.RSSI_LEVELS);
        Drawable wifiIcon = NetworkBadging.getWifiIcon(
                iconSignalLevel, NetworkBadging.BADGING_NONE, mContext.getTheme()).mutate();

        // Connected Header Pref
        mConnectionDetailPref.setIcon(wifiIcon);
        mConnectionDetailPref.setTitle(mAccessPoint.getSettingsSummary());

        // Signal Strength Pref
        Drawable wifiIconDark = wifiIcon.getConstantState().newDrawable().mutate();
        wifiIconDark.setTint(mContext.getResources().getColor(
                R.color.wifi_details_icon_color, mContext.getTheme()));
        mSignalStrengthPref.setIcon(wifiIconDark);

        int summarySignalLevel = mAccessPoint.getLevel();
        mSignalStrengthPref.setDetailText(mSignalStr[summarySignalLevel]);

        // Link Speed Pref
        mLinkSpeedPref.setDetailText(mContext.getString(
                R.string.link_speed, mWifiInfo.getLinkSpeed()));

        // Frequency Pref
        final int frequency = mWifiInfo.getFrequency();
        String band = null;
        if (frequency >= AccessPoint.LOWER_FREQ_24GHZ
                && frequency < AccessPoint.HIGHER_FREQ_24GHZ) {
            band = mContext.getResources().getString(R.string.wifi_band_24ghz);
        } else if (frequency >= AccessPoint.LOWER_FREQ_5GHZ
                && frequency < AccessPoint.HIGHER_FREQ_5GHZ) {
            band = mContext.getResources().getString(R.string.wifi_band_5ghz);
        } else {
            Log.e(TAG, "Unexpected frequency " + frequency);
        }
        mFrequencyPref.setDetailText(band);
    }

    private void setIpText() {
        mIpv6AddressCategory.removeAll();
        mIpv6AddressCategory.setVisible(false);

        Network currentNetwork = mWifiManager.getCurrentNetwork();
        if (currentNetwork == null) {
            return;
        }

        ConnectivityManager cm = mContext.getSystemService(ConnectivityManager.class);
        LinkProperties prop = cm.getLinkProperties(currentNetwork);
        List<InetAddress> addresses = prop.getAllAddresses();

        // Set ip addresses
        for (int i = 0; i < addresses.size(); i++) {
            InetAddress addr = addresses.get(i);
            if (addr instanceof Inet4Address) {
                mIpAddressPref.setDetailText(addr.getHostAddress());
            } else if (addr instanceof Inet6Address) {
                String ip = addr.getHostAddress();
                Preference pref = new Preference(mPrefContext);
                pref.setKey(ip);
                pref.setTitle(ip);
                mIpv6AddressCategory.addPreference(pref);
                mIpv6AddressCategory.setVisible(true); // TODO(sghuman): Make sure to
            }
        }

        String subnetMask = null;
        String router;
        DhcpInfo dhcp = mWifiManager.getDhcpInfo();
        if (dhcp != null) {
            if (dhcp.netmask == 0) {
                Log.e(TAG, "invalid netmask value of 0 for DhcpInfo: " + dhcp);
                mSubnetPref.setVisible(false);
            } else {
                subnetMask = NetworkUtils.intToInetAddress(dhcp.netmask).getHostAddress();
                mSubnetPref.setVisible(true);
            }

            router = NetworkUtils.intToInetAddress(dhcp.gateway).getHostAddress();
        } else { // Statically configured IP

            // TODO(sghuman): How do we get subnet mask for static ips?
            mSubnetPref.setVisible(false);

            router = mWifiManager.getWifiApConfiguration().getStaticIpConfiguration().gateway
                    .getHostAddress();
        }
        mRouterPref.setDetailText(router);
        mSubnetPref.setDetailText(subnetMask);

        // Set DNS
        addresses = prop.getDnsServers();
        StringBuilder builder = new StringBuilder();

        // addresses is backed by an ArrayList, so use a hand-written iterator for performance gains
        for (int i = 0; i < addresses.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(addresses.get(i).getHostAddress());
        }
        mDnsPref.setDetailText(builder.toString());
    }

    /**
     * Returns whether the network represented by this preference can be forgotten.
     */
    public boolean canForgetNetwork() {
        return mWifiInfo != null && mWifiInfo.isEphemeral() || mWifiConfig != null;
    }

    /**
     * Forgets the wifi network associated with this preference.
     */
    public void forgetNetwork() {
        if (mWifiInfo != null && mWifiInfo.isEphemeral()) {
            mWifiManager.disableEphemeralNetwork(mWifiInfo.getSSID());
        } else if (mWifiConfig != null) {
            if (mWifiConfig.isPasspoint()) {
                mWifiManager.removePasspointConfiguration(mWifiConfig.FQDN);
            } else {
                mWifiManager.forget(mWifiConfig.networkId, null /* action listener */);
            }
        }
    }
}
