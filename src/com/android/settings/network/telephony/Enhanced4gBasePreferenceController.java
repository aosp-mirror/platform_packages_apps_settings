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
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.ims.ImsManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Enhanced 4G LTE"
 */
public class Enhanced4gBasePreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private Preference mPreference;
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;
    @VisibleForTesting
    ImsManager mImsManager;
    private PhoneCallStateListener mPhoneStateListener;
    @VisibleForTesting
    Integer mCallState;
    private final List<On4gLteUpdateListener> m4gLteListeners;

    protected static final int MODE_NONE = -1;
    protected static final int MODE_VOLTE = 0;
    protected static final int MODE_ADVANCED_CALL = 1;
    protected static final int MODE_4G_CALLING = 2;
    private int m4gCurrentMode = MODE_NONE;

    public Enhanced4gBasePreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        m4gLteListeners = new ArrayList<>();
        mPhoneStateListener = new PhoneCallStateListener();
    }

    public Enhanced4gBasePreferenceController init(int subId) {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && mSubId == subId) {
            return this;
        }
        mSubId = subId;
        mCarrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mImsManager = ImsManager.getInstance(mContext, SubscriptionManager.getPhoneId(mSubId));
        }

        final boolean show4GForLTE = mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL);
        m4gCurrentMode = mCarrierConfig.getInt(
                CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT);
        if (m4gCurrentMode != MODE_ADVANCED_CALL) {
            m4gCurrentMode = show4GForLTE ? MODE_4G_CALLING : MODE_VOLTE;
        }
        return this;
    }

    @VisibleForTesting
    ProvisioningManager getProvisioningManager(int subId) {
        return ProvisioningManager.createForSubscriptionId(subId);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        init(subId);
        if (!isModeMatched()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
        final boolean isVisible = subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && mImsManager != null && carrierConfig != null
                && mImsManager.isVolteEnabledByPlatform()
                && isVolteProvisionedOnDevice(mSubId)
                && MobileNetworkUtils.isImsServiceStateReady(mImsManager)
                && !carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL);
        return isVisible
                ? (isPrefEnabled() ? AVAILABLE : AVAILABLE_UNSEARCHABLE)
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mPhoneStateListener.register(mContext, mSubId);
    }

    @Override
    public void onStop() {
        mPhoneStateListener.unregister();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final SwitchPreference switchPreference = (SwitchPreference) preference;

        switchPreference.setEnabled(isPrefEnabled());
        switchPreference.setChecked(mImsManager.isEnhanced4gLteModeSettingEnabledByUser()
                && mImsManager.isNonTtyOrTtyOnVolteEnabled());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mImsManager.setEnhanced4gLteModeSetting(isChecked);
        for (final On4gLteUpdateListener lsn : m4gLteListeners) {
            lsn.on4gLteUpdated();
        }
        return true;
    }

    @Override
    public boolean isChecked() {
        return mImsManager.isEnhanced4gLteModeSettingEnabledByUser();
    }

    public Enhanced4gBasePreferenceController addListener(On4gLteUpdateListener lsn) {
        m4gLteListeners.add(lsn);
        return this;
    }

    protected int getMode() {
        return MODE_NONE;
    }

    private boolean isModeMatched() {
        return m4gCurrentMode == getMode();
    }

    private boolean isPrefEnabled() {
        return mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && (mCallState != null) && (mCallState == TelephonyManager.CALL_STATE_IDLE)
                && mImsManager != null
                && mImsManager.isNonTtyOrTtyOnVolteEnabled()
                && mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL);
    }

    private boolean isVolteProvisionedOnDevice(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return true;
        }
        final ProvisioningManager provisioningMgr = getProvisioningManager(subId);
        if (provisioningMgr == null) {
            return true;
        }
        return provisioningMgr.getProvisioningStatusForCapability(
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
    }

    private class PhoneCallStateListener extends PhoneStateListener {

        PhoneCallStateListener() {
            super();
        }

        private TelephonyManager mTelephonyManager;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            mTelephonyManager = context.getSystemService(TelephonyManager.class);
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
            }
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
        }

        public void unregister() {
            mCallState = null;
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
        }
    }

    /**
     * Update other preferences when 4gLte state is changed
     */
    public interface On4gLteUpdateListener {
        void on4gLteUpdated();
    }
}
