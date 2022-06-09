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

package com.android.settings.network.telephony.gsm;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyBasePreferenceController;

/**
 * Preference controller for "Open network select"
 */
public class OpenNetworkSelectPagePreferenceController extends
        TelephonyBasePreferenceController implements
        AutoSelectPreferenceController.OnNetworkSelectModeListener, LifecycleObserver {

    private TelephonyManager mTelephonyManager;
    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    private int mCacheOfModeStatus;

    public OpenNetworkSelectPagePreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mCacheOfModeStatus = TelephonyManager.NETWORK_SELECTION_MODE_UNKNOWN;
        mAllowedNetworkTypesListener = new AllowedNetworkTypesListener(
                context.getMainExecutor());
        mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(
                () -> updatePreference());

    }

    private void updatePreference() {
        if (mPreferenceScreen != null) {
            displayPreference(mPreferenceScreen);
        }
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, subId)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mAllowedNetworkTypesListener.register(mContext, mSubId);
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mAllowedNetworkTypesListener.unregister(mContext, mSubId);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(mCacheOfModeStatus
                != TelephonyManager.NETWORK_SELECTION_MODE_AUTO);

        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                "com.android.settings.Settings$NetworkSelectActivity");
        intent.putExtra(Settings.EXTRA_SUB_ID, mSubId);
        preference.setIntent(intent);
    }

    @Override
    public CharSequence getSummary() {
        final ServiceState ss = mTelephonyManager.getServiceState();
        if (ss != null && ss.getState() == ServiceState.STATE_IN_SERVICE) {
            return MobileNetworkUtils.getCurrentCarrierNameForDisplay(mContext, mSubId);
        } else {
            return mContext.getString(R.string.network_disconnected);
        }
    }

    /**
     * Initialization based on given subscription id.
     **/
    public OpenNetworkSelectPagePreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
        return this;
    }

    @Override
    public void onNetworkSelectModeUpdated(int mode) {
        mCacheOfModeStatus = mode;
        if (mPreference != null) {
            updateState(mPreference);
        }
    }
}
