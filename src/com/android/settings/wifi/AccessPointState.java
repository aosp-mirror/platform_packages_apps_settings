/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.GroupCipher;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.PairwiseCipher;
import android.net.wifi.WifiConfiguration.Protocol;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

public final class AccessPointState implements Comparable<AccessPointState>, Parcelable {

    private static final String TAG = "AccessPointState";
    
    // Constants used for different security types
    public static final String WPA2 = "WPA2";
    public static final String WPA = "WPA";
    public static final String WEP = "WEP";
    public static final String OPEN = "Open";

    /** String present in capabilities if the scan result is ad-hoc */
    private static final String ADHOC_CAPABILITY = "[IBSS]";
    /** String present in capabilities if the scan result is enterprise secured */
    private static final String ENTERPRISE_CAPABILITY = "-EAP-";

    public static final String BSSID_ANY = "any";
    public static final int NETWORK_ID_NOT_SET = -1;
    /** This should be used with care! */
    static final int NETWORK_ID_ANY = -2;
    
    public static final int MATCH_NONE = 0;
    public static final int MATCH_WEAK = 1;
    public static final int MATCH_STRONG = 2;
    public static final int MATCH_EXACT = 3;
    
    // Don't set these directly, use the setters.
    public int networkId;
    public int priority;
    public boolean hiddenSsid;
    public int linkSpeed;
    public int ipAddress;
    public String bssid;
    public String ssid;
    public int signal;
    public boolean primary;
    public boolean seen;
    public boolean configured;
    public NetworkInfo.DetailedState status;
    public String security;
    public boolean disabled;
    
    /**
     * Use this for sorting based on signal strength. It is a heavily-damped
     * time-averaged weighted signal.
     */
    private float signalForSorting = Float.MIN_VALUE;
    
    private static final float DAMPING_FACTOR = 0.2f;
    
    /**
     * This will be a user entered password, and NOT taken from wpa_supplicant
     * (since it would give us *)
     */
    private String mPassword; 
    private boolean mConfigHadPassword;
    
    public static final int WEP_PASSWORD_AUTO = 0;
    public static final int WEP_PASSWORD_ASCII = 1;
    public static final int WEP_PASSWORD_HEX = 2;
    private int mWepPasswordType;
    
    private Context mContext;

    /**
     * If > 0, don't refresh (changes are being batched), use
     * {@link #blockRefresh()} and {@link #unblockRefresh()} only.
     */
    private int mBlockRefresh;
    /**
     * This will be set by {@link #requestRefresh} and shouldn't be written to
     * elsewhere.
     */
    private boolean mNeedsRefresh;    
    
    private AccessPointStateCallback mCallback;
    
    private StringBuilder mSummaryBuilder = new StringBuilder();
    
    interface AccessPointStateCallback {
        void refreshAccessPointState();
    }
    
    public AccessPointState(Context context) {
        this();
        
        setContext(context);
    }

    private AccessPointState() {
        bssid = BSSID_ANY;
        ssid = "";
        networkId = NETWORK_ID_NOT_SET;
        hiddenSsid = false;
    }

    void setContext(Context context) {
        mContext = context;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }
    
    public void setBssid(String bssid) {
        if (bssid != null) {
            // If the BSSID is a wildcard, do NOT let a specific BSSID replace it
            if (!this.bssid.equals(BSSID_ANY)) {
                this.bssid = bssid;
            }
        }
    }

    private String getWpaSupplicantBssid() {
        return bssid.equals(BSSID_ANY) ? null : bssid;
    }
    
    public static String convertToQuotedString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        
        final int lastPos = string.length() - 1;
        if (lastPos < 0 || (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
            return string;
        }
        
        return "\"" + string + "\"";
    }
    
    public void setPrimary(boolean primary) {
        if (this.primary != primary) {
            this.primary = primary;
            requestRefresh();
        }
    }
    
    public void setSeen(boolean seen) {
        if (this.seen != seen) {
            this.seen = seen;
            requestRefresh();
        }
    }
    
