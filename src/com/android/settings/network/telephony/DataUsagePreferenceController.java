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
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.datausage.lib.DataUsageLib;
import com.android.settingslib.net.DataUsageController;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Preference controller for "Data usage"
 */
public class DataUsagePreferenceController extends TelephonyBasePreferenceController {

    private static final String LOG_TAG = "DataUsagePreferCtrl";

    private Future<NetworkTemplate> mTemplateFuture;
    private AtomicReference<NetworkTemplate> mTemplate;
    private Future<Long> mHistoricalUsageLevel;

    public DataUsagePreferenceController(Context context, String key) {
        super(context, key);
        mTemplate = new AtomicReference<NetworkTemplate>();
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return (SubscriptionManager.isValidSubscriptionId(subId))
                ? AVAILABLE
                : AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        final Intent intent = new Intent(Settings.ACTION_MOBILE_DATA_USAGE);
        intent.putExtra(Settings.EXTRA_NETWORK_TEMPLATE, getNetworkTemplate());
        intent.putExtra(Settings.EXTRA_SUB_ID, mSubId);

        mContext.startActivity(intent);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            preference.setEnabled(false);
            return;
        }
        final CharSequence summary = getDataUsageSummary(mContext, mSubId);
        if (summary == null) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
            preference.setSummary(summary);
        }
    }

    public void init(int subId) {
        mSubId = subId;
        mTemplate.set(null);
        mTemplateFuture = ThreadUtils.postOnBackgroundThread(()
                -> fetchMobileTemplate(mContext, mSubId));
    }

    private NetworkTemplate fetchMobileTemplate(Context context, int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return null;
        }
        return DataUsageLib.getMobileTemplate(context, subId);
    }

    private NetworkTemplate getNetworkTemplate() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return null;
        }
        NetworkTemplate template = mTemplate.get();
        if (template != null) {
            return template;
        }
        try {
            template = mTemplateFuture.get();
            mTemplate.set(template);
        } catch (ExecutionException | InterruptedException | NullPointerException exception) {
            Log.e(LOG_TAG, "Fail to get data usage template", exception);
        }
        return template;
    }

    @VisibleForTesting
    DataUsageController.DataUsageInfo getDataUsageInfo(DataUsageController controller) {
        return controller.getDataUsageInfo(getNetworkTemplate());
    }

    private CharSequence getDataUsageSummary(Context context, int subId) {
        final DataUsageController controller = new DataUsageController(context);
        controller.setSubscriptionId(subId);

        mHistoricalUsageLevel = ThreadUtils.postOnBackgroundThread(() ->
                controller.getHistoricalUsageLevel(getNetworkTemplate()));

        final DataUsageController.DataUsageInfo usageInfo = getDataUsageInfo(controller);

        long usageLevel = usageInfo.usageLevel;
        if (usageLevel <= 0L) {
            try {
                usageLevel = mHistoricalUsageLevel.get();
            } catch (Exception exception) {
            }
        }
        if (usageLevel <= 0L) {
            return null;
        }
        return context.getString(R.string.data_usage_template,
                DataUsageUtils.formatDataUsage(context, usageLevel), usageInfo.period);
    }
}
