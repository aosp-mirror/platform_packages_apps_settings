/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.nfc.BaseNfcEnabler;

/**
 * PaymentSettingsEnabler is a helper to manage the payment feature enable / disable state.
 * It enables / disables payment features based on NFC state, and ensures the summary of the
 * preference is updated.
 */
public class PaymentSettingsEnabler extends BaseNfcEnabler {
    private final CardEmulation mCardEmuManager;
    private final Preference mPreference;
    boolean mIsPaymentAvailable;

    public PaymentSettingsEnabler(Context context, Preference preference) {
        super(context);
        mCardEmuManager = CardEmulation.getInstance(super.mNfcAdapter);
        mIsPaymentAvailable = false;
        mPreference = preference;
    }

    @Override
    protected void handleNfcStateChanged(int newState) {
        switch (newState) {
            case NfcAdapter.STATE_OFF:
                mPreference.setSummary(
                        R.string.nfc_and_payment_settings_payment_off_nfc_off_summary);
                mPreference.setEnabled(false);
                break;
            case NfcAdapter.STATE_ON:
                if (mIsPaymentAvailable) {
                    mPreference.setSummary(null);
                    mPreference.setEnabled(true);
                } else {
                    mPreference.setSummary(
                            R.string.nfc_and_payment_settings_no_payment_installed_summary);

                    mPreference.setEnabled(false);
                }
                break;
        }
    }

    @Override
    public void resume() {
        if (!isNfcAvailable()) {
            return;
        }
        if (mCardEmuManager.getServices(CardEmulation.CATEGORY_PAYMENT).isEmpty()) {
            mIsPaymentAvailable = false;
        } else {
            mIsPaymentAvailable = true;
        }
        super.resume();
    }
}
