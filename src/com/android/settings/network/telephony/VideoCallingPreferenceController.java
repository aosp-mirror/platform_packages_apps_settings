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
import android.content.pm.PackageManager;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.MobileDataEnabledListener;
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
    private PhoneTelephonyCallback mTelephonyCallback;
    @VisibleForTesting
    Integer mCallState;
    private MobileDataEnabledListener mDataContentObserver;

    public VideoCallingPreferenceController(Context context, String key) {
        super(context, key);
        mDataContentObserver = new MobileDataEnabledListener(context, this);
        mTelephonyCallback = new PhoneTelephonyCallback();
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
        mTelephonyCallback.register(mContext, mSubId);
        mDataContentObserver.start(mSubId);
    }

    @Override
    public void onStop() {
        mTelephonyCallback.unregister();
        mDataContentObserver.stop();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if ((mCallState == null) || (preference == null)) {
            Log.d(TAG, "Skip update under mCallState=" + mCallState);
            return;
        }
        final TwoStatePreference switchPreference = (TwoStatePreference) preference;
        final boolean videoCallEnabled = isVideoCallEnabled(mSubId);
        switchPreference.setVisible(videoCallEnabled);
        if (videoCallEnabled) {
            final boolean videoCallEditable = queryVoLteState(mSubId).isEnabledByUser()
                    && queryImsState(mSubId).isAllowUserControl();
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

    @VisibleForTesting
    protected boolean isImsSupported() {
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_IMS);
    }

    public VideoCallingPreferenceController init(int subId) {
        mSubId = subId;

        return this;
    }

    @VisibleForTesting
    boolean isVideoCallEnabled(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }

        final PersistableBundle carrierConfig =
                CarrierConfigCache.getInstance(mContext).getConfigForSubId(subId);
        if (carrierConfig == null) {
            return false;
        }

        if (!carrierConfig.getBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS)
                && (!mContext.getSystemService(TelephonyManager.class)
                    .createForSubscriptionId(subId).isDataEnabled())) {
            return false;
        }

        return isImsSupported() && queryImsState(subId).isReadyToVideoCall();
    }

    @Override
    public void on4gLteUpdated() {
        updateState(mPreference);
    }

    private class PhoneTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {

        private TelephonyManager mTelephonyManager;

        @Override
        public void onCallStateChanged(int state) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            mTelephonyManager = context.getSystemService(TelephonyManager.class);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
            }
            // assign current call state so that it helps to show correct preference state even
            // before first onCallStateChanged() by initial registration.
            mCallState = mTelephonyManager.getCallState(subId);
            mTelephonyManager.registerTelephonyCallback(context.getMainExecutor(), this);
        }

        public void unregister() {
            mCallState = null;
            mTelephonyManager.unregisterTelephonyCallback(this);
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
