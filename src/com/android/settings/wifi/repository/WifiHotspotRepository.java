/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.repository;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Wi-Fi Hotspot Repository
 */
public class WifiHotspotRepository {

    protected final Context mAppContext;
    protected final WifiManager mWifiManager;

    protected String mLastPassword;
    protected LastPasswordListener mLastPasswordListener = new LastPasswordListener();

    public WifiHotspotRepository(@NonNull Context appContext, @NonNull WifiManager wifiManager) {
        mAppContext = appContext;
        mWifiManager = wifiManager;
    }

    /**
     * Query the last configured Tethered Ap Passphrase since boot.
     */
    public void queryLastPasswordIfNeeded() {
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config.getSecurityType() != SoftApConfiguration.SECURITY_TYPE_OPEN) {
            return;
        }
        mWifiManager.queryLastConfiguredTetheredApPassphraseSinceBoot(mAppContext.getMainExecutor(),
                mLastPasswordListener);
    }

    /**
     * Generate password.
     */
    public String generatePassword() {
        return !TextUtils.isEmpty(mLastPassword) ? mLastPassword : generateRandomPassword();
    }

    private class LastPasswordListener implements Consumer<String> {
        @Override
        public void accept(String password) {
            mLastPassword = password;
        }
    }

    private static String generateRandomPassword() {
        String randomUUID = UUID.randomUUID().toString();
        //first 12 chars from xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        return randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
    }
}
