/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.data.ApnSetting;
import android.util.AttributeSet;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.network.MobileDataEnabledListener;

/**
 * Preference which displays billing cycle of subscription
 */
public class BillingCyclePreference extends Preference
        implements TemplatePreference, MobileDataEnabledListener.Client {

    private NetworkTemplate mTemplate;
    private NetworkServices mServices;
    private int mSubId;
    private MobileDataEnabledListener mListener;

    /**
     * Preference constructor
     *
     * @param context Context of preference
     * @param arrts The attributes of the XML tag that is inflating the preference
     */
    public BillingCyclePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mListener = new MobileDataEnabledListener(context, this);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mListener.start(mSubId);
    }

    @Override
    public void onDetached() {
        mListener.stop();
        super.onDetached();
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId,
            NetworkServices services) {
        mTemplate = template;
        mSubId = subId;
        mServices = services;
        setSummary(null);

        setIntent(getIntent());
    }

    private void updateEnabled() {
        try {
            setEnabled(mServices.mNetworkService.isBandwidthControlEnabled()
                    && mServices.mTelephonyManager.createForSubscriptionId(mSubId)
                            .isDataEnabledForApn(ApnSetting.TYPE_DEFAULT)
                    && mServices.mUserManager.isAdminUser());
        } catch (RemoteException e) {
            setEnabled(false);
        }
    }

    @Override
    public Intent getIntent() {
        final Bundle args = new Bundle();
        args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, mTemplate);
        return new SubSettingLauncher(getContext())
                .setDestination(BillingCycleSettings.class.getName())
                .setArguments(args)
                .setTitleRes(R.string.billing_cycle)
                .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                .toIntent();
    }

    /**
     * Implementation of {@code MobileDataEnabledListener.Client}
     */
    public void onMobileDataEnabledChange() {
        updateEnabled();
    }
}
