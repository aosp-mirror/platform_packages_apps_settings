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
import android.net.NetworkTemplate;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settingslib.net.DataUsageController;

/**
 * Preference controller for "Data usage"
 */
public class DataUsagePreferenceController extends TelephonyBasePreferenceController {

    private NetworkTemplate mTemplate;
    private DataUsageController.DataUsageInfo mDataUsageInfo;
    private Intent mIntent;

    public DataUsagePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                ? AVAILABLE
                : AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            mContext.startActivity(mIntent);
            return true;
        }

        return false;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            preference.setEnabled(false);
            return;
        }
        long usageLevel = mDataUsageInfo.usageLevel;
        if (usageLevel <= 0L) {
            final DataUsageController controller = new DataUsageController(mContext);
            usageLevel = controller.getHistoricalUsageLevel(mTemplate);
        }
        final boolean enabled = usageLevel > 0L;
        preference.setEnabled(enabled);

        if (enabled) {
            preference.setSummary(mContext.getString(R.string.data_usage_template,
                    DataUsageUtils.formatDataUsage(mContext, mDataUsageInfo.usageLevel),
                    mDataUsageInfo.period));
        }
    }

    public void init(int subId) {
        mSubId = subId;

        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mTemplate = DataUsageUtils.getDefaultTemplate(mContext, mSubId);

            final DataUsageController controller = new DataUsageController(mContext);
            controller.setSubscriptionId(mSubId);
            mDataUsageInfo = controller.getDataUsageInfo(mTemplate);

            mIntent = new Intent(Settings.ACTION_MOBILE_DATA_USAGE);
            mIntent.putExtra(Settings.EXTRA_NETWORK_TEMPLATE, mTemplate);
            mIntent.putExtra(Settings.EXTRA_SUB_ID, mSubId);
        }
    }
}
