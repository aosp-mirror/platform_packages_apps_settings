/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static androidx.lifecycle.Lifecycle.Event;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copied the logic of WiFi calling from {@link WifiCallingPreferenceController}.
 */
public class NetworkProviderWifiCallingGroup extends
        AbstractPreferenceController implements LifecycleObserver,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private static final String TAG = "NetworkProviderWifiCallingGroup";
    private static final int PREF_START_ORDER = 10;
    private static final String KEY_PREFERENCE_WIFICALLING_GROUP = "provider_model_wfc_group";

    @VisibleForTesting
    protected CarrierConfigManager mCarrierConfigManager;
    private SubscriptionManager mSubscriptionManager;
    private PhoneCallStateTelephonyCallback mTelephonyCallback;
    private String mPreferenceGroupKey;
    private PreferenceGroup mPreferenceGroup;
    private Map<Integer, TelephonyManager> mTelephonyManagerList = new HashMap<>();
    private Map<Integer, PhoneAccountHandle> mSimCallManagerList = new HashMap<>();
    private Map<Integer, Preference> mWifiCallingForSubPreferences;
    private List<SubscriptionInfo> mSubInfoListForWfc;
    private SubscriptionsChangeListener mChangeListener;

    public NetworkProviderWifiCallingGroup(Context context, Lifecycle lifecycle,
            String preferenceGroupKey) {
        super(context);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class)
                .createForAllUserProfiles();
        mPreferenceGroupKey = preferenceGroupKey;
        mWifiCallingForSubPreferences = new ArrayMap<>();
        setSubscriptionInfoList(context);
        if (mTelephonyCallback == null) {
            mTelephonyCallback = new PhoneCallStateTelephonyCallback();
        }
        lifecycle.addObserver(this);
        mChangeListener = new SubscriptionsChangeListener(context, this);
    }

    private void setSubscriptionInfoList(Context context) {
        mSubInfoListForWfc = new ArrayList<>(
                SubscriptionUtil.getActiveSubscriptions(mSubscriptionManager));
        mSubInfoListForWfc.removeIf(info -> {
            final int subId = info.getSubscriptionId();
            setTelephonyManagerForSubscriptionId(context, subId);
            setPhoneAccountHandleForSubscriptionId(context, subId);
            boolean isExisted = mSubInfoListForWfc.contains(info);
            boolean shouldShowWfcForSub = shouldShowWifiCallingForSub(subId);
            if (!shouldShowWfcForSub && isExisted) {
                return true;
            }
            return false;
        });
        Log.d(TAG, "setSubscriptionInfoList: mSubInfoListForWfc size:"
                + mSubInfoListForWfc.size());
    }

    private void setTelephonyManagerForSubscriptionId(Context context, int subId) {
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        mTelephonyManagerList.put(subId, telephonyManager);
    }

    private void setPhoneAccountHandleForSubscriptionId(Context context, int subId) {
        PhoneAccountHandle phoneAccountHandle = context.getSystemService(TelecomManager.class)
                .getSimCallManagerForSubscription(subId);
        mSimCallManagerList.put(subId, phoneAccountHandle);
    }

    private TelephonyManager getTelephonyManagerForSubscriptionId(int subId) {
        return mTelephonyManagerList.get(subId);
    }

    @VisibleForTesting
    protected PhoneAccountHandle getPhoneAccountHandleForSubscriptionId(int subId) {
        return mSimCallManagerList.get(subId);
    }

    @VisibleForTesting
    protected WifiCallingQueryImsState queryImsState(int subId) {
        return new WifiCallingQueryImsState(mContext, subId);
    }

    @OnLifecycleEvent(Event.ON_RESUME)
    public void onResume() {
        if (mChangeListener != null) {
            mChangeListener.start();
        }
        updateListener();
        update();
    }

    @OnLifecycleEvent(Event.ON_PAUSE)
    public void onPause() {
        if (mChangeListener != null) {
            mChangeListener.stop();
        }
        if ((mTelephonyCallback != null)) {
            mTelephonyCallback.unregister();
        }
    }

    @Override
    public boolean isAvailable() {
        if (mSubInfoListForWfc == null) {
            Log.d(TAG, "No active subscriptions, hide the controller");
            return false;
        } else {
            return mSubInfoListForWfc.size() >= 1;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceGroup = screen.findPreference(mPreferenceGroupKey);
        update();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        // Do nothing in this case since preference is invisible
        if (preference == null) {
            return;
        }
        update();
    }

    private void update() {
        if (mPreferenceGroup == null) {
            Log.d(TAG, "mPreferenceGroup == null");
            return;
        }

        if (!isAvailable()) {
            for (Preference pref : mWifiCallingForSubPreferences.values()) {
                mPreferenceGroup.removePreference(pref);
            }
            mWifiCallingForSubPreferences.clear();
            return;
        }

        final Map<Integer, Preference> toRemovePreferences = mWifiCallingForSubPreferences;
        mWifiCallingForSubPreferences = new ArrayMap<>();
        setSubscriptionInfoForPreference(toRemovePreferences);
        for (Preference pref : toRemovePreferences.values()) {
            mPreferenceGroup.removePreference(pref);
        }
    }

    private void setSubscriptionInfoForPreference(Map<Integer, Preference> toRemovePreferences) {
        int order = PREF_START_ORDER;
        for (SubscriptionInfo info : mSubInfoListForWfc) {
            final int subId = info.getSubscriptionId();

            if (!shouldShowWifiCallingForSub(subId)) {
                continue;
            }

            Preference pref = toRemovePreferences.remove(subId);
            if (pref == null) {
                pref = new Preference(mPreferenceGroup.getContext());
                mPreferenceGroup.addPreference(pref);
            }

            CharSequence title = SubscriptionUtil.getUniqueSubscriptionDisplayName(info,
                    mContext);
            if (getPhoneAccountHandleForSubscriptionId(subId) != null) {
                final Intent intent = MobileNetworkUtils.buildPhoneAccountConfigureIntent(
                        mContext,
                        getPhoneAccountHandleForSubscriptionId(subId));
                if (intent != null) {
                    final PackageManager pm = mContext.getPackageManager();
                    final List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
                    title = resolutions.get(0).loadLabel(pm);
                    pref.setIntent(intent);
                }
            }

            pref.setTitle(title);
            pref.setOnPreferenceClickListener(clickedPref -> {
                final Intent intent = new Intent(
                        mContext,
                        com.android.settings.Settings.WifiCallingSettingsActivity.class);
                intent.putExtra(Settings.EXTRA_SUB_ID, info.getSubscriptionId());
                mContext.startActivity(intent);
                return true;
            });

            pref.setEnabled(getTelephonyManagerForSubscriptionId(subId)
                    .getCallStateForSubscription() == TelephonyManager.CALL_STATE_IDLE);
            pref.setOrder(order++);

            int resId = com.android.internal.R.string.wifi_calling_off_summary;
            if (queryImsState(subId).isEnabledByUser()) {
                resId = R.string.calls_sms_wfc_summary;
            }
            pref.setSummary(resId);

            mWifiCallingForSubPreferences.put(subId, pref);
        }
    }

    // Do nothing in this case since preference will not be impacted.
    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onSubscriptionsChanged() {
        Log.d(TAG, "onSubscriptionsChanged:");
        setSubscriptionInfoList(mContext);
        if (mPreferenceGroup != null) {
            mPreferenceGroup.setVisible(isAvailable());
        }
        updateListener();
        update();
    }

    private void updateListener() {
        for (SubscriptionInfo info : mSubInfoListForWfc) {
            int subId = info.getSubscriptionId();
            if ((mTelephonyCallback != null)) {
                mTelephonyCallback.register(mContext, subId);
            }
        }
    }

    private class PhoneCallStateTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {

        private TelephonyManager mTelephonyManager;

        @Override
        public void onCallStateChanged(int state) {
            update();
        }

        public void register(Context context, int subId) {
            mTelephonyManager = getTelephonyManagerForSubscriptionId(subId);
            mTelephonyManager.registerTelephonyCallback(context.getMainExecutor(), this);
        }

        public void unregister() {
            if (mTelephonyManager != null) {
                mTelephonyManager.unregisterTelephonyCallback(this);
            }
        }
    }

    /**
     * To indicate that should show the Wi-Fi calling preference or not.
     *
     * It will check these 3 conditions:
     * 1. Check the subscription is valid or not.
     * 2. Check whether Wi-Fi Calling can be perform or not on this subscription.
     * 3. Check the carrier's config (carrier_wfc_ims_available_bool). If true, the carrier
     * supports the Wi-Fi calling, otherwise false.
     */
    @VisibleForTesting
    protected boolean shouldShowWifiCallingForSub(int subId) {
        if (SubscriptionManager.isValidSubscriptionId(subId)
                && MobileNetworkUtils.isWifiCallingEnabled(mContext, subId, queryImsState(subId))) {
            return true;
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PREFERENCE_WIFICALLING_GROUP;
    }
}
