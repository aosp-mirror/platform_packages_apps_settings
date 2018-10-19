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

package com.android.settings.network.telephony;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.core.BasePreferenceController;

/**
 * Preference controller for "Data service setup"
 */
public class DataServiceSetupPreferenceController extends BasePreferenceController {

    private CarrierConfigManager mCarrierConfigManager;
    private TelephonyManager mTelephonyManager;
    private PersistableBundle mCarrierConfig;
    private String mSetupUrl;
    private int mSubId;

    public DataServiceSetupPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSetupUrl = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean isLteOnCdma = mTelephonyManager.getLteOnCdmaMode()
                == PhoneConstants.LTE_ON_CDMA_TRUE;
        return mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && mCarrierConfig != null
                && !mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)
                && isLteOnCdma && !TextUtils.isEmpty(mSetupUrl)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    public void init(int subId) {
        mSubId = subId;
        mTelephonyManager = TelephonyManager.from(mContext).createForSubscriptionId(mSubId);
        mCarrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            if (!TextUtils.isEmpty(mSetupUrl)) {
                String imsi = mTelephonyManager.getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                final String url = TextUtils.expandTemplate(mSetupUrl, imsi).toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                mContext.startActivity(intent);
            }
            return true;
        }

        return false;
    }
}
