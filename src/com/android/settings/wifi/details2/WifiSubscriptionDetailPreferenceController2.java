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

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.wifitrackerlib.WifiEntry;

/**
 * {@link BasePreferenceController} that controls show the subscription detail preference item.
 * or not
 */
public class WifiSubscriptionDetailPreferenceController2 extends BasePreferenceController {

    private static final String KEY_WIFI_SUBSCRIPTION_DETAIL = "subscription_detail";
    private WifiEntry mWifiEntry;

    public WifiSubscriptionDetailPreferenceController2(Context context) {
        super(context, KEY_WIFI_SUBSCRIPTION_DETAIL);
    }

    public void setWifiEntry(WifiEntry wifiEntry) {
        mWifiEntry = wifiEntry;
    }

    @Override
    public int getAvailabilityStatus() {
        return mWifiEntry.canManageSubscription() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_WIFI_SUBSCRIPTION_DETAIL.equals(preference.getKey())) {
            mWifiEntry.manageSubscription();
            return true; /* click is handled */
        }

        return false; /* click is not handled */
    }
}
