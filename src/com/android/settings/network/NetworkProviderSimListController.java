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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.telephony.SubscriptionManager;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkProviderSimListController extends BasePreferenceController implements
        DefaultLifecycleObserver, MobileNetworkRepository.MobileNetworkCallback,
        DefaultSubscriptionReceiver.DefaultSubscriptionListener {

    private final SubscriptionManager mSubscriptionManager;
    @Nullable
    private PreferenceCategory mPreferenceCategory;
    private Map<Integer, RestrictedPreference> mPreferences;
    private final MobileNetworkRepository mMobileNetworkRepository;
    private List<SubscriptionInfoEntity> mSubInfoEntityList = new ArrayList<>();
    private final DefaultSubscriptionReceiver mDataSubscriptionChangedReceiver;

    public NetworkProviderSimListController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mPreferences = new ArrayMap<>();
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(context);
        mDataSubscriptionChangedReceiver = new DefaultSubscriptionReceiver(context, this);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        mMobileNetworkRepository.addRegister(owner, this,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mMobileNetworkRepository.updateEntity();
        mDataSubscriptionChangedReceiver.registerReceiver();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        mMobileNetworkRepository.removeRegister(this);
        mDataSubscriptionChangedReceiver.unRegisterReceiver();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        update();
    }

    private void update() {
        if (mPreferenceCategory == null) {
            return;
        }

        final Map<Integer, RestrictedPreference> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();

        final List<SubscriptionInfoEntity> subscriptions = getAvailablePhysicalSubscriptions();
        for (SubscriptionInfoEntity info : subscriptions) {
            final int subId = Integer.parseInt(info.subId);
            RestrictedPreference pref = existingPreferences.remove(subId);
            if (pref == null) {
                pref = new RestrictedPreference(mPreferenceCategory.getContext());
                mPreferenceCategory.addPreference(pref);
            }
            final CharSequence displayName = info.uniqueName;
            pref.setTitle(displayName);
            boolean isActiveSubscriptionId = info.isActiveSubscriptionId;
            pref.setSummary(getSummary(info, displayName));
            final Drawable drawable = mContext.getDrawable(
                    info.isEmbedded ? R.drawable.ic_sim_card_download : R.drawable.ic_sim_card);
            pref.setIcon(drawable);
            pref.setOnPreferenceClickListener(clickedPref -> {
                if (!info.isEmbedded && !isActiveSubscriptionId
                        && !SubscriptionUtil.showToggleForPhysicalSim(mSubscriptionManager)) {
                    SubscriptionUtil.startToggleSubscriptionDialogActivity(mContext, subId,
                            true);
                } else {
                    MobileNetworkUtils.launchMobileNetworkSettings(mContext, info);
                }
                return true;
            });
            mPreferences.put(subId, pref);
        }
        for (RestrictedPreference pref : existingPreferences.values()) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    public CharSequence getSummary(SubscriptionInfoEntity subInfo, CharSequence displayName) {
        if (subInfo.isActiveSubscriptionId) {
            CharSequence config = SubscriptionUtil.getDefaultSimConfig(mContext,
                    subInfo.getSubId());
            CharSequence summary = mContext.getResources().getString(
                    R.string.sim_category_active_sim);
            if (config == "") {
                return summary;
            } else {
                final StringBuilder activeSim = new StringBuilder();
                activeSim.append(summary).append(config);
                return activeSim;
            }
        } else {
            if (!subInfo.isEmbedded && !SubscriptionUtil.showToggleForPhysicalSim(
                    mSubscriptionManager)) {
                return mContext.getString(R.string.mobile_network_tap_to_activate, displayName);
            } else {
                return mContext.getString(R.string.sim_category_inactive_sim);
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return getAvailablePhysicalSubscriptions().isEmpty()
                ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @VisibleForTesting
    protected List<SubscriptionInfoEntity> getAvailablePhysicalSubscriptions() {
        return new ArrayList<>(mSubInfoEntityList);
    }

    @Override
    public void onAvailableSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        mSubInfoEntityList = subInfoEntityList;
        if (mPreferenceCategory != null) {
            mPreferenceCategory.setVisible(isAvailable());
        }
        update();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(mPreferenceCategory);
        update();
    }

    @Override
    public void onDefaultDataChanged(int defaultDataSubId) {
        refreshSummary(mPreferenceCategory);
        update();
    }

    @Override
    public void onDefaultVoiceChanged(int defaultVoiceSubId) {
        refreshSummary(mPreferenceCategory);
        update();
    }

    @Override
    public void onDefaultSmsChanged(int defaultSmsSubId) {
        refreshSummary(mPreferenceCategory);
        update();
    }
}
