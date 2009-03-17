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
import android.text.TextUtils;

public class WifiStatus {
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
        
        switch (detailedState) {
            case AUTHENTICATING:
                return context.getString(R.string.status_authenticating);
            case CONNECTED:
                return context.getString(R.string.status_connected);
            case CONNECTING:
                return context.getString(R.string.status_connecting);
            case DISCONNECTED:
                return context.getString(R.string.status_disconnected);
            case DISCONNECTING:
                return context.getString(R.string.status_disconnecting);
            case FAILED:
                return context.getString(R.string.status_failed);
            case OBTAINING_IPADDR:
                return context.getString(R.string.status_obtaining_ip);
            case SCANNING:
                return context.getString(R.string.status_scanning);
            default:
                return null;
        }
    }
    
    public static String getPrintableFragment(Context context,
            NetworkInfo.DetailedState detailedState, String apName) {
        
        String fragment = null;
        switch (detailedState) {
            case AUTHENTICATING:
                fragment = context.getString(R.string.fragment_status_authenticating);
                break;
            case CONNECTED:
                fragment = context.getString(R.string.fragment_status_connected);
                break;
            case CONNECTING:
                fragment = context.getString(R.string.fragment_status_connecting);
                break;
            case DISCONNECTED:
                fragment = context.getString(R.string.fragment_status_disconnected);
                break;
            case DISCONNECTING:
                fragment = context.getString(R.string.fragment_status_disconnecting);
                break;
            case FAILED:
                fragment = context.getString(R.string.fragment_status_failed);
                break;
            case OBTAINING_IPADDR:
                fragment = context.getString(R.string.fragment_status_obtaining_ip);
                break;
            case SCANNING:
                fragment = context.getString(R.string.fragment_status_scanning);
                break;
        }
        
        return String.format(fragment, apName);
    }
    
}
