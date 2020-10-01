/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.specialaccess;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * This Controller works with PaymentSettingsEnabler to control payment features availability
 * based on NFC status
 */
public class DefaultPaymentSettingsPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private final NfcAdapter mNfcAdapter;
    private PaymentSettingsEnabler mPaymentSettingsEnabler;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;

    public DefaultPaymentSettingsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mPackageManager = context.getPackageManager();
        mUserManager = context.getSystemService(UserManager.class);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (!isAvailable()) {
            mPaymentSettingsEnabler = null;
            return;
        }

        final Preference preference = screen.findPreference(getPreferenceKey());
        mPaymentSettingsEnabler = new PaymentSettingsEnabler(mContext, preference);
    }

    @Override
    public void onResume() {
        if (mPaymentSettingsEnabler != null) {
            mPaymentSettingsEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        if (mPaymentSettingsEnabler != null) {
            mPaymentSettingsEnabler.pause();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
                || !mPackageManager.hasSystemFeature(
                        PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!mUserManager.isAdminUser()) {
            return DISABLED_FOR_USER;
        }
        if (mNfcAdapter == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        if (!mNfcAdapter.isEnabled()) {
            return DISABLED_DEPENDENT_SETTING;
        }
        return AVAILABLE;
    }
}
