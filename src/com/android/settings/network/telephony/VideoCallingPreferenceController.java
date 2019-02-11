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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.ims.ImsManager;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Video Calling"
 */
public class VideoCallingPreferenceController extends TelephonyTogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop,
        Enhanced4gLtePreferenceController.On4gLteUpdateListener {

    private Preference mPreference;
    private TelephonyManager mTelephonyManager;
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;
    @VisibleForTesting
    ImsManager mImsManager;
    private PhoneCallStateListener mPhoneStateListener;
    private DataContentObserver mDataContentObserver;

    public VideoCallingPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mDataContentObserver = new DataContentObserver(new Handler(Looper.getMainLooper()));
        mPhoneStateListener = new PhoneCallStateListener(Looper.getMainLooper());
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && MobileNetworkUtils.isWifiCallingEnabled(mContext,
                SubscriptionManager.getPhoneId(subId))
                && isVideoCallEnabled()
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
        mDataContentObserver.register(mContext, mSubId);
    }

    @Override
    public void onStop() {
        mPhoneStateListener.unregister();
        mDataContentObserver.unRegister(mContext);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final SwitchPreference switchPreference = (SwitchPreference) preference;
        final boolean videoCallEnabled = isVideoCallEnabled();
        switchPreference.setVisible(videoCallEnabled);
        if (videoCallEnabled) {
            final boolean is4gLteEnabled = mImsManager.isEnhanced4gLteModeSettingEnabledByUser()
                    && mImsManager.isNonTtyOrTtyOnVolteEnabled();
            preference.setEnabled(is4gLteEnabled &&
                    mTelephonyManager.getCallState(mSubId) == TelephonyManager.CALL_STATE_IDLE);
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
        mTelephonyManager = TelephonyManager.from(mContext).createForSubscriptionId(mSubId);
        mCarrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mImsManager = ImsManager.getInstance(mContext, SubscriptionManager.getPhoneId(mSubId));
        }

        return this;
    }

    @VisibleForTesting
    boolean isVideoCallEnabled() {
        return mCarrierConfig != null && mImsManager != null
                && mImsManager.isVtEnabledByPlatform()
                && mImsManager.isVtProvisionedOnDevice()
                && MobileNetworkUtils.isImsServiceStateReady(mImsManager)
                && (mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS)
                || mTelephonyManager.isDataEnabled());
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
            updateState(mPreference);
        }

        public void register(int subId) {
            mSubId = subId;
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
        }

        public void unregister() {
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
        }
    }

    /**
     * Listener that listens mobile data state change.
     */
    public class DataContentObserver extends ContentObserver {

        public DataContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA);
            if (TelephonyManager.getDefault().getSimCount() != 1) {
                uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId);
            }
            context.getContentResolver().registerContentObserver(uri,
                    false /* notifyForDescendants */, this /* observer */);
        }

        public void unRegister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }
    }
}
