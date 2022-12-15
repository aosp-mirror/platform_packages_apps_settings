/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Preference controller for "Backup Calling"
 **/
public class NetworkProviderBackupCallingGroup extends
        TelephonyTogglePreferenceController implements LifecycleObserver,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient  {
    private static final String TAG = "NetworkProviderBackupCallingGroup";
    private static final String KEY_PREFERENCE_BACKUPCALLING_GROUP =
            "provider_model_backup_call_group";
    private static final int PREF_START_ORDER = 10;

    private String mPreferenceGroupKey;
    private PreferenceGroup mPreferenceGroup;
    private Map<Integer, SwitchPreference> mBackupCallingForSubPreferences;
    private List<SubscriptionInfo> mSubInfoListForBackupCall;
    private Map<Integer, TelephonyManager> mTelephonyManagerList = new HashMap<>();
    private SubscriptionsChangeListener mSubscriptionsChangeListener;

    public NetworkProviderBackupCallingGroup(Context context, Lifecycle lifecycle,
            List<SubscriptionInfo> subscriptionList, String preferenceGroupKey) {
        super(context, preferenceGroupKey);
        mPreferenceGroupKey = preferenceGroupKey;
        mSubInfoListForBackupCall = subscriptionList;
        mBackupCallingForSubPreferences = new ArrayMap<>();
        setSubscriptionInfoList(context);
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(Event.ON_RESUME)
    public void onResume() {
        if (mSubscriptionsChangeListener == null) {
            mSubscriptionsChangeListener = new SubscriptionsChangeListener(mContext, this);
        }
        mSubscriptionsChangeListener.start();
    }

    @OnLifecycleEvent(Event.ON_PAUSE)
    public void onPause() {
        if (mSubscriptionsChangeListener != null) {
            mSubscriptionsChangeListener.stop();
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (mSubInfoListForBackupCall == null
                || getSubscriptionInfoFromList(mSubInfoListForBackupCall, subId) == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        return (mSubInfoListForBackupCall.size() > 1) ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    private boolean setCrossSimCallingEnabled(int subId, boolean checked) {
        ImsMmTelManager imsMmTelMgr = getImsMmTelManager(subId);
        if (imsMmTelMgr == null) {
            Log.d(TAG, "setCrossSimCallingEnabled(), ImsMmTelManager is null");
            return false;
        }

        try {
            imsMmTelMgr.setCrossSimCallingEnabled(checked);
        } catch (ImsException exception) {
            Log.w(TAG, "fail to get cross SIM calling configuration", exception);
            return false;
        }
        return true;
    }

    @Override
    public boolean setChecked(boolean checked) {
        return false;
    }

    private boolean isCrossSimCallingEnabled(int subId) {
        ImsMmTelManager imsMmTelMgr = getImsMmTelManager(subId);
        if (imsMmTelMgr == null) {
            Log.d(TAG, "isCrossSimCallingEnabled(), ImsMmTelManager is null");
            return false;
        }
        try {
            return imsMmTelMgr.isCrossSimCallingEnabled();
        } catch (ImsException exception) {
            Log.w(TAG, "fail to get cross SIM calling configuration", exception);
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        return false;
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
            return;
        }

        setSubscriptionInfoList(mContext);
        if (mSubInfoListForBackupCall == null || mSubInfoListForBackupCall.size() < 2) {
            for (SwitchPreference pref : mBackupCallingForSubPreferences.values()) {
                mPreferenceGroup.removePreference(pref);
            }
            mBackupCallingForSubPreferences.clear();
            return;
        }

        Map<Integer, SwitchPreference> toRemovePreferences = mBackupCallingForSubPreferences;
        mBackupCallingForSubPreferences = new ArrayMap<>();
        setSubscriptionInfoForPreference(toRemovePreferences);
    }

    private void setSubscriptionInfoForPreference(
            Map<Integer, SwitchPreference> toRemovePreferences) {
        int order = PREF_START_ORDER;
        for (SubscriptionInfo subInfo : mSubInfoListForBackupCall) {
            final int subId = subInfo.getSubscriptionId();

            SwitchPreference pref = toRemovePreferences.remove(subId);
            if (pref == null) {
                pref = new SwitchPreference(mPreferenceGroup.getContext());
                mPreferenceGroup.addPreference(pref);
            }

            CharSequence displayName = (subInfo == null) ? ""
                    : SubscriptionUtil.getUniqueSubscriptionDisplayName(subInfo, mContext);
            pref.setTitle(displayName);
            pref.setOrder(order++);
            pref.setSummary(getSummary(displayName));
            boolean enabled = isCrossSimCallingEnabled(subId);
            pref.setChecked(enabled);
            pref.setOnPreferenceClickListener(clickedPref -> {
                setCrossSimCallingEnabled(subId, !enabled);
                return true;
            });
            mBackupCallingForSubPreferences.put(subId, pref);
        }
    }

    private String getSummary(CharSequence displayName) {
        String summary = String.format(
                getResourcesForSubId().getString(R.string.backup_calling_setting_summary),
                displayName)
                .toString();
        return summary;
    }

    private void setSubscriptionInfoList(Context context) {
        if (mSubInfoListForBackupCall != null) {
            mSubInfoListForBackupCall.removeIf(info -> {
                int subId = info.getSubscriptionId();
                setTelephonyManagerForSubscriptionId(context, subId);
                if (!hasBackupCallingFeature(subId) && mSubInfoListForBackupCall.contains(info)) {
                    return true;
                }
                return false;
            });
        } else {
            Log.d(TAG, "No active subscriptions");
        }
    }

    private void setTelephonyManagerForSubscriptionId(Context context, int subId) {
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        mTelephonyManagerList.put(subId, telephonyManager);
    }

    @VisibleForTesting
    protected boolean hasBackupCallingFeature(int subscriptionId) {
        return isCrossSimEnabledByPlatform(mContext, subscriptionId);
    }

    /**
     * Copied from {@link BackupCallingPreferenceController}
     **/
    @VisibleForTesting
    protected boolean isCrossSimEnabledByPlatform(Context context, int subscriptionId) {
        // TODO : Change into API which created for accessing
        //        com.android.ims.ImsManager#isCrossSimEnabledByPlatform()
        if ((new WifiCallingQueryImsState(context, subscriptionId)).isWifiCallingSupported()) {
            PersistableBundle bundle = getCarrierConfigForSubId(subscriptionId);
            return (bundle != null) && bundle.getBoolean(
                    CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                    false /*default*/);
        }
        Log.d(TAG, "WifiCalling is not supported by framework. subId = " + subscriptionId);
        return false;
    }

    private ImsMmTelManager getImsMmTelManager(int subId) {
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            return null;
        }
        ImsManager imsMgr = mContext.getSystemService(ImsManager.class);
        return (imsMgr == null) ? null : imsMgr.getImsMmTelManager(subId);
    }

    private SubscriptionInfo getSubscriptionInfoFromList(
            List<SubscriptionInfo> subInfoList, int subId) {
        for (SubscriptionInfo subInfo : subInfoList) {
            if ((subInfo != null) && (subInfo.getSubscriptionId() == subId)) {
                return subInfo;
            }
        }
        return null;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PREFERENCE_BACKUPCALLING_GROUP;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {}

    @Override
    public void onSubscriptionsChanged() {
        SubscriptionManager subscriptionManager =
                mContext.getSystemService(SubscriptionManager.class);
        mSubInfoListForBackupCall = SubscriptionUtil.getActiveSubscriptions(subscriptionManager);
        update();
    }
}
