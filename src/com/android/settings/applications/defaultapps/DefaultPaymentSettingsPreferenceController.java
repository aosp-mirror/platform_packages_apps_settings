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

package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.nfc.PaymentBackend;
import com.android.settingslib.core.AbstractPreferenceController;

public class DefaultPaymentSettingsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private final NfcAdapter mNfcAdapter;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private PaymentBackend mPaymentBackend;

    public DefaultPaymentSettingsPreferenceController(Context context) {
        super(context);
        mPackageManager = context.getPackageManager();
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
    }

    @Override
    public boolean isAvailable() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
                && mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
                && mUserManager.isAdminUser()
                && mNfcAdapter != null
                && mNfcAdapter.isEnabled();
    }

    @Override
    public void updateState(Preference preference) {
        if (mPaymentBackend == null) {
            if (mNfcAdapter != null) {
                mPaymentBackend = new PaymentBackend(mContext);
            } else {
                mPaymentBackend = null;
            }
        }
        if (mPaymentBackend == null) {
            return;
        }
        mPaymentBackend.refresh();
        final PaymentBackend.PaymentAppInfo app = mPaymentBackend.getDefaultApp();
        if (app != null) {
            preference.setSummary(app.label);
        } else {
            preference.setSummary(R.string.app_list_preference_none);
        }
    }

    @Override
    public String getPreferenceKey() {
        return "default_payment_app";
    }
}
