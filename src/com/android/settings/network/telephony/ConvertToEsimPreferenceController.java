/**
 * Copyright (C) 2022 The Android Open Source Project
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

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.network.MobileNetworkRepository;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.DataServiceUtils;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.mobile.dataservice.UiccInfoEntity;

import java.util.ArrayList;
import java.util.List;

public class ConvertToEsimPreferenceController extends TelephonyBasePreferenceController implements
        LifecycleObserver, MobileNetworkRepository.MobileNetworkCallback {

    private Preference mPreference;
    private LifecycleOwner mLifecycleOwner;
    private MobileNetworkRepository mMobileNetworkRepository;
    private List<SubscriptionInfoEntity> mSubscriptionInfoEntityList = new ArrayList<>();
    private SubscriptionInfoEntity mSubscriptionInfoEntity;

    public ConvertToEsimPreferenceController(Context context, String key, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner, int subId) {
        super(context, key);
        mSubId = subId;
        mMobileNetworkRepository = MobileNetworkRepository.createBySubId(context, this, mSubId);
        mLifecycleOwner = lifecycleOwner;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    public void init(int subId, SubscriptionInfoEntity subInfoEntity) {
        mSubId = subId;
        mSubscriptionInfoEntity = subInfoEntity;
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mMobileNetworkRepository.addRegister(mLifecycleOwner);
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mMobileNetworkRepository.removeRegister();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return CONDITIONALLY_UNAVAILABLE;
        // TODO(b/262195754): Need the intent to enabled the feature.
//        return mSubscriptionInfoEntity != null && mSubscriptionInfoEntity.isActiveSubscriptionId
//                && !mSubscriptionInfoEntity.isEmbedded ? AVAILABLE
//                : CONDITIONALLY_UNAVAILABLE;
    }

    @VisibleForTesting
    void update() {
        if (mPreference == null) {
            return;
        }
        mPreference.setVisible(getAvailabilityStatus(mSubId) == AVAILABLE);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        // Send intent to launch LPA
        return true;
    }

    @VisibleForTesting
    public void setSubscriptionInfoEntity(SubscriptionInfoEntity subscriptionInfoEntity) {
        mSubscriptionInfoEntity = subscriptionInfoEntity;
    }

    @Override
    public void onActiveSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        // TODO(b/262195754): Need the intent to enabled the feature.
//        if (DataServiceUtils.shouldUpdateEntityList(mSubscriptionInfoEntityList,
//                subInfoEntityList)) {
//            mSubscriptionInfoEntityList = subInfoEntityList;
//            mSubscriptionInfoEntityList.forEach(entity -> {
//                if (Integer.parseInt(entity.subId) == mSubId) {
//                    mSubscriptionInfoEntity = entity;
//                    update();
//                }
//            });
//        }
    }
}
