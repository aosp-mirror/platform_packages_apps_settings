/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.datausage;

import android.app.Activity;
import android.net.NetworkTemplate;
import android.telephony.SubscriptionManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.net.DataUsageController;

/**
 * The controller displays a data usage chart for the specified Wi-Fi network.
 */
public class WifiDataUsageSummaryPreferenceController extends DataUsageSummaryPreferenceController {
    final String mNetworkId;

    public WifiDataUsageSummaryPreferenceController(Activity activity,
            Lifecycle lifecycle, PreferenceFragmentCompat fragment, CharSequence networkId) {
        super(activity, lifecycle, fragment, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        if (networkId == null) {
            mNetworkId = null;
        } else {
            mNetworkId = String.valueOf(networkId);
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }

        final DataUsageSummaryPreference mPreference = (DataUsageSummaryPreference) preference;
        // TODO(b/126299427): Currently gets data usage of whole Wi-Fi networks, but should get
        //  specified one.
        final NetworkTemplate template = NetworkTemplate.buildTemplateWifi(mNetworkId);
        final DataUsageController.DataUsageInfo info = mDataUsageController.getDataUsageInfo(
                template);
        mDataInfoController.updateDataLimit(info, mPolicyEditor.getPolicy(template));

        mPreference.setWifiMode(/* isWifiMode */ true, /* usagePeriod */
                info.period, /* isSingleWifi */ true);
        mPreference.setChartEnabled(true);
        // Treats Wi-Fi network as unlimited network, which has same usage level and limited level.
        mPreference.setUsageNumbers(info.usageLevel, info.usageLevel, /* hasMobileData */ false);

        // TODO(b/126142293): Passpoint Wi-Fi should have limit of data usage and time remaining
        mPreference.setProgress(100);
        mPreference.setLabels(DataUsageUtils.formatDataUsage(mContext, /* sizeBytes */ 0),
                DataUsageUtils.formatDataUsage(mContext, info.usageLevel));
    }
}
