/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.debug.IAdbManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.deviceinfo.AbstractConnectivityPreferenceController;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;

/**
 * Controller for the ip address preference in the Wireless debugging
 * fragment.
 */
public class AdbIpAddressPreferenceController extends AbstractConnectivityPreferenceController {
    private static final String TAG = "AdbIpAddrPrefCtrl";

    private static final String[] CONNECTIVITY_INTENTS = {
            ConnectivityManager.CONNECTIVITY_ACTION,
            WifiManager.ACTION_LINK_CONFIGURATION_CHANGED,
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
    };

    private static final String PREF_KEY = "adb_ip_addr_pref";
    private Preference mAdbIpAddrPref;
    private int mPort;
    private final ConnectivityManager mCM;
    private IAdbManager mAdbManager;

    public AdbIpAddressPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
        mCM = context.getSystemService(ConnectivityManager.class);
        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(
                Context.ADB_SERVICE));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mAdbIpAddrPref = screen.findPreference(PREF_KEY);
        updateConnectivity();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateConnectivity();
    }

    @Override
    protected String[] getConnectivityIntents() {
        return CONNECTIVITY_INTENTS;
    }

    protected int getPort() {
        try {
            return mAdbManager.getAdbWirelessPort();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get the adbwifi port");
        }
        return 0;
    }

    public String getIpv4Address() {
        return getDefaultIpAddresses(mCM);
    }

    @Override
    protected void updateConnectivity() {
        String ipAddress = getDefaultIpAddresses(mCM);
        if (ipAddress != null) {
            int port = getPort();
            if (port <= 0) {
                mAdbIpAddrPref.setSummary(R.string.status_unavailable);
            } else {
                ipAddress += ":" + port;
            }
            mAdbIpAddrPref.setSummary(ipAddress);
        } else {
            mAdbIpAddrPref.setSummary(R.string.status_unavailable);
        }
    }

    /**
     * Returns the default link's IP addresses, if any, taking into account IPv4 and IPv6 style
     * addresses.
     * @param cm ConnectivityManager
     * @return the formatted and newline-separated IP addresses, or null if none.
     */
    private static String getDefaultIpAddresses(ConnectivityManager cm) {
        LinkProperties prop = cm.getActiveLinkProperties();
        return formatIpAddresses(prop);
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) {
            return null;
        }

        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        // If there are no entries, return null
        if (!iter.hasNext()) {
            return null;
        }

        // Concatenate all available addresses, newline separated
        StringBuilder addresses = new StringBuilder();
        while (iter.hasNext()) {
            InetAddress addr = iter.next();
            if (addr instanceof Inet4Address) {
                // adb only supports ipv4 at the moment
                addresses.append(addr.getHostAddress());
                break;
            }
        }
        return addresses.toString();
    }
}
