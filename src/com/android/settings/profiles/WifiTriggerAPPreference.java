/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.profiles;

import android.content.Context;
import android.net.wifi.WifiConfiguration;

public class WifiTriggerAPPreference extends AbstractTriggerPreference {

    private String mSSID;
    private WifiConfiguration mConfig;

    WifiTriggerAPPreference(Context context, WifiConfiguration config) {
        super(context);
        loadConfig(config);
        setTitle(mSSID);
    }

    WifiTriggerAPPreference(Context context, String ssid) {
        super(context);
        mSSID = ssid;
        setTitle(mSSID);
    }

     private void loadConfig(WifiConfiguration config) {
        mSSID = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
        mConfig = config;
    }

    public String getSSID() {
        return mSSID;
    }

    public static String removeDoubleQuotes(String string) {
        final int length = string.length();
        if (length >= 2) {
            if (string.startsWith("\"") && string.endsWith("\"")) {
                return string.substring(1, length - 1);
            }
        }
        return string;
    }
}
