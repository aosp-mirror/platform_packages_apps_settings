/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import com.android.settings.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;


class AccessPoint extends Preference {
    static final String TAG = "Settings.AccessPoint";

    /**
     * Lower bound on the 2.4 GHz (802.11b/g/n) WLAN channels
     */
    public static final int LOWER_FREQ_24GHZ = 2400;

    /**
     * Upper bound on the 2.4 GHz (802.11b/g/n) WLAN channels
     */
    public static final int HIGHER_FREQ_24GHZ = 2500;

    /**
     * Lower bound on the 5.0 GHz (802.11a/h/j/n/ac) WLAN channels
     */
    public static final int LOWER_FREQ_5GHZ = 4900;

    /**
     * Upper bound on the 5.0 GHz (802.11a/h/j/n/ac) WLAN channels
     */
    public static final int HIGHER_FREQ_5GHZ = 5900;

    /**
     * Experimental: we should be able to show the user the list of BSSIDs and bands
     *  for that SSID.
     *  For now this data is used only with Verbose Logging so as to show the band and number
     *  of BSSIDs on which that network is seen.
     */
    public LruCache<String, ScanResult> mScanResultCache;


    private static final String KEY_NETWORKINFO = "key_networkinfo";
    private static final String KEY_WIFIINFO = "key_wifiinfo";
    private static final String KEY_SCANRESULT = "key_scanresult";
    private static final String KEY_CONFIG = "key_config";

    private static final int[] STATE_SECURED = {
        R.attr.state_encrypted
    };
    private static final int[] STATE_NONE = {};

    private static int[] wifi_signal_attributes = { R.attr.wifi_signal };

    /**
     * These values are matched in string arrays -- changes must be kept in sync
     */
    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;

    enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }

    String ssid;
    String bssid;
    int security;
    int networkId = -1;
    boolean wpsAvailable = false;
    boolean showSummary = true;

    PskType pskType = PskType.UNKNOWN;

    private WifiConfiguration mConfig;
    /* package */ScanResult mScanResult;

    private int mRssi = Integer.MAX_VALUE;
    private long mSeen = 0;

    private WifiInfo mInfo;
    private NetworkInfo mNetworkInfo;
    private TextView mSummaryView;

    private static final int VISIBILITY_MAX_AGE_IN_MILLI = 1000000;
    private static final int VISIBILITY_OUTDATED_AGE_IN_MILLI = 20000;
    private static final int SECOND_TO_MILLI = 1000;

    static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    private static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    public String getSecurityString(boolean concise) {
        Context context = getContext();
        switch(security) {
            case SECURITY_EAP:
                return concise ? context.getString(R.string.wifi_security_short_eap) :
                    context.getString(R.string.wifi_security_eap);
            case SECURITY_PSK:
                switch (pskType) {
                    case WPA:
                        return concise ? context.getString(R.string.wifi_security_short_wpa) :
                            context.getString(R.string.wifi_security_wpa);
                    case WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa2) :
                            context.getString(R.string.wifi_security_wpa2);
                    case WPA_WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa_wpa2) :
                            context.getString(R.string.wifi_security_wpa_wpa2);
                    case UNKNOWN:
                    default:
                        return concise ? context.getString(R.string.wifi_security_short_psk_generic)
                                : context.getString(R.string.wifi_security_psk_generic);
                }
            case SECURITY_WEP:
                return concise ? context.getString(R.string.wifi_security_short_wep) :
                    context.getString(R.string.wifi_security_wep);
            case SECURITY_NONE:
            default:
                return concise ? "" : context.getString(R.string.wifi_security_none);
        }
    }

    private static PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            Log.w(TAG, "Received abnormal flag string: " + result.capabilities);
            return PskType.UNKNOWN;
        }
    }

    AccessPoint(Context context, WifiConfiguration config) {
        super(context);
        loadConfig(config);
        refresh();
    }

    AccessPoint(Context context, ScanResult result) {
        super(context);
        loadResult(result);
        refresh();
    }

    AccessPoint(Context context, Bundle savedState) {
        super(context);

        mConfig = savedState.getParcelable(KEY_CONFIG);
        if (mConfig != null) {
            loadConfig(mConfig);
        }
        mScanResult = (ScanResult) savedState.getParcelable(KEY_SCANRESULT);
        if (mScanResult != null) {
            loadResult(mScanResult);
        }
        mInfo = (WifiInfo) savedState.getParcelable(KEY_WIFIINFO);
        if (savedState.containsKey(KEY_NETWORKINFO)) {
            mNetworkInfo = savedState.getParcelable(KEY_NETWORKINFO);
        }
        update(mInfo, mNetworkInfo);
    }

    public void saveWifiState(Bundle savedState) {
        savedState.putParcelable(KEY_CONFIG, mConfig);
        savedState.putParcelable(KEY_SCANRESULT, mScanResult);
        savedState.putParcelable(KEY_WIFIINFO, mInfo);
        if (mNetworkInfo != null) {
            savedState.putParcelable(KEY_NETWORKINFO, mNetworkInfo);
        }
    }

    private void loadConfig(WifiConfiguration config) {
        ssid = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
        bssid = config.BSSID;
        security = getSecurity(config);
        networkId = config.networkId;
        mConfig = config;
    }

    private void loadResult(ScanResult result) {
        ssid = result.SSID;
        bssid = result.BSSID;
        security = getSecurity(result);
        wpsAvailable = security != SECURITY_EAP && result.capabilities.contains("WPS");
        if (security == SECURITY_PSK)
            pskType = getPskType(result);
        mRssi = result.level;
        mScanResult = result;
        if (result.seen > mSeen) {
            mSeen = result.seen;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        updateIcon(getLevel(), getContext());

        mSummaryView = (TextView) view.findViewById(com.android.internal.R.id.summary);
        mSummaryView.setVisibility(showSummary ? View.VISIBLE : View.GONE);

        notifyChanged();
    }

    protected void updateIcon(int level, Context context) {
        if (level == -1) {
            setIcon(null);
        } else {
            Drawable drawable = getIcon();

            if (drawable == null) {
                // To avoid a drawing race condition, we first set the state (SECURE/NONE) and then
                // set the icon (drawable) to that state's drawable.
                StateListDrawable sld = (StateListDrawable) context.getTheme()
                        .obtainStyledAttributes(wifi_signal_attributes).getDrawable(0);
                // If sld is null then we are indexing and therefore do not have access to
                // (nor need to display) the drawable.
                if (sld != null) {
                    sld.setState((security != SECURITY_NONE) ? STATE_SECURED : STATE_NONE);
                    drawable = sld.getCurrent();
                    setIcon(drawable);
                }
            }

            if (drawable != null) {
                drawable.setLevel(level);
            }
        }
    }

    @Override
    public int compareTo(Preference preference) {
        if (!(preference instanceof AccessPoint)) {
            return 1;
        }
        AccessPoint other = (AccessPoint) preference;
        // Active one goes first.
        if (isActive() && !other.isActive()) return -1;
        if (!isActive() && other.isActive()) return 1;

        // Reachable one goes before unreachable one.
        if (mRssi != Integer.MAX_VALUE && other.mRssi == Integer.MAX_VALUE) return -1;
        if (mRssi == Integer.MAX_VALUE && other.mRssi != Integer.MAX_VALUE) return 1;

        // Configured one goes before unconfigured one.
        if (networkId != WifiConfiguration.INVALID_NETWORK_ID
                && other.networkId == WifiConfiguration.INVALID_NETWORK_ID) return -1;
        if (networkId == WifiConfiguration.INVALID_NETWORK_ID
                && other.networkId != WifiConfiguration.INVALID_NETWORK_ID) return 1;

        // Sort by signal strength.
        int difference = WifiManager.compareSignalLevel(other.mRssi, mRssi);
        if (difference != 0) {
            return difference;
        }
        // Sort by ssid.
        return ssid.compareToIgnoreCase(other.ssid);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AccessPoint)) return false;
        return (this.compareTo((AccessPoint) other) == 0);
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (mInfo != null) result += 13 * mInfo.hashCode();
        result += 19 * mRssi;
        result += 23 * networkId;
        result += 29 * ssid.hashCode();
        return result;
    }

    boolean update(ScanResult result) {
        if (result.seen > mSeen) {
            mSeen = result.seen;
        }
        if (WifiSettings.mVerboseLogging > 0) {
            if (mScanResultCache == null) {
                mScanResultCache = new LruCache<String, ScanResult>(32);
            }
            mScanResultCache.put(result.BSSID, result);
        }

        if (ssid.equals(result.SSID) && security == getSecurity(result)) {
            if (WifiManager.compareSignalLevel(result.level, mRssi) > 0) {
                int oldLevel = getLevel();
                mRssi = result.level;
                if (getLevel() != oldLevel) {
                    notifyChanged();
                }
            }
            // This flag only comes from scans, is not easily saved in config
            if (security == SECURITY_PSK) {
                pskType = getPskType(result);
            }
            mScanResult = result;
            refresh();
            return true;
        }
        return false;
    }

    /** Return whether the given {@link WifiInfo} is for this access point. */
    private boolean isInfoForThisAccessPoint(WifiInfo info) {
        if (networkId != WifiConfiguration.INVALID_NETWORK_ID) {
            return networkId == info.getNetworkId();
        } else {
            // Might be an ephemeral connection with no WifiConfiguration. Try matching on SSID.
            // (Note that we only do this if the WifiConfiguration explicitly equals INVALID).
            // TODO: Handle hex string SSIDs.
            return ssid.equals(removeDoubleQuotes(info.getSSID()));
        }
    }

    void update(WifiInfo info, NetworkInfo networkInfo) {
        boolean reorder = false;
        if (info != null && isInfoForThisAccessPoint(info)) {
            reorder = (mInfo == null);
            mRssi = info.getRssi();
            mInfo = info;
            mNetworkInfo = networkInfo;
            refresh();
        } else if (mInfo != null) {
            reorder = true;
            mInfo = null;
            mNetworkInfo = null;
            refresh();
        }
        if (reorder) {
            notifyHierarchyChanged();
        }
    }

    int getLevel() {
        if (mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(mRssi, 4);
    }

    WifiConfiguration getConfig() {
        return mConfig;
    }

    WifiInfo getInfo() {
        return mInfo;
    }

    NetworkInfo getNetworkInfo() {
        return mNetworkInfo;
    }

    DetailedState getState() {
        return mNetworkInfo != null ? mNetworkInfo.getDetailedState() : null;
    }

    static String removeDoubleQuotes(String string) {
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    /**
     * Shows or Hides the Summary of an AccessPoint.
     *
     * @param showSummary true will show the summary, false will hide the summary
     */
    public void setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
        if (mSummaryView != null) {
            mSummaryView.setVisibility(showSummary ? View.VISIBLE : View.GONE);
        } // otherwise, will be handled in onBindView.
    }

    /**
     * Returns the visibility status of the WifiConfiguration.
     *
     * @return autojoin debugging information
     * TODO: use a string formatter
     * ["rssi 5Ghz", "num results on 5GHz" / "rssi 5Ghz", "num results on 5GHz"]
     * For instance [-40,5/-30,2]
     */
    private String getVisibilityStatus() {
        StringBuilder visibility = new StringBuilder();
        StringBuilder scans24GHz = null;
        StringBuilder scans5GHz = null;
        String bssid = null;

        long now = System.currentTimeMillis();

        if (mInfo != null) {
            bssid = mInfo.getBSSID();
            if (bssid != null) {
                visibility.append(" ").append(bssid);
            }
            visibility.append(" rssi=").append(mInfo.getRssi());
            visibility.append(" ");
            visibility.append(" score=").append(mInfo.score);
            visibility.append(String.format(" tx=%.1f,", mInfo.txSuccessRate));
            visibility.append(String.format("%.1f,", mInfo.txRetriesRate));
            visibility.append(String.format("%.1f ", mInfo.txBadRate));
            visibility.append(String.format("rx=%.1f", mInfo.rxSuccessRate));
        }

        if (mScanResultCache != null) {
            int rssi5 = WifiConfiguration.INVALID_RSSI;
            int rssi24 = WifiConfiguration.INVALID_RSSI;
            int num5 = 0;
            int num24 = 0;
            int numBlackListed = 0;
            int n24 = 0; // Number scan results we included in the string
            int n5 = 0; // Number scan results we included in the string
            Map<String, ScanResult> list = mScanResultCache.snapshot();
            // TODO: sort list by RSSI or age
            for (ScanResult result : list.values()) {
                if (result.seen == 0)
                    continue;

                if (result.autoJoinStatus != ScanResult.ENABLED) numBlackListed++;

                if (result.frequency >= LOWER_FREQ_5GHZ
                        && result.frequency <= HIGHER_FREQ_5GHZ) {
                    // Strictly speaking: [4915, 5825]
                    // number of known BSSID on 5GHz band
                    num5 = num5 + 1;
                } else if (result.frequency >= LOWER_FREQ_24GHZ
                        && result.frequency <= HIGHER_FREQ_24GHZ) {
                    // Strictly speaking: [2412, 2482]
                    // number of known BSSID on 2.4Ghz band
                    num24 = num24 + 1;
                }

                // Ignore results seen, older than 20 seconds
                if (now - result.seen > VISIBILITY_OUTDATED_AGE_IN_MILLI) continue;

                if (result.frequency >= LOWER_FREQ_5GHZ
                        && result.frequency <= HIGHER_FREQ_5GHZ) {
                    if (result.level > rssi5) {
                        rssi5 = result.level;
                    }
                    if (n5 < 4) {
                        if (scans5GHz == null) scans5GHz = new StringBuilder();
                        scans5GHz.append(" \n{").append(result.BSSID);
                        if (bssid != null && result.BSSID.equals(bssid)) scans5GHz.append("*");
                        scans5GHz.append("=").append(result.frequency);
                        scans5GHz.append(",").append(result.level);
                        if (result.autoJoinStatus != 0) {
                            scans5GHz.append(",st=").append(result.autoJoinStatus);
                        }
                        if (result.numIpConfigFailures != 0) {
                            scans5GHz.append(",ipf=").append(result.numIpConfigFailures);
                        }
                        scans5GHz.append("}");
                        n5++;
                    }
                } else if (result.frequency >= LOWER_FREQ_24GHZ
                        && result.frequency <= HIGHER_FREQ_24GHZ) {
                    if (result.level > rssi24) {
                        rssi24 = result.level;
                    }
                    if (n24 < 4) {
                        if (scans24GHz == null) scans24GHz = new StringBuilder();
                        scans24GHz.append(" \n{").append(result.BSSID);
                        if (bssid != null && result.BSSID.equals(bssid)) scans24GHz.append("*");
                        scans24GHz.append("=").append(result.frequency);
                        scans24GHz.append(",").append(result.level);
                        if (result.autoJoinStatus != 0) {
                            scans24GHz.append(",st=").append(result.autoJoinStatus);
                        }
                        if (result.numIpConfigFailures != 0) {
                            scans24GHz.append(",ipf=").append(result.numIpConfigFailures);
                        }
                        scans24GHz.append("}");
                        n24++;
                    }
                }
            }
            visibility.append(" [");
            if (num24 > 0) {
                visibility.append("(").append(num24).append(")");
                if (n24 <= 4) {
                    if (scans24GHz != null) {
                        visibility.append(scans24GHz.toString());
                    }
                } else {
                    visibility.append("max=").append(rssi24);
                    if (scans24GHz != null) {
                        visibility.append(",").append(scans24GHz.toString());
                    }
                }
            }
            visibility.append(";");
            if (num5 > 0) {
                visibility.append("(").append(num5).append(")");
                if (n5 <= 4) {
                    if (scans5GHz != null) {
                        visibility.append(scans5GHz.toString());
                    }
                } else {
                    visibility.append("max=").append(rssi5);
                    if (scans5GHz != null) {
                        visibility.append(",").append(scans5GHz.toString());
                    }
                }
            }
            if (numBlackListed > 0)
                visibility.append("!").append(numBlackListed);
            visibility.append("]");
        } else {
            if (mRssi != Integer.MAX_VALUE) {
                visibility.append(" rssi=");
                visibility.append(mRssi);
                if (mScanResult != null) {
                    visibility.append(", f=");
                    visibility.append(mScanResult.frequency);
                }
            }
        }

        return visibility.toString();
    }

    /**
     * Return whether this is the active connection.
     * For ephemeral connections (networkId is invalid), this returns false if the network is
     * disconnected.
     */
    boolean isActive() {
        return mNetworkInfo != null &&
                (networkId != WifiConfiguration.INVALID_NETWORK_ID ||
                 mNetworkInfo.getState() != State.DISCONNECTED);
    }

    /**
     * Updates the title and summary; may indirectly call notifyChanged().
     */
    private void refresh() {
        setTitle(ssid);

        final Context context = getContext();
        updateIcon(getLevel(), context);

        // Force new summary
        setSummary(null);

        // Update to new summary
        StringBuilder summary = new StringBuilder();

        if (isActive()) { // This is the active connection
            summary.append(Summary.get(context, getState(),
                    networkId == WifiConfiguration.INVALID_NETWORK_ID));
        } else if (mConfig != null
                && mConfig.hasNoInternetAccess()) {
            summary.append(context.getString(R.string.wifi_no_internet));
        } else if (mConfig != null && ((mConfig.status == WifiConfiguration.Status.DISABLED &&
                mConfig.disableReason != WifiConfiguration.DISABLED_UNKNOWN_REASON)
               || mConfig.autoJoinStatus
                >= WifiConfiguration.AUTO_JOIN_DISABLED_ON_AUTH_FAILURE)) {
            if (mConfig.autoJoinStatus
                    >= WifiConfiguration.AUTO_JOIN_DISABLED_ON_AUTH_FAILURE) {
                if (mConfig.disableReason == WifiConfiguration.DISABLED_DHCP_FAILURE) {
                    summary.append(context.getString(R.string.wifi_disabled_network_failure));
                } else if (mConfig.disableReason == WifiConfiguration.DISABLED_AUTH_FAILURE) {
                    summary.append(context.getString(R.string.wifi_disabled_password_failure));
                } else {
                    summary.append(context.getString(R.string.wifi_disabled_wifi_failure));
                }
            } else {
                switch (mConfig.disableReason) {
                    case WifiConfiguration.DISABLED_AUTH_FAILURE:
                        summary.append(context.getString(R.string.wifi_disabled_password_failure));
                        break;
                    case WifiConfiguration.DISABLED_DHCP_FAILURE:
                    case WifiConfiguration.DISABLED_DNS_FAILURE:
                        summary.append(context.getString(R.string.wifi_disabled_network_failure));
                        break;
                    case WifiConfiguration.DISABLED_UNKNOWN_REASON:
                    case WifiConfiguration.DISABLED_ASSOCIATION_REJECT:
                        summary.append(context.getString(R.string.wifi_disabled_generic));
                        break;
                }
            }
        } else if (mRssi == Integer.MAX_VALUE) { // Wifi out of range
            summary.append(context.getString(R.string.wifi_not_in_range));
        } else { // In range, not disabled.
            if (mConfig != null) { // Is saved network
                summary.append(context.getString(R.string.wifi_remembered));
            }
        }

        if (WifiSettings.mVerboseLogging > 0) {
            // Add RSSI/band information for this config, what was seen up to 6 seconds ago
            // verbose WiFi Logging is only turned on thru developers settings
            if (mInfo != null && mNetworkInfo != null) { // This is the active connection
                summary.append(" f=" + Integer.toString(mInfo.getFrequency()));
            }
            summary.append(" " + getVisibilityStatus());
            if (mConfig != null && mConfig.autoJoinStatus > 0) {
                summary.append(" (" + mConfig.autoJoinStatus);
                if (mConfig.blackListTimestamp > 0) {
                    long now = System.currentTimeMillis();
                    long diff = (now - mConfig.blackListTimestamp)/1000;
                    long sec = diff%60; //seconds
                    long min = (diff/60)%60; //minutes
                    long hour = (min/60)%60; //hours
                    summary.append(", ");
                    if (hour > 0) summary.append(Long.toString(hour) + "h ");
                    summary.append( Long.toString(min) + "m ");
                    summary.append( Long.toString(sec) + "s ");
                }
                summary.append(")");
            }
            if (mConfig != null && mConfig.numIpConfigFailures > 0) {
                summary.append(" ipf=").append(mConfig.numIpConfigFailures);
            }
            if (mConfig != null && mConfig.numConnectionFailures > 0) {
                summary.append(" cf=").append(mConfig.numConnectionFailures);
            }
            if (mConfig != null && mConfig.numAuthFailures > 0) {
                summary.append(" authf=").append(mConfig.numAuthFailures);
            }
            if (mConfig != null && mConfig.numNoInternetAccessReports > 0) {
                summary.append(" noInt=").append(mConfig.numNoInternetAccessReports);
            }
        }

        if (summary.length() > 0) {
            setSummary(summary.toString());
            setShowSummary(true);
        } else {
            setShowSummary(false);
        }
    }

    /**
     * Generate and save a default wifiConfiguration with common values.
     * Can only be called for unsecured networks.
     * @hide
     */
    protected void generateOpenNetworkConfig() {
        if (security != SECURITY_NONE)
            throw new IllegalStateException();
        if (mConfig != null)
            return;
        mConfig = new WifiConfiguration();
        mConfig.SSID = AccessPoint.convertToQuotedString(ssid);
        mConfig.allowedKeyManagement.set(KeyMgmt.NONE);
    }
}
