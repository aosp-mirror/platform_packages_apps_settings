/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.network.telephony.cdma;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.network.telephony.MobileNetworkUtils;

/**
 * Preference controller for "CDMA subscription"
 */
public class CdmaSubscriptionPreferenceController extends CdmaBasePreferenceController
        implements ListPreference.OnPreferenceChangeListener {
    private static final String TYPE_NV = "NV";
    private static final String TYPE_RUIM = "RUIM";

    @VisibleForTesting
    ListPreference mPreference;

    public CdmaSubscriptionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return MobileNetworkUtils.isCdmaOptions(mContext, subId) && deviceSupportsNvAndRuim()
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setVisible(getAvailabilityStatus() == AVAILABLE);
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE,
                TelephonyManager.CDMA_SUBSCRIPTION_RUIM_SIM);
        if (mode != TelephonyManager.CDMA_SUBSCRIPTION_UNKNOWN) {
            listPreference.setValue(Integer.toString(mode));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        final int newMode = Integer.parseInt((String) object);
        //TODO(b/117611981): only set it in one place
        if (mTelephonyManager.setCdmaSubscriptionMode(newMode)) {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.CDMA_SUBSCRIPTION_MODE, newMode);
            return true;
        }

        return false;
    }

    @VisibleForTesting
    boolean deviceSupportsNvAndRuim() {
        // retrieve the list of subscription types supported by device.
        final String subscriptionsSupported = SystemProperties.get("ril.subscription.types");
        boolean nvSupported = false;
        boolean ruimSupported = false;

        if (!TextUtils.isEmpty(subscriptionsSupported)) {
            // Searches through the comma-separated list for a match for "NV"
            // and "RUIM" to update nvSupported and ruimSupported.
            for (String subscriptionType : subscriptionsSupported.split(",")) {
                subscriptionType = subscriptionType.trim();
                if (subscriptionType.equalsIgnoreCase(TYPE_NV)) {
                    nvSupported = true;
                } else if (subscriptionType.equalsIgnoreCase(TYPE_RUIM)) {
                    ruimSupported = true;
                }
            }
        }

        return (nvSupported && ruimSupported);
    }
}
