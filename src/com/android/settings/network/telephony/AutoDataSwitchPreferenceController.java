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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.flags.Flags;
import com.android.settings.network.MobileDataContentObserver;
import com.android.settings.network.ProxySubscriptionManager;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Controls whether switch mobile data to the non-default SIM if the non-default SIM has better
 * availability.
 *
 * This is used for temporarily allowing data on the non-default data SIM when on-default SIM
 * has better availability on DSDS devices, where better availability means strong
 * signal/connectivity.
 * If this feature is enabled, data will be temporarily enabled on the non-default data SIM,
 * including during any voice calls.
 */
public class AutoDataSwitchPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String LOG_TAG = "AutoDataSwitchPrefCtrl";

    private TwoStatePreference mPreference;
    private SubscriptionsChangeListener mChangeListener;
    private TelephonyManager mManager;
    private MobileDataContentObserver mMobileDataContentObserver;
    private PreferenceScreen mScreen;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public AutoDataSwitchPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    void init(int subId) {
        this.mSubId = subId;
        mManager = mContext.getSystemService(TelephonyManager.class).createForSubscriptionId(subId);
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        if (mChangeListener == null) {
            mChangeListener = new SubscriptionsChangeListener(mContext, this);
        }
        mChangeListener.start();
        if (mMobileDataContentObserver == null) {
            mMobileDataContentObserver = new MobileDataContentObserver(
                    new Handler(Looper.getMainLooper()));
            mMobileDataContentObserver.setOnMobileDataChangedListener(() -> refreshPreference());
        }
        mMobileDataContentObserver.register(mContext, mSubId);
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

    private int getOtherSubId(@NonNull int[] subIds) {
        if (subIds.length > 1) {
            for (int subId : subIds) {
                if (subId != mSubId) {
                    return subId;
                }
            }
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    private boolean isEnabled(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return false;
        }
        TelephonyManager telephonyManager = mContext.getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        return telephonyManager != null && telephonyManager.isMobileDataPolicyEnabled(
                        TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mManager.setMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
                isChecked);
        if (mContext.getResources().getBoolean(
                R.bool.config_auto_data_switch_enables_cross_sim_calling)) {
            trySetCrossSimCalling(mContext, getActiveSubscriptionIdList(), isChecked /* enabled */);
        }
        return true;
    }

    @VisibleForTesting
    protected boolean hasMobileData() {
        return DataUsageUtils.hasMobileData(mContext);
    }

    private boolean isCrossSimCallingAllowedByPlatform(Context context, int subId) {
        if ((new WifiCallingQueryImsState(context, subId)).isWifiCallingSupported()) {
            PersistableBundle bundle = getCarrierConfigForSubId(subId);
            return (bundle != null) && bundle.getBoolean(
                    CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                    false /*default*/);
        }
        return false;
    }

    protected ImsMmTelManager getImsMmTelManager(Context context, int subId) {
        ImsManager imsMgr = context.getSystemService(ImsManager.class);
        return (imsMgr == null) ? null : imsMgr.getImsMmTelManager(subId);
    }

    private void trySetCrossSimCallingPerSub(Context context, int subId, boolean enabled) {
        try {
            getImsMmTelManager(context, subId).setCrossSimCallingEnabled(enabled);
        } catch (ImsException | IllegalArgumentException | NullPointerException exception) {
            Log.w(LOG_TAG, "failed to change cross SIM calling configuration to " + enabled
                    + " for subID " + subId + "with exception: ", exception);
        }
    }

    private void trySetCrossSimCalling(Context context, int[] subIds, boolean enabled) {
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_UPDATE_CROSS_SIM_CALLING_ON_AUTO_DATA_SWITCH_EVENT, enabled);
        for (int subId : subIds) {
            if (isCrossSimCallingAllowedByPlatform(context, subId)) {
                trySetCrossSimCallingPerSub(context, subId, enabled);
            }
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (Flags.isDualSimOnboardingEnabled()
                || !SubscriptionManager.isValidSubscriptionId(subId)
                || SubscriptionManager.getDefaultDataSubscriptionId() == subId
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
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {}

    @Override
    public void onSubscriptionsChanged() {
        updateState(mPreference);
    }

    private int[] getActiveSubscriptionIdList() {
        return ProxySubscriptionManager.getInstance(mContext).getActiveSubscriptionIdList();
    }

    /**
     * Trigger displaying preference when Mobile data content changed.
     */
    @VisibleForTesting
    public void refreshPreference() {
        if (mContext.getResources().getBoolean(
                R.bool.config_auto_data_switch_enables_cross_sim_calling)) {
            int[] subIds = getActiveSubscriptionIdList();
            trySetCrossSimCalling(mContext, subIds, isEnabled(getOtherSubId(subIds)));
        }
        if (mScreen != null) {
            super.displayPreference(mScreen);
        }
    }
}
