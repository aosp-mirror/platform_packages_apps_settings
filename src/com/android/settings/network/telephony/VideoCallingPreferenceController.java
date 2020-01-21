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
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.ims.ImsManager;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.ims.VolteQueryImsState;
import com.android.settings.network.ims.VtQueryImsState;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Video Calling"
 */
public class VideoCallingPreferenceController extends TelephonyTogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop,
        MobileDataEnabledListener.Client,
        Enhanced4gBasePreferenceController.On4gLteUpdateListener {

    private static final String TAG = "VideoCallingPreference";

    private Preference mPreference;
    private CarrierConfigManager mCarrierConfigManager;
    @VisibleForTesting
    ImsManager mImsManager;
    private PhoneCallStateListener mPhoneStateListener;
    @VisibleForTesting
    Integer mCallState;
    private MobileDataEnabledListener mDataContentObserver;

    public VideoCallingPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mDataContentObserver = new MobileDataEnabledListener(context, this);
        mPhoneStateListener = new PhoneCallStateListener();
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return SubscriptionManager.isValidSubscriptionId(subId)
                && isVideoCallEnabled(subId)
                ? AVAILABLE
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
        mDataContentObserver.start(mSubId);
    }

    @Override
    public void onStop() {
        mPhoneStateListener.unregister();
        mDataContentObserver.stop();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mCallState == null) {
            return;
        }
        final SwitchPreference switchPreference = (SwitchPreference) preference;
        final boolean videoCallEnabled = isVideoCallEnabled(mSubId, mImsManager);
        switchPreference.setVisible(videoCallEnabled);
        if (videoCallEnabled) {
            final boolean videoCallEditable = queryVoLteState(mSubId).isEnabledByUser()
                    && mImsManager.isNonTtyOrTtyOnVolteEnabled();
            preference.setEnabled(videoCallEditable
                    && mCallState == TelephonyManager.CALL_STATE_IDLE);
            switchPreference.setChecked(videoCallEditable && isChecked());
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        final ImsMmTelManager imsMmTelManager = ImsMmTelManager.createForSubscriptionId(mSubId);
        if (imsMmTelManager == null) {
            return false;
        }
        try {
            imsMmTelManager.setVtSettingEnabled(isChecked);
            return true;
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Unable to set VT status " + isChecked + ". subId=" + mSubId,
                    exception);
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        return queryImsState(mSubId).isEnabledByUser();
    }

    public VideoCallingPreferenceController init(int subId) {
        mSubId = subId;
        if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
            mImsManager = ImsManager.getInstance(mContext,
                    SubscriptionUtil.getPhoneId(mContext, mSubId));
        }

        return this;
    }

    private boolean isVideoCallEnabled(int subId) {
        final ImsManager imsManager = SubscriptionManager.isValidSubscriptionId(subId)
                ? ImsManager.getInstance(mContext, SubscriptionUtil.getPhoneId(mContext, subId))
                : null;
        return isVideoCallEnabled(subId, imsManager);
    }

    @VisibleForTesting
    ProvisioningManager getProvisioningManager(int subId) {
        return ProvisioningManager.createForSubscriptionId(subId);
    }

    private boolean isVtProvisionedOnDevice(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return true;
        }
        final ProvisioningManager provisioningMgr = getProvisioningManager(subId);
        if (provisioningMgr == null) {
            return true;
        }
        return provisioningMgr.getProvisioningStatusForCapability(
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
    }

    @VisibleForTesting
    boolean isVideoCallEnabled(int subId, ImsManager imsManager) {
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            telephonyManager = telephonyManager.createForSubscriptionId(subId);
        }
        return carrierConfig != null && imsManager != null
                && imsManager.isVtEnabledByPlatform()
                && isVtProvisionedOnDevice(subId)
                && MobileNetworkUtils.isImsServiceStateReady(imsManager)
                && (carrierConfig.getBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS)
                || telephonyManager.isDataEnabled());
    }

    @Override
    public void on4gLteUpdated() {
        updateState(mPreference);
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
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
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
     * Implementation of MobileDataEnabledListener.Client
     */
    public void onMobileDataEnabledChange() {
        updateState(mPreference);
    }

    @VisibleForTesting
    VtQueryImsState queryImsState(int subId) {
        return new VtQueryImsState(mContext, subId);
    }

    @VisibleForTesting
    VolteQueryImsState queryVoLteState(int subId) {
        return new VolteQueryImsState(mContext, subId);
    }
}
