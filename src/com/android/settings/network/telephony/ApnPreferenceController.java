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

import static android.provider.Telephony.Carriers.ENFORCE_MANAGED_URI;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settings.network.ApnSettings;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Apn settings"
 */
public class ApnPreferenceController extends TelephonyBasePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    CarrierConfigManager mCarrierConfigManager;
    private Preference mPreference;
    private DpcApnEnforcedObserver mDpcApnEnforcedObserver;

    public ApnPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mDpcApnEnforcedObserver = new DpcApnEnforcedObserver(new Handler(Looper.getMainLooper()));
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
        final boolean isCdmaApn = MobileNetworkUtils.isCdmaOptions(mContext, subId)
                && carrierConfig != null
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_APN_SETTING_CDMA_BOOL);
        final boolean isGsmApn = MobileNetworkUtils.isGsmOptions(mContext, subId)
                && carrierConfig != null
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL);
        final boolean hideCarrierNetwork = carrierConfig == null
                || carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL);

        return !hideCarrierNetwork && (isCdmaApn || isGsmApn)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void onStart() {
        mDpcApnEnforcedObserver.register(mContext);
    }

    @Override
    public void onStop() {
        mDpcApnEnforcedObserver.unRegister(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mPreference == null) {
            return;
        }
        ((RestrictedPreference) mPreference).setDisabledByAdmin(
                MobileNetworkUtils.isDpcApnEnforced(mContext)
                        ? RestrictedLockUtilsInternal.getDeviceOwner(mContext)
                        : null);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            // This activity runs in phone process, we must use intent to start
            final Intent intent = new Intent(Settings.ACTION_APN_SETTINGS);
            // This will setup the Home and Search affordance
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
            intent.putExtra(ApnSettings.SUB_ID, mSubId);
            mContext.startActivity(intent);
            return true;
        }

        return false;
    }

    public void init(int subId) {
        mSubId = subId;
    }

    @VisibleForTesting
    void setPreference(Preference preference) {
        mPreference = preference;
    }

    private class DpcApnEnforcedObserver extends ContentObserver {
        DpcApnEnforcedObserver(Handler handler) {
            super(handler);
        }

        public void register(Context context) {
            context.getContentResolver().registerContentObserver(ENFORCE_MANAGED_URI, false, this);

        }

        public void unRegister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateState(mPreference);
        }
    }
}
