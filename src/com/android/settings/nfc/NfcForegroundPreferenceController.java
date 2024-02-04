/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.nfc;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.permission.flags.Flags;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

public class NfcForegroundPreferenceController extends BasePreferenceController implements
        PaymentBackend.Callback, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnStart, OnStop {

    private ListPreference mPreference;
    private PaymentBackend mPaymentBackend;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private final String[] mListValues;
    private final String[] mListEntries;

    public NfcForegroundPreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mListValues = context.getResources().getStringArray(R.array.nfc_payment_favor_values);
        mListEntries = context.getResources().getStringArray(R.array.nfc_payment_favor);
    }

    public void setPaymentBackend(PaymentBackend backend) {
        mPaymentBackend = backend;
    }

    @Override
    public void onStart() {
        if (mPaymentBackend != null) {
            mPaymentBackend.registerCallback(this);
        }
    }

    @Override
    public void onStop() {
        if (mPaymentBackend != null) {
            mPaymentBackend.unregisterCallback(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.walletRoleEnabled()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        final PackageManager pm = mContext.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mPaymentBackend == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        final List<PaymentBackend.PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();
        return (appInfos != null && !appInfos.isEmpty())
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onPaymentAppsChanged() {
        updateState(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!(preference instanceof ListPreference)) {
            return;
        }
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setIconSpaceReserved(false);
        listPreference.setValue(mListValues[mPaymentBackend.isForegroundMode() ? 1 : 0]);
    }

    @Override
    public CharSequence getSummary() {
        return mListEntries[mPaymentBackend.isForegroundMode() ? 1 : 0];
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(preference instanceof ListPreference)) {
            return false;
        }

        final ListPreference listPreference = (ListPreference) preference;
        final String newValueString = (String) newValue;
        listPreference.setSummary(mListEntries[listPreference.findIndexOfValue(newValueString)]);
        final boolean foregroundMode = Integer.parseInt(newValueString) != 0;
        mPaymentBackend.setForegroundMode(foregroundMode);
        mMetricsFeatureProvider.action(mContext,
                foregroundMode ? SettingsEnums.ACTION_NFC_PAYMENT_FOREGROUND_SETTING
                        : SettingsEnums.ACTION_NFC_PAYMENT_ALWAYS_SETTING);
        return true;
    }
}
