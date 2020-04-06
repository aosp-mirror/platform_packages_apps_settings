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

package com.android.settings.network.telephony;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;

import androidx.preference.PreferenceScreen;

import com.android.settings.network.MobileDataContentObserver;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Mobile data"
 */
public class MmsMessagePreferenceController extends TelephonyTogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop {
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private MobileDataContentObserver mMobileDataContentObserver;
    private PreferenceScreen mScreen;

    public MmsMessagePreferenceController(Context context, String key) {
        super(context, key);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mMobileDataContentObserver = new MobileDataContentObserver(
                new Handler(Looper.getMainLooper()));
        mMobileDataContentObserver.setOnMobileDataChangedListener(()->refreshPreference());
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        final TelephonyManager telephonyManager = TelephonyManager
                .from(mContext).createForSubscriptionId(subId);
        return (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && !telephonyManager.isDataEnabled()
                && telephonyManager.isApnMetered(ApnSetting.TYPE_MMS))
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void onStart() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mMobileDataContentObserver.register(mContext, mSubId);
        }
    }

    @Override
    public void onStop() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mMobileDataContentObserver.unRegister(mContext);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
    }


    public void init(int subId) {
        mSubId = subId;
        mTelephonyManager = TelephonyManager.from(mContext).createForSubscriptionId(mSubId);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return mSubscriptionManager.setAlwaysAllowMmsData(mSubId, isChecked);
    }

    @Override
    public boolean isChecked() {
        return mTelephonyManager.isDataEnabledForApn(ApnSetting.TYPE_MMS);
    }

    private void refreshPreference() {
        if (mScreen != null) {
            super.displayPreference(mScreen);
        }
    }
}
