
package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

/**
 * This receiver catches when quick settings turns off the hotspot, so we can
 * cancel the alarm in that case.  All other cancels are handled in tethersettings.
 */
public class HotspotOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_DISABLED) {
                // The hotspot has been turned off, we don't need to recheck tethering.
                TetherService.cancelRecheckAlarmIfNecessary(context, TetherSettings.WIFI_TETHERING);
            }
        }
    }
}
