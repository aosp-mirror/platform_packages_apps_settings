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
import android.text.TextUtils;

import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

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

    private DropDownPreference mPreference;
    private PaymentBackend mPaymentBackend;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    public NfcForegroundPreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
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
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference == null) {
            return;
        }

        mPreference.setEntries(new CharSequence[]{
                mContext.getText(R.string.nfc_payment_favor_open),
                mContext.getText(R.string.nfc_payment_favor_default)
        });
        mPreference.setEntryValues(new CharSequence[]{"1", "0"});
    }

    @Override
    public void onPaymentAppsChanged() {
        updateState(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof DropDownPreference) {
            ((DropDownPreference) preference).setValue(
                    mPaymentBackend.isForegroundMode() ? "1" : "0");
        }
        super.updateState(preference);
    }

    @Override
    public CharSequence getSummary() {
        return mPreference.getEntry();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(preference instanceof DropDownPreference)) {
            return false;
        }
        final DropDownPreference pref = (DropDownPreference) preference;
        final String newValueString = (String) newValue;
        pref.setSummary(pref.getEntries()[pref.findIndexOfValue(newValueString)]);
        final boolean foregroundMode = Integer.parseInt(newValueString) != 0;
        mPaymentBackend.setForegroundMode(foregroundMode);
        mMetricsFeatureProvider.action(mContext,
                foregroundMode ? SettingsEnums.ACTION_NFC_PAYMENT_FOREGROUND_SETTING
                        : SettingsEnums.ACTION_NFC_PAYMENT_ALWAYS_SETTING);
        return true;
    }

    @Override
    public void updateNonIndexableKeys(List<String> keys) {
        final String key = getPreferenceKey();
        if (!TextUtils.isEmpty(key)) {
            keys.add(key);
        }
    }
}