package com.android.settings.wifi;

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
     * {@link WifiManager#getCurrentNetworkWpsNfcConfigurationToken}
     */
    public String getCurrentNetworkWpsNfcConfigurationToken() {
        return mWifiManager.getCurrentNetworkWpsNfcConfigurationToken();
    }
}
