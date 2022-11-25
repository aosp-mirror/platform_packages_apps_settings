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

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.network.MobileDataContentObserver;
import com.android.settings.network.SubscriptionsChangeListener;

import java.util.List;

/**
 * Controls whether switch mobile data to the non-default SIM if the non-default SIM has better
 * availability.
 *
 * This is used for temporarily allowing data on the non-default data SIM when on-default SIM
 * has better availability on DSDS devices, where better availability means strong
 * signal/connectivity.
 * If this feature is enabled, data will be temporarily enabled on the non-default data SIM,
 * including during any voice calls.
 *
 * Showing this preference in the default data sim UI.
 */
public class AutoDataSwitchPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "AutoDataSwitchPreferenceController";

    private SwitchPreference mPreference;
    private SubscriptionsChangeListener mChangeListener;
    private TelephonyManager mManager;
    private MobileDataContentObserver mMobileDataContentObserver;
    private PreferenceScreen mScreen;
    private SubscriptionManager mSubscriptionManager;
    private List<SubscriptionInfo> mSubInfoList;

    public AutoDataSwitchPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
    }

    void init(int subId) {
        this.mSubId = subId;
        if (renewSubscriptionInfoList()) {
            // If the subscriptionInfos are changed, then
            mManager = mContext.getSystemService(TelephonyManager.class)
                    .createForSubscriptionId(getNonDdsSubId());
        }
        if (mMobileDataContentObserver == null) {
            mMobileDataContentObserver = new MobileDataContentObserver(
                    new Handler(Looper.getMainLooper()));
            mMobileDataContentObserver.setOnMobileDataChangedListener(() -> {
                mManager = mContext.getSystemService(TelephonyManager.class)
                        .createForSubscriptionId(getNonDdsSubId());
                refreshPreference();
            });
        }
    }

    private void renewTelephonyComponent() {
        if (renewSubscriptionInfoList()) {
            // If the subscriptionInfos are changed, then
            if (mMobileDataContentObserver != null) {
                mMobileDataContentObserver.unRegister(mContext);
            }
        }
        if (mSubInfoList == null) {
            Log.d(TAG, "mSubInfoList is null. Stop to register the listener");
            return;
        }
        if (mMobileDataContentObserver != null) {
            for (SubscriptionInfo subInfo : mSubInfoList) {
                mMobileDataContentObserver.register(mContext, subInfo.getSubscriptionId());
            }
        }
        mManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(getNonDdsSubId());
    }

    /**
     * Renew the subscriptionInfoList if the subscriptionInfos are changed.
     * @return true if the subscriptionInfos are changed. Otherwise, return false.
     */
    private boolean renewSubscriptionInfoList() {
        final List<SubscriptionInfo> newSubInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if ((newSubInfoList == null && mSubInfoList == null)
                || (mSubInfoList != null && mSubInfoList.equals(newSubInfoList))) {
            return false;
        }
        mSubInfoList = newSubInfoList;
        return true;
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        renewTelephonyComponent();
        if (mChangeListener == null) {
            mChangeListener = new SubscriptionsChangeListener(mContext, this);
        }
        mChangeListener.start();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        if (mChangeListener != null) {
            mChangeListener.stop();
        }
        if (mMobileDataContentObserver != null) {
            mMobileDataContentObserver.unRegister(mContext);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mScreen = screen;
    }

    @Override
    public boolean isChecked() {
        return mManager != null && mManager.isMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mManager == null) {
            Log.d(TAG, "mManager is null.");
            return false;
        }
        mManager.setMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
                isChecked);
        return true;
    }

    @VisibleForTesting
    protected boolean hasMobileData() {
        return DataUsageUtils.hasMobileData(mContext);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)
                || SubscriptionManager.getDefaultDataSubscriptionId() != subId
                || !SubscriptionManager.isValidSubscriptionId(getNonDdsSubId())
                || (!hasMobileData())) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        preference.setVisible(isAvailable());
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onSubscriptionsChanged() {
        renewTelephonyComponent();
        updateState(mPreference);
    }

    /**
     * Trigger displaying preference when Mobilde data content changed.
     */
    @VisibleForTesting
    public void refreshPreference() {
        if (mScreen != null) {
            super.displayPreference(mScreen);
        }
    }

    private int getNonDdsSubId() {
        int ddsSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        Log.d(TAG, "DDS SubId: " + ddsSubId);

        if (ddsSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        if (mSubInfoList == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        return mSubInfoList.stream()
                .mapToInt(subInfo -> subInfo.getSubscriptionId())
                .filter(subId -> subId != ddsSubId)
                .findFirst()
                .orElse(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }
}