    public void setDisabled(boolean disabled) {
        if (this.disabled != disabled) {
            this.disabled = disabled;
            requestRefresh();
        }
    }
    
    public void setSignal(int signal) {

        if (signalForSorting == Float.MIN_VALUE) {
            signalForSorting = signal;
        } else {
            signalForSorting = (DAMPING_FACTOR * signal) + ((1-DAMPING_FACTOR) * signalForSorting);
        }

        if (this.signal != signal) {
            this.signal = signal;
            requestRefresh();
        }
    }
    
    public String getHumanReadableSsid() {
        if (TextUtils.isEmpty(ssid)) {
            return "";
        }
        
        final int lastPos = ssid.length() - 1;
        if (ssid.charAt(0) == '"' && ssid.charAt(lastPos) == '"') {
            return ssid.substring(1, lastPos);
        }
        
        return ssid;
    }
    
    public void setSsid(String ssid) {
        if (ssid != null) {
            this.ssid = convertToQuotedString(ssid);
            requestRefresh();
        }
    }
    
    public void setPriority(int priority) {
        if (this.priority != priority) {
            this.priority = priority;
            requestRefresh();
        }
    }

    public void setHiddenSsid(boolean hiddenSsid) {
        if (this.hiddenSsid != hiddenSsid) {
            this.hiddenSsid = hiddenSsid;
            requestRefresh();
        }
    }

    public void setLinkSpeed(int linkSpeed) {
        if (this.linkSpeed != linkSpeed) {
            this.linkSpeed = linkSpeed;
            requestRefresh();
        }
    }
    
    public void setIpAddress(int address) {
        if (ipAddress != address) {
            ipAddress = address;
            requestRefresh();
        }
    }
    
    public void setConfigured(boolean configured) {
        if (this.configured != configured) {
            this.configured = configured;
            requestRefresh();
        }
    }
    
    public void setStatus(NetworkInfo.DetailedState status) {
        if (this.status != status) {
            this.status = status;
            requestRefresh();
        }
    }
    
    public void setSecurity(String security) {
        if (TextUtils.isEmpty(this.security) || !this.security.equals(security)) {
            this.security = security;
            requestRefresh();
        }
    }

    public boolean hasSecurity() {
        return security != null && !security.contains(OPEN);
    }
    
    public String getHumanReadableSecurity() {
        if (security.equals(OPEN)) return mContext.getString(R.string.wifi_security_open);
        else if (security.equals(WEP)) return mContext.getString(R.string.wifi_security_wep);
        else if (security.equals(WPA)) return mContext.getString(R.string.wifi_security_wpa);
        else if (security.equals(WPA2)) return mContext.getString(R.string.wifi_security_wpa2);
        
        return mContext.getString(R.string.wifi_security_unknown);
    }
    
    public void updateFromScanResult(ScanResult scanResult) {
        blockRefresh();
        
        // We don't keep specific AP BSSIDs and instead leave that as wildcard
        
        setSeen(true);
        setSsid(scanResult.SSID);
        if (networkId == NETWORK_ID_NOT_SET) {
            // Since ScanResults don't cross-reference network ID, we set it as a wildcard
            setNetworkId(NETWORK_ID_ANY);
        }
        setSignal(scanResult.level);
        setSecurity(getScanResultSecurity(scanResult));
        unblockRefresh();
    }
    
