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

package com.android.settings.network;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.telephony.MobileNetworkActivity;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkProviderSimListController extends AbstractPreferenceController implements
        LifecycleObserver, SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "NetworkProviderSimListCtrl";
    private static final String KEY_PREFERENCE_CATEGORY_SIM = "provider_model_sim_category";
    private static final String KEY_PREFERENCE_SIM = "provider_model_sim_list";

    private SubscriptionManager mSubscriptionManager;
    private SubscriptionsChangeListener mChangeListener;
    private PreferenceCategory mPreferenceCategory;
    private Map<Integer, Preference> mPreferences;

    public NetworkProviderSimListController(Context context, Lifecycle lifecycle) {
        super(context);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mChangeListener = new SubscriptionsChangeListener(context, this);
        mPreferences = new ArrayMap<>();
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mChangeListener.start();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mContext.registerReceiver(mDataSubscriptionChangedReceiver, filter);
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mChangeListener.stop();
        if (mDataSubscriptionChangedReceiver != null) {
            mContext.unregisterReceiver(mDataSubscriptionChangedReceiver);
        }
    }

    @VisibleForTesting
    final BroadcastReceiver mDataSubscriptionChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                update();
            }
        }
    };

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(KEY_PREFERENCE_CATEGORY_SIM);
        update();
    }

    private void update() {
        if (mPreferenceCategory == null) {
            return;
        }

        final Map<Integer, Preference> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();

        final List<SubscriptionInfo> subscriptions = getAvailablePhysicalSubscription();
        for (SubscriptionInfo info : subscriptions) {
            final int subId = info.getSubscriptionId();
            Preference pref = existingPreferences.remove(subId);
            if (pref == null) {
                pref = new Preference(mPreferenceCategory.getContext());
                mPreferenceCategory.addPreference(pref);
            }
            final CharSequence displayName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                    info, mContext);
            pref.setTitle(displayName);
            pref.setSummary(getSummary(subId, displayName));

            pref.setOnPreferenceClickListener(clickedPref -> {
                if (!mSubscriptionManager.isActiveSubscriptionId(subId)
                        && !SubscriptionUtil.showToggleForPhysicalSim(mSubscriptionManager)) {
                    SubscriptionUtil.startToggleSubscriptionDialogActivity(mContext, subId,
                            true);
                } else {
                    final Intent intent = new Intent(mContext, MobileNetworkActivity.class);
                    intent.putExtra(Settings.EXTRA_SUB_ID, info.getSubscriptionId());
                    mContext.startActivity(intent);
                }
                return true;
            });
            mPreferences.put(subId, pref);
        }
        for (Preference pref : existingPreferences.values()) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    public CharSequence getSummary(int subId, CharSequence displayName) {
        if (mSubscriptionManager.isActiveSubscriptionId(subId)) {
            CharSequence config = SubscriptionUtil.getDefaultSimConfig(mContext, subId);
            CharSequence summary = mContext.getResources().getString(
                    R.string.sim_category_active_sim);
            if (config == null) {
                return summary;
            } else {
                final StringBuilder activeSim = new StringBuilder();
                activeSim.append(summary).append(config);
                return activeSim;
            }
        } else if (SubscriptionUtil.showToggleForPhysicalSim(mSubscriptionManager)) {
            return mContext.getString(R.string.sim_category_inactive_sim);
        } else {
            return mContext.getString(R.string.mobile_network_tap_to_activate, displayName);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!getAvailablePhysicalSubscription().isEmpty()) {
            return true;
        }
        return false;
    }

    @VisibleForTesting
    protected List<SubscriptionInfo> getAvailablePhysicalSubscription() {
        List<SubscriptionInfo> subList = new ArrayList<>();
        for (SubscriptionInfo info : SubscriptionUtil.getAvailableSubscriptions(mContext)) {
            if (!info.isEmbedded()) {
                subList.add(info);
            }
        }
        return subList;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PREFERENCE_SIM;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onSubscriptionsChanged() {
        update();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(mPreferenceCategory);
        update();
    }

    @VisibleForTesting
    protected int getDefaultVoiceSubscriptionId() {
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    @VisibleForTesting
    protected int getDefaultSmsSubscriptionId() {
        return SubscriptionManager.getDefaultSmsSubscriptionId();
    }

    @VisibleForTesting
    protected int getDefaultDataSubscriptionId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }
}
