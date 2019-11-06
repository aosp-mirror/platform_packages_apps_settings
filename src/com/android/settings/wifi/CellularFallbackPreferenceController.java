/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.telephony.SubscriptionManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.TogglePreferenceController;

/**
 * CellularFallbackPreferenceController controls whether we should fall back to celluar when
 * wifi is bad.
 */
public class CellularFallbackPreferenceController extends TogglePreferenceController {

    public CellularFallbackPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return avoidBadWifiConfig() ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return avoidBadWifiCurrentSettings();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        // On: avoid bad wifi. Off: prompt.
        return Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.NETWORK_AVOID_BAD_WIFI, isChecked ? "1" : null);
    }

    private boolean avoidBadWifiConfig() {
        final int activeDataSubscriptionId = getActiveDataSubscriptionId();
        if (activeDataSubscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return true;
        }

        final Resources resources = getResourcesForSubId(activeDataSubscriptionId);
        return resources.getInteger(com.android.internal.R.integer.config_networkAvoidBadWifi) == 1;
    }

    @VisibleForTesting
    int getActiveDataSubscriptionId() {
        return SubscriptionManager.getActiveDataSubscriptionId();
    }

    @VisibleForTesting
    Resources getResourcesForSubId(int subscriptionId) {
        return SubscriptionManager.getResourcesForSubId(mContext, subscriptionId,
                false /* useRootLocale */);
    }

    private boolean avoidBadWifiCurrentSettings() {
        return "1".equals(Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.NETWORK_AVOID_BAD_WIFI));
    }
}
