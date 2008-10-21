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
import android.net.wifi.WifiInfo;
import android.text.TextUtils;

public class WifiStatus {

    // e.g., "Connecting"
    public static String sScanning;
    public static String sConnecting;
    public static String sAuthenticating;
    public static String sObtainingIp;
    public static String sConnected;
    public static String sDisconnecting;
    public static String sDisconnected;
    public static String sFailed;    

    // e.g., "Connecting to %1$s"
    public static String sScanningFragment;
    public static String sConnectingFragment;
    public static String sAuthenticatingFragment;
    public static String sObtainingIpFragment;
    public static String sConnectedFragment;
    public static String sDisconnectingFragment;
    public static String sDisconnectedFragment;
    public static String sFailedFragment;    

    private static void fillStrings(Context context) {
        sScanning = context.getString(R.string.status_scanning);
        sConnecting = context.getString(R.string.status_connecting);
        sAuthenticating = context.getString(R.string.status_authenticating);
        sObtainingIp = context.getString(R.string.status_obtaining_ip);
        sConnected = context.getString(R.string.status_connected);
        sDisconnecting = context.getString(R.string.status_disconnecting);
        sDisconnected = context.getString(R.string.status_disconnected);
        sFailed = context.getString(R.string.status_failed);

        sScanningFragment = context.getString(R.string.fragment_status_scanning);
        sConnectingFragment = context.getString(R.string.fragment_status_connecting);
        sAuthenticatingFragment = context.getString(R.string.fragment_status_authenticating);
        sObtainingIpFragment = context.getString(R.string.fragment_status_obtaining_ip);
        sConnectedFragment = context.getString(R.string.fragment_status_connected);
        sDisconnectingFragment = context.getString(R.string.fragment_status_disconnecting);
        sDisconnectedFragment = context.getString(R.string.fragment_status_disconnected);
        sFailedFragment = context.getString(R.string.fragment_status_failed);
    }
    
    public static String getStatus(Context context, String ssid,
            NetworkInfo.DetailedState detailedState) {
        
        if (!TextUtils.isEmpty(ssid) && isLiveConnection(detailedState)) {
            return getPrintableFragment(context, detailedState, ssid);
        } else {
            return getPrintable(context, detailedState);
        }
    }
    
    public static boolean isLiveConnection(NetworkInfo.DetailedState detailedState) {
        return detailedState != NetworkInfo.DetailedState.DISCONNECTED
                && detailedState != NetworkInfo.DetailedState.FAILED
                && detailedState != NetworkInfo.DetailedState.IDLE
                && detailedState != NetworkInfo.DetailedState.SCANNING;
    }
    
    public static String getPrintable(Context context,
            NetworkInfo.DetailedState detailedState) {
        
        if (sScanning == null) {
            fillStrings(context);
        }
        
        switch (detailedState) {
            case AUTHENTICATING:
                return sAuthenticating;
            case CONNECTED:
                return sConnected;
            case CONNECTING:
                return sConnecting;
            case DISCONNECTED:
                return sDisconnected;
            case DISCONNECTING:
                return sDisconnecting;
            case FAILED:
                return sFailed;
            case OBTAINING_IPADDR:
                return sObtainingIp;
            case SCANNING:
                return sScanning;
            default:
                return null;
        }
    }
    
    public static String getPrintableFragment(Context context,
            NetworkInfo.DetailedState detailedState, String apName) {
        
        if (sScanningFragment == null) {
            fillStrings(context);
        }

        String fragment = null;
        switch (detailedState) {
            case AUTHENTICATING:
                fragment = sAuthenticatingFragment;
                break;
            case CONNECTED:
                fragment = sConnectedFragment;
                break;
            case CONNECTING:
                fragment = sConnectingFragment;
                break;
            case DISCONNECTED:
                fragment = sDisconnectedFragment;
                break;
            case DISCONNECTING:
                fragment = sDisconnectingFragment;
                break;
            case FAILED:
                fragment = sFailedFragment;
                break;
            case OBTAINING_IPADDR:
                fragment = sObtainingIpFragment;
                break;
            case SCANNING:
                fragment = sScanningFragment;
                break;
        }
        
        return String.format(fragment, apName);
    }
    
}
