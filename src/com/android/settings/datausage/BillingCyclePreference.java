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
import android.util.AttributeSet;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.datausage.CellDataPreference.DataStateListener;

public class BillingCyclePreference extends Preference implements TemplatePreference {

    private NetworkTemplate mTemplate;
    private NetworkServices mServices;
    private int mSubId;

    public BillingCyclePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mListener.setListener(true, mSubId, getContext());
    }

    @Override
    public void onDetached() {
        mListener.setListener(false, mSubId, getContext());
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
                    && mServices.mTelephonyManager.getDataEnabled(mSubId)
                    && mServices.mUserManager.isAdminUser());
        } catch (RemoteException e) {
            setEnabled(false);
        }
    }

    @Override
    public Intent getIntent() {
        Bundle args = new Bundle();
        args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, mTemplate);
        return new SubSettingLauncher(getContext())
                .setDestination(BillingCycleSettings.class.getName())
                .setArguments(args)
                .setTitleRes(R.string.billing_cycle)
                .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                .toIntent();
    }

    private final DataStateListener mListener = new DataStateListener() {
        @Override
        public void onChange(boolean selfChange) {
            updateEnabled();
        }
    };
}
