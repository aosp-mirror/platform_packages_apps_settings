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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

/**
 * Preference controller for "Wifi Calling"
 */
public class WifiCallingPreferenceController extends TelephonyBasePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "WifiCallingPreference";

    @VisibleForTesting
    Integer mCallState;
    @VisibleForTesting
    CarrierConfigManager mCarrierConfigManager;
    private ImsMmTelManager mImsMmTelManager;
    @VisibleForTesting
    PhoneAccountHandle mSimCallManager;
    private PhoneCallStateListener mPhoneStateListener;
    private Preference mPreference;

    public WifiCallingPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mPhoneStateListener = new PhoneCallStateListener();
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return SubscriptionManager.isValidSubscriptionId(subId)
                && isWifiCallingEnabled(mContext, subId)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
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
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        final Intent intent = mPreference.getIntent();
        if (intent != null) {
            intent.putExtra(Settings.EXTRA_SUB_ID, mSubId);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if ((mCallState == null) || (preference == null)) {
            Log.d(TAG, "Skip update under mCallState=" + mCallState);
            return;
        }
        CharSequence summaryText = null;
        if (mSimCallManager != null) {
            final Intent intent = MobileNetworkUtils.buildPhoneAccountConfigureIntent(mContext,
                    mSimCallManager);
            if (intent == null) {
                // Do nothing in this case since preference is invisible
                return;
            }
            final PackageManager pm = mContext.getPackageManager();
            final List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
            preference.setTitle(resolutions.get(0).loadLabel(pm));
            preference.setIntent(intent);
        } else {
            final String title = SubscriptionManager.getResourcesForSubId(mContext, mSubId)
                    .getString(R.string.wifi_calling_settings_title);
            preference.setTitle(title);
            summaryText = getResourceIdForWfcMode(mSubId);
        }
        preference.setSummary(summaryText);
        preference.setEnabled(mCallState == TelephonyManager.CALL_STATE_IDLE);
    }

    private CharSequence getResourceIdForWfcMode(int subId) {
        int resId = com.android.internal.R.string.wifi_calling_off_summary;
        if (queryImsState(subId).isEnabledByUser()) {
            boolean useWfcHomeModeForRoaming = false;
            if (mCarrierConfigManager != null) {
                final PersistableBundle carrierConfig =
                        mCarrierConfigManager.getConfigForSubId(subId);
                if (carrierConfig != null) {
                    useWfcHomeModeForRoaming = carrierConfig.getBoolean(
                            CarrierConfigManager
                                    .KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL);
                }
            }
            final boolean isRoaming = getTelephonyManager(mContext, subId)
                    .isNetworkRoaming();
            final int wfcMode = (isRoaming && !useWfcHomeModeForRoaming)
                    ? mImsMmTelManager.getVoWiFiRoamingModeSetting() :
                    mImsMmTelManager.getVoWiFiModeSetting();
            switch (wfcMode) {
                case ImsMmTelManager.WIFI_MODE_WIFI_ONLY:
                    resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                    break;
                case ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED:
                    resId = com.android.internal.R.string
                            .wfc_mode_cellular_preferred_summary;
                    break;
                case ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED:
                    resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                    break;
                default:
                    break;
            }
        }
        return SubscriptionManager.getResourcesForSubId(mContext, subId).getText(resId);
    }

    public WifiCallingPreferenceController init(int subId) {
        mSubId = subId;
        mImsMmTelManager = getImsMmTelManager(mSubId);
        mSimCallManager = mContext.getSystemService(TelecomManager.class)
                .getSimCallManagerForSubscription(mSubId);

        return this;
    }

    @VisibleForTesting
    WifiCallingQueryImsState queryImsState(int subId) {
        return new WifiCallingQueryImsState(mContext, subId);
    }

    protected ImsMmTelManager getImsMmTelManager(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return null;
        }
        return ImsMmTelManager.createForSubscriptionId(subId);
    }

    @VisibleForTesting
    TelephonyManager getTelephonyManager(Context context, int subId) {
        final TelephonyManager telephonyMgr = context.getSystemService(TelephonyManager.class);
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return telephonyMgr;
        }
        final TelephonyManager subscriptionTelephonyMgr =
                telephonyMgr.createForSubscriptionId(subId);
        return (subscriptionTelephonyMgr == null) ? telephonyMgr : subscriptionTelephonyMgr;
    }


    private class PhoneCallStateListener extends PhoneStateListener {

        PhoneCallStateListener() {
            super(Looper.getMainLooper());
        }

        private TelephonyManager mTelephonyManager;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            mTelephonyManager = getTelephonyManager(context, subId);
            // assign current call state so that it helps to show correct preference state even
            // before first onCallStateChanged() by initial registration.
            mCallState = mTelephonyManager.getCallState(subId);
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
        }

        public void unregister() {
            mCallState = null;
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
        }
    }

    private boolean isWifiCallingEnabled(Context context, int subId) {
        final PhoneAccountHandle simCallManager =
                context.getSystemService(TelecomManager.class)
                       .getSimCallManagerForSubscription(subId);
        final int phoneId = SubscriptionManager.getSlotIndex(subId);

        boolean isWifiCallingEnabled;
        if (simCallManager != null) {
            final Intent intent = MobileNetworkUtils.buildPhoneAccountConfigureIntent(
                    context, simCallManager);

            isWifiCallingEnabled = intent != null;
        } else {
            isWifiCallingEnabled = queryImsState(subId).isReadyToWifiCalling();
        }

        return isWifiCallingEnabled;
    }
}
