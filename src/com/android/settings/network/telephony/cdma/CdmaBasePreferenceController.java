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
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyBasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller related to CDMA category
 */
public abstract class CdmaBasePreferenceController extends TelephonyBasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    protected Preference mPreference;
    protected TelephonyManager mTelephonyManager;
    protected PreferenceManager mPreferenceManager;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;

    public CdmaBasePreferenceController(Context context, String key) {
        super(context, key);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    @Override
    public void onStart() {
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.register(mContext, mSubId);
        }
    }

    @Override
    public void onStop() {
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.unregister(mContext, mSubId);
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return MobileNetworkUtils.isCdmaOptions(mContext, subId)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    public void init(PreferenceManager preferenceManager, int subId) {
        mPreferenceManager = preferenceManager;
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);

        if (mAllowedNetworkTypesListener == null) {
            mAllowedNetworkTypesListener = new AllowedNetworkTypesListener(
                    mContext.getMainExecutor());
            mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(
                    () -> updatePreference());
        }
    }

    public void init(int subId) {
        init(null, subId);
    }

    private void updatePreference() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference instanceof CdmaListPreference) {
            ((CdmaListPreference) mPreference).setSubId(mSubId);
        }
    }
}