    /**
     * @return The security of a given {@link ScanResult}.
     */
    public static String getScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = { WEP, WPA, WPA2 }; 
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }
        
        return OPEN;
    }
    
    /**
     * @return Whether the given ScanResult represents an adhoc network.
     */
    public static boolean isAdhoc(ScanResult scanResult) {
        return scanResult.capabilities.contains(ADHOC_CAPABILITY);
    }
    
    /**
     * @return Whether the given ScanResult has enterprise security.
     */
    public static boolean isEnterprise(ScanResult scanResult) {
        return scanResult.capabilities.contains(ENTERPRISE_CAPABILITY);
    }
    
    public void updateFromWifiConfiguration(WifiConfiguration wifiConfig) {
        if (wifiConfig != null) {
            blockRefresh();
            setBssid(wifiConfig.BSSID);
            setNetworkId(wifiConfig.networkId);
            setPriority(wifiConfig.priority);
            setHiddenSsid(wifiConfig.hiddenSSID);
            setSsid(wifiConfig.SSID);
            setConfigured(true);
            setDisabled(wifiConfig.status == WifiConfiguration.Status.DISABLED);
            parseWifiConfigurationSecurity(wifiConfig);
            unblockRefresh();
        }
    }
    
    public void setPassword(String password) {
        setPassword(password, WEP_PASSWORD_AUTO);
    }
    
    public void setPassword(String password, int wepPasswordType) {
        mPassword = password;
        mWepPasswordType = wepPasswordType;
    }
    
    public boolean hasPassword() {
        return !TextUtils.isEmpty(mPassword) || mConfigHadPassword; 
    }
    
    private static boolean hasPassword(WifiConfiguration wifiConfig) {
        return !TextUtils.isEmpty(wifiConfig.preSharedKey)
                || !TextUtils.isEmpty(wifiConfig.wepKeys[0])
                || !TextUtils.isEmpty(wifiConfig.wepKeys[1])
                || !TextUtils.isEmpty(wifiConfig.wepKeys[2])
                || !TextUtils.isEmpty(wifiConfig.wepKeys[3]);        
    }
    
    private void parseWifiConfigurationSecurity(WifiConfiguration wifiConfig) {
        setSecurity(getWifiConfigurationSecurity(wifiConfig));
        mConfigHadPassword = hasPassword(wifiConfig);
    }
    
    /**
     * @return The security of a given {@link WifiConfiguration}.
     */
    public static String getWifiConfigurationSecurity(WifiConfiguration wifiConfig) {

        if (wifiConfig.allowedKeyManagement.get(KeyMgmt.NONE)) {
            // If we never set group ciphers, wpa_supplicant puts all of them.
            // For open, we don't set group ciphers.
            // For WEP, we specifically only set WEP40 and WEP104, so CCMP
            // and TKIP should not be there.
            if (!wifiConfig.allowedGroupCiphers.get(GroupCipher.CCMP)
                    && (wifiConfig.allowedGroupCiphers.get(GroupCipher.WEP40)
                            || wifiConfig.allowedGroupCiphers.get(GroupCipher.WEP104))) {
                return WEP;
            } else {
                return OPEN;
            }
        } else if (wifiConfig.allowedProtocols.get(Protocol.RSN)) {
            return WPA2;
        } else if (wifiConfig.allowedProtocols.get(Protocol.WPA)) {
            return WPA;
        } else {
            Log.w(TAG, "Unknown security type from WifiConfiguration, falling back on open.");
            return OPEN;
        }
    }
    
    public void updateFromWifiInfo(WifiInfo wifiInfo, NetworkInfo.DetailedState state) {
        if (wifiInfo != null) {
            blockRefresh();
            setBssid(wifiInfo.getBSSID());
            setLinkSpeed(wifiInfo.getLinkSpeed());
            setNetworkId(wifiInfo.getNetworkId());
            setIpAddress(wifiInfo.getIpAddress());
            setSsid(wifiInfo.getSSID());
            if (state != null) {
                setStatus(state);
            }
            setHiddenSsid(wifiInfo.getHiddenSSID());
            unblockRefresh();
        }
    }
    
    /**
     * @return Whether this AP can be connected to at the moment.
     */
    public boolean isConnectable() {
        return !primary && seen;
    }

    /**
     * @return Whether this AP can be forgotten at the moment.
     */
    public boolean isForgetable() {
        return configured;
    }
    
    /**
     * Updates the state as if it were never configured.
     * <p>
     * Note: This will not pass the forget call to the Wi-Fi API.
     */
    public void forget() {
        blockRefresh();
        setConfigured(false);
        setNetworkId(NETWORK_ID_NOT_SET);
        setPrimary(false);
        setStatus(null);
        setDisabled(false);
        unblockRefresh();
    }
    
    public void updateWifiConfiguration(WifiConfiguration config) {
        config.BSSID = getWpaSupplicantBssid();
        config.priority = priority;
        config.hiddenSSID = hiddenSsid;
        config.SSID = convertToQuotedString(ssid);
        
        setupSecurity(config);
    }
    
    private void setupSecurity(WifiConfiguration config) {
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        
        if (TextUtils.isEmpty(security)) {
            security = OPEN;
            Log.w(TAG, "Empty security, assuming open");
        }
        
        if (security.equals(WEP)) {
            
            // If password is empty, it should be left untouched
            if (!TextUtils.isEmpty(mPassword)) {
                if (mWepPasswordType == WEP_PASSWORD_AUTO) {
                    if (isHexWepKey(mPassword)) {
                        config.wepKeys[0] = mPassword;
                    } else {
                        config.wepKeys[0] = convertToQuotedString(mPassword);
                    }
                } else {
                    config.wepKeys[0] = mWepPasswordType == WEP_PASSWORD_ASCII
                            ? convertToQuotedString(mPassword)
                            : mPassword;
                }
            }
            
            config.wepTxKeyIndex = 0;
            
            config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);

            config.allowedKeyManagement.set(KeyMgmt.NONE);
            
            config.allowedGroupCiphers.set(GroupCipher.WEP40);
            config.allowedGroupCiphers.set(GroupCipher.WEP104);
            
        } else if (security.equals(WPA) || security.equals(WPA2)){
            config.allowedGroupCiphers.set(GroupCipher.TKIP);
            config.allowedGroupCiphers.set(GroupCipher.CCMP);
            
            config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
            
            config.allowedPairwiseCiphers.set(PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(PairwiseCipher.TKIP);

            config.allowedProtocols.set(security.equals(WPA2) ? Protocol.RSN : Protocol.WPA);
            
            // If password is empty, it should be left untouched
            if (!TextUtils.isEmpty(mPassword)) {
                if (mPassword.length() == 64 && isHex(mPassword)) {
                    // Goes unquoted as hex
                    config.preSharedKey = mPassword;
                } else {
                    // Goes quoted as ASCII
                    config.preSharedKey = convertToQuotedString(mPassword);
                }
            }
            
        } else if (security.equals(OPEN)) {
            config.allowedKeyManagement.set(KeyMgmt.NONE);
        }
    }
    
    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();
        
        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }
        
        return isHex(wepKey);
    }
    
    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }
        
        return true;
    }
    
    public void setCallback(AccessPointStateCallback callback) {
        mCallback = callback;
    }
    
    void blockRefresh() {
        mBlockRefresh++;
    }
    
    void unblockRefresh() {
        if (--mBlockRefresh == 0 && mNeedsRefresh) {
            requestRefresh();
        }
    }
    
    private void requestRefresh() {
        if (mBlockRefresh > 0) {
            mNeedsRefresh = true;
            return;
        }
        
        if (mCallback != null) {
            mCallback.refreshAccessPointState();
        }
        
        mNeedsRefresh = false;
    }
    
    /**
     * {@inheritDoc}
     * @see #hashCode()
     * @see #equals(Object)
     */
    public int matches(int otherNetworkId, String otherBssid, String otherSsid,
            String otherSecurity) {
        
        // Whenever this method is touched, please ensure #equals and #hashCode
        // still work with the changes here!
        
        if (otherSsid == null) {
            if (WifiLayer.LOGV) {
                Log.w(TAG, "BSSID: " + otherBssid + ", SSID: " + otherSsid);
            }
            return MATCH_NONE;
        }

        /*
         * If we both have 'security' set, it must match (an open network still
         * has 'security' set to OPEN)
         */
        if (security != null && otherSecurity != null) {
            if (!security.equals(otherSecurity)) {
                return MATCH_NONE;
            }
        }
        
        // WifiConfiguration gives an empty bssid as a BSSID wildcard
        if (TextUtils.isEmpty(otherBssid)) {
            otherBssid = AccessPointState.BSSID_ANY;
        }

        final boolean networkIdMatches = networkId == otherNetworkId;
        if (!networkIdMatches && networkId != NETWORK_ID_ANY && otherNetworkId != NETWORK_ID_ANY) {
            // Network IDs don't match (e.g., 1 & 2 or unset & 1) and neither is a wildcard
            return MATCH_NONE;
        }
        
        if (networkIdMatches && otherNetworkId != NETWORK_ID_NOT_SET
                && otherNetworkId != NETWORK_ID_ANY) {
            // Network ID matches (they're set to the same ID)
            return MATCH_EXACT;
        }
        
        // So now, network IDs aren't set or at least one is a wildcard 
        
        final boolean bssidMatches = bssid.equals(otherBssid);
        final boolean otherBssidIsWildcard = otherBssid.equals(BSSID_ANY);
        if (bssidMatches && !otherBssidIsWildcard) {
            // BSSID matches (and neither is a wildcard)
            return MATCH_STRONG;
        }

        if (!bssidMatches && !bssid.equals(BSSID_ANY) && !otherBssidIsWildcard) {
            // BSSIDs don't match (e.g., 00:24:21:21:42:12 & 42:12:44:21:22:52)
            // and neither is a wildcard
            return MATCH_NONE;
        }
        
        // So now, BSSIDs are both wildcards
        
        final boolean ssidMatches = ssid.equals(otherSsid); 
        if (ssidMatches) {
            // SSID matches
            return MATCH_WEAK;
        }

        return MATCH_NONE;
    }
    
    /**
     * {@inheritDoc}
     * @see #matches(int, String, String)
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        // Two equal() objects must have same hashCode.
        // With Wi-Fi, the broadest match is if two SSIDs are the same.  The finer-grained matches
        // imply this (for example, the same network IDs means the same WifiConfiguration which
        // means the same SSID).
        // See #matches for the exact matching algorithm we use.
        return ssid != null ? ssid.hashCode() : 0;
    }

    /**
     * {@inheritDoc}
     * @see #matches(int, String, String)
     * @see #hashCode()
     */
    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(getClass())) {
            return false;
        }
        
        final AccessPointState other = (AccessPointState) o;

        // To see which conditions cause two AccessPointStates to be equal, see
        // where #matches returns MATCH_WEAK or greater.
        
        return matches(other.networkId, other.bssid, other.ssid, other.security) >= MATCH_WEAK;
    }

    public int matchesWifiConfiguration(WifiConfiguration wifiConfig) {
        String security = getWifiConfigurationSecurity(wifiConfig);
        return matches(wifiConfig.networkId, wifiConfig.BSSID, wifiConfig.SSID, security);
    }
    
    String getSummarizedStatus() {
        StringBuilder sb = mSummaryBuilder;
        sb.delete(0, sb.length());
        
        if (primary && status != null) {
            buildSummary(sb, WifiStatus.getPrintable(mContext, status), true);
            
        } else if (!seen) {
            buildSummary(sb, mContext.getString(R.string.summary_not_in_range), true);

            // Remembered comes second in this case
            if (!primary && configured) {
                buildSummary(sb, mContext.getString(R.string.summary_remembered), true);
            }
            
        } else {
            if (configured && disabled) {
                // The connection failure overrides all in this case
                return mContext.getString(R.string.summary_connection_failed);
            }

            // Remembered comes first in this case
            if (!primary && configured) {
                buildSummary(sb, mContext.getString(R.string.summary_remembered), true);
            }
            
            // If it is seen (and not the primary), show the security type
            String verboseSecurity = getVerboseSecurity();
            if (verboseSecurity != null) {
                buildSummary(sb, verboseSecurity, true);
            }
        }
        
        return sb.toString();
    }
    
    private String getVerboseSecurity() {
        if (WEP.equals(security)) {
            return mContext.getString(R.string.wifi_security_verbose_wep);
        } else if (WPA.equals(security)) {
            return mContext.getString(R.string.wifi_security_verbose_wpa);
        } else if (WPA2.equals(security)) {
            return mContext.getString(R.string.wifi_security_verbose_wpa2);
        } else if (OPEN.equals(security)) {
            return mContext.getString(R.string.wifi_security_verbose_open);
        } else {
            return null;
        }
    }
    
    private void buildSummary(StringBuilder sb, String string, boolean autoLowerCaseFirstLetter) {
        if (sb.length() == 0) {
            sb.append(string);
        } else {
            sb.append(", ");
            if (autoLowerCaseFirstLetter) {
                // Convert first letter to lowercase
                sb.append(Character.toLowerCase(string.charAt(0))).append(string, 1,
                        string.length());
            } else {
                sb.append(string);
            }
        }
    }
    
    public int compareTo(AccessPointState other) {
        // This ranks the states for displaying in the AP list, not for
        // connecting to (wpa_supplicant does that using the WifiConfiguration's
        // priority field).
        
        // Clarity > efficiency, of this logic:
        int comparison;
        
        // Primary
        comparison = (other.primary ? 1 : 0) - (primary ? 1 : 0);
        if (comparison != 0) return comparison;
        
        // Currently seen (similar to, but not always the same as within range)
        comparison = (other.seen ? 1 : 0) - (seen ? 1 : 0);
        if (comparison != 0) return comparison;

        // Configured
        comparison = (other.configured ? 1 : 0) - (configured ? 1 : 0);
        if (comparison != 0) return comparison;

        if (!configured) {
            // Neither are configured

            // Open network
            comparison = (hasSecurity() ? 1 : 0) - (other.hasSecurity() ? 1 : 0);
            if (comparison != 0) return comparison;
        }

        // Signal strength
        comparison = (int) (other.signalForSorting - signalForSorting);
        if (comparison != 0) return comparison;

        // Alphabetical
        return ssid.compareToIgnoreCase(other.ssid);
    }

    public String toString() {
        return ssid + " (" + bssid + ", " + networkId + ", " + super.toString() + ")";
    }
    
    /** Implement the Parcelable interface */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bssid);
        dest.writeInt(configured ? 1 : 0);
        dest.writeInt(ipAddress);
        dest.writeInt(linkSpeed);
        dest.writeInt(networkId);
        dest.writeInt(primary ? 1 : 0);
        dest.writeInt(priority);
        dest.writeInt(hiddenSsid ? 1 : 0);
        dest.writeString(security);
        dest.writeInt(seen ? 1 : 0);
        dest.writeInt(disabled ? 1 : 0);
        dest.writeInt(signal);
        dest.writeString(ssid);
        dest.writeString(status != null ? status.toString() : null);
        dest.writeString(mPassword);
        dest.writeInt(mConfigHadPassword ? 1 : 0);
        dest.writeInt(mWepPasswordType);
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    public static final Creator<AccessPointState> CREATOR =
        new Creator<AccessPointState>() {
            public AccessPointState createFromParcel(Parcel in) {
                AccessPointState state = new AccessPointState();
                state.bssid = in.readString();
                state.configured = in.readInt() == 1;
                state.ipAddress = in.readInt();
                state.linkSpeed = in.readInt();
                state.networkId = in.readInt();
                state.primary = in.readInt() == 1;
                state.priority = in.readInt();
                state.hiddenSsid = in.readInt() == 1;
                state.security = in.readString();
                state.seen = in.readInt() == 1;
                state.disabled = in.readInt() == 1;
                state.signal = in.readInt();
                state.ssid = in.readString();
                String statusStr = in.readString();
                if (statusStr != null) {
                    state.status = NetworkInfo.DetailedState.valueOf(statusStr);
                }
                state.mPassword = in.readString();
                state.mConfigHadPassword = in.readInt() == 1;
                state.mWepPasswordType = in.readInt();
                return state;
            }

            public AccessPointState[] newArray(int size) {
                return new AccessPointState[size];
            }
        };

        
}
