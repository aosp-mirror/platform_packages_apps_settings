package com.android.settings.wrapper;

import android.net.wifi.WifiManager;

/**
 * Wrapper around {@link WifiManager} to facilitate unit testing.
 *
 * TODO: delete this class once robolectric supports Android O
 */
public class WifiManagerWrapper {
    private final WifiManager mWifiManager;

    public WifiManagerWrapper(WifiManager wifiManager) {
        mWifiManager = wifiManager;
    }

    /**
     * Gets the real WifiManager
     * @return the real WifiManager
     */
    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    /**
     * {@link WifiManager#getCurrentNetworkWpsNfcConfigurationToken}
     */
    public String getCurrentNetworkWpsNfcConfigurationToken() {
        return mWifiManager.getCurrentNetworkWpsNfcConfigurationToken();
    }

    /**
     * {@link WifiManager#removePasspointConfiguration}
     */
    public void removePasspointConfiguration(String fqdn) {
        mWifiManager.removePasspointConfiguration(fqdn);
    }

    /**
     * {@link WifiManager#removePasspointConfiguration}
     */
    public void forget(int netId, WifiManager.ActionListener listener) {
        mWifiManager.forget(netId, listener);
    }
}
