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
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.ims.ImsManager;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Video Calling"
 */
public class VideoCallingPreferenceController extends TelephonyTogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop,
        MobileDataEnabledListener.Client,
        Enhanced4gLtePreferenceController.On4gLteUpdateListener {

    private Preference mPreference;
    private TelephonyManager mTelephonyManager;
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
        mPhoneStateListener = new PhoneCallStateListener(Looper.getMainLooper());
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
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
        mPhoneStateListener.register(mSubId);
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
            final boolean is4gLteEnabled = mImsManager.isEnhanced4gLteModeSettingEnabledByUser()
                    && mImsManager.isNonTtyOrTtyOnVolteEnabled();
            preference.setEnabled(is4gLteEnabled &&
                    mCallState == TelephonyManager.CALL_STATE_IDLE);
            switchPreference.setChecked(is4gLteEnabled && mImsManager.isVtEnabledByUser());
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mImsManager.setVtSetting(isChecked);
        return true;
    }

    @Override
    public boolean isChecked() {
        return mImsManager.isVtEnabledByUser();
    }

    public VideoCallingPreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(mSubId);
            mImsManager = ImsManager.getInstance(mContext, SubscriptionManager.getPhoneId(mSubId));
        }

        return this;
    }

    private boolean isVideoCallEnabled(int subId) {
        final ImsManager imsManager = subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                ? ImsManager.getInstance(mContext, SubscriptionManager.getPhoneId(subId))
                : null;
        return isVideoCallEnabled(subId, imsManager);
    }

    @VisibleForTesting
    boolean isVideoCallEnabled(int subId, ImsManager imsManager) {
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            telephonyManager = telephonyManager.createForSubscriptionId(subId);
        }
        return carrierConfig != null && imsManager != null
                && imsManager.isVtEnabledByPlatform()
                && imsManager.isVtProvisionedOnDevice()
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

        public PhoneCallStateListener(Looper looper) {
            super(looper);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(int subId) {
            mSubId = subId;
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
}
