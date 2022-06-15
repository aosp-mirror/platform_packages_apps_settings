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

package com.android.settings.wifi.details2;

import android.content.Context;

import com.android.settings.core.TogglePreferenceController;
import com.android.wifitrackerlib.WifiEntry;

/**
 * {@link TogglePreferenceController} that controls whether the Wi-Fi Auto-connect feature should be
 * enabled.
 */
public class WifiAutoConnectPreferenceController2 extends TogglePreferenceController {

    private static final String KEY_AUTO_CONNECT = "auto_connect";

    private WifiEntry mWifiEntry;

    public WifiAutoConnectPreferenceController2(Context context) {
        super(context, KEY_AUTO_CONNECT);
    }

    public void setWifiEntry(WifiEntry wifiEntry) {
        mWifiEntry = wifiEntry;
    }

    @Override
    public int getAvailabilityStatus() {
        return mWifiEntry.canSetAutoJoinEnabled() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return mWifiEntry.isAutoJoinEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mWifiEntry.setAutoJoinEnabled(isChecked);
        return true;
    }
}
