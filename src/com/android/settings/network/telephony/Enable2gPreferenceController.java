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
package com.android.settings.network.telephony;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Preference controller for "Enable 2G"
 */
public class Enable2gPreferenceController extends TelephonyTogglePreferenceController {

    private static final String LOG_TAG = "Enable2gPreferenceController";
    private static final long BITMASK_2G =  TelephonyManager.NETWORK_TYPE_BITMASK_GSM
                | TelephonyManager.NETWORK_TYPE_BITMASK_GPRS
                | TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
                | TelephonyManager.NETWORK_TYPE_BITMASK_CDMA
                | TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private CarrierConfigCache mCarrierConfigCache;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    /**
     * Class constructor of "Enable 2G" toggle.
     *
     * @param context of settings
     * @param key assigned within UI entry of XML file
     */
    public Enable2gPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
    }

    /**
     * Initialization based on a given subscription id.
     *
     * @param subId is the subscription id
     * @return this instance after initialization
     */
    public Enable2gPreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
              .createForSubscriptionId(mSubId);
        return this;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null || !SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            return;
        }
        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(mSubId);
        boolean isDisabledByCarrier =
                carrierConfig != null
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_ENABLE_2G);
        preference.setEnabled(!isDisabledByCarrier);
        String summary;
        if (isDisabledByCarrier) {
            summary = mContext.getString(R.string.enable_2g_summary_disabled_carrier,
                getCarrierName());
        } else {
            summary = mContext.getString(R.string.enable_2g_summary);
        }
        preference.setSummary(summary);
    }

    private String getCarrierName() {
        SubscriptionInfo subInfo = SubscriptionUtil.getSubById(mSubscriptionManager, mSubId);
        if (subInfo == null) {
            return "";
        }
        CharSequence carrierName = subInfo.getCarrierName();
        return TextUtils.isEmpty(carrierName) ? "" : carrierName.toString();
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(subId);
        if (mTelephonyManager == null) {
            Log.w(LOG_TAG, "Telephony manager not yet initialized");
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }
        boolean visible =
                SubscriptionManager.isUsableSubscriptionId(subId)
                && carrierConfig != null
                && mTelephonyManager.isRadioInterfaceCapabilitySupported(
                    mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
        return visible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        long currentlyAllowedNetworkTypes = mTelephonyManager.getAllowedNetworkTypesForReason(
                mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G);
        return (currentlyAllowedNetworkTypes & BITMASK_2G) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            return false;
        }
        long currentlyAllowedNetworkTypes = mTelephonyManager.getAllowedNetworkTypesForReason(
                mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G);
        boolean enabled = (currentlyAllowedNetworkTypes & BITMASK_2G) != 0;
        if (enabled == isChecked) {
            return false;
        }
        long newAllowedNetworkTypes = currentlyAllowedNetworkTypes;
        if (isChecked) {
            newAllowedNetworkTypes = currentlyAllowedNetworkTypes | BITMASK_2G;
            Log.i(LOG_TAG, "Enabling 2g. Allowed network types: " + newAllowedNetworkTypes);
        } else {
            newAllowedNetworkTypes = currentlyAllowedNetworkTypes & ~BITMASK_2G;
            Log.i(LOG_TAG, "Disabling 2g. Allowed network types: " + newAllowedNetworkTypes);
        }
        mTelephonyManager.setAllowedNetworkTypesForReason(
                mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G, newAllowedNetworkTypes);
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_2G_ENABLED, isChecked);
        return true;
    }
}
