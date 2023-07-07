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

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.network.MobileNetworkRepository;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Roaming"
 */
public class RoamingPreferenceController extends TelephonyTogglePreferenceController implements
        LifecycleObserver, MobileNetworkRepository.MobileNetworkCallback {
    private static final String TAG = "RoamingController";
    private static final String DIALOG_TAG = "MobileDataDialog";

    private RestrictedSwitchPreference mSwitchPreference;
    private TelephonyManager mTelephonyManager;
    private CarrierConfigManager mCarrierConfigManager;
    protected MobileNetworkRepository mMobileNetworkRepository;
    protected LifecycleOwner mLifecycleOwner;
    private List<MobileNetworkInfoEntity> mMobileNetworkInfoEntityList = new ArrayList<>();

    @VisibleForTesting
    FragmentManager mFragmentManager;
    MobileNetworkInfoEntity mMobileNetworkInfoEntity;

    public RoamingPreferenceController(Context context, String key, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner, int subId) {
        super(context, key);
        mSubId = subId;
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(context);
        mLifecycleOwner = lifecycleOwner;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        if (carrierConfig != null && carrierConfig.getBoolean(
                CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL)) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mMobileNetworkRepository.addRegister(mLifecycleOwner, this, mSubId);
        mMobileNetworkRepository.updateEntity();
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mMobileNetworkRepository.removeRegister(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitchPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                ? AVAILABLE
                : AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isDialogNeeded()) {
            showDialog();
        } else {
            // Update data directly if we don't need dialog
            mTelephonyManager.setDataRoamingEnabled(isChecked);
            return true;
        }

        return false;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mSwitchPreference = (RestrictedSwitchPreference) preference;
        update();
    }

    private void update() {
        if (mSwitchPreference == null) {
            return;
        }
        if (!mSwitchPreference.isDisabledByAdmin()) {
            mSwitchPreference.setEnabled(mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            mSwitchPreference.setChecked(isChecked());
        }
    }

    @VisibleForTesting
    boolean isDialogNeeded() {
        final boolean isRoamingEnabled = mMobileNetworkInfoEntity == null ? false
                : mMobileNetworkInfoEntity.isDataRoamingEnabled;
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(
                mSubId);
        // Need dialog if we need to turn on roaming and the roaming charge indication is allowed
        if (!isRoamingEnabled && (carrierConfig == null || !carrierConfig.getBoolean(
                CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        return mMobileNetworkInfoEntity == null ? false
                : mMobileNetworkInfoEntity.isDataRoamingEnabled;
    }

    public void init(FragmentManager fragmentManager, int subId, MobileNetworkInfoEntity entity) {
        mFragmentManager = fragmentManager;
        mSubId = subId;
        mMobileNetworkInfoEntity = entity;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }
        final TelephonyManager telephonyManager = mTelephonyManager
                .createForSubscriptionId(mSubId);
        if (telephonyManager == null) {
            Log.w(TAG, "fail to init in sub" + mSubId);
            mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            return;
        }
        mTelephonyManager = telephonyManager;
    }

    private void showDialog() {
        final RoamingDialogFragment dialogFragment = RoamingDialogFragment.newInstance(mSubId);

        dialogFragment.show(mFragmentManager, DIALOG_TAG);
    }

    @VisibleForTesting
    public void setMobileNetworkInfoEntity(MobileNetworkInfoEntity mobileNetworkInfoEntity) {
        mMobileNetworkInfoEntity = mobileNetworkInfoEntity;
    }

    @Override
    public void onAllMobileNetworkInfoChanged(
            List<MobileNetworkInfoEntity> mobileNetworkInfoEntityList) {
        mMobileNetworkInfoEntityList = mobileNetworkInfoEntityList;
        mMobileNetworkInfoEntityList.forEach(entity -> {
            if (Integer.parseInt(entity.subId) == mSubId) {
                mMobileNetworkInfoEntity = entity;
                update();
                refreshSummary(mSwitchPreference);
                return;
            }
        });
    }

    @Override
    public void onDataRoamingChanged(int subId, boolean enabled) {
        if (subId != mSubId) {
            Log.d(TAG, "onDataRoamingChanged - wrong subId : " + subId + " / " + enabled);
            return;
        }
        update();
    }
}
