/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.nfc;

import android.content.Context;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;

import com.android.settings.R;

public class NfcForegroundPreference extends DropDownPreference implements
        PaymentBackend.Callback, Preference.OnPreferenceChangeListener {

    private final PaymentBackend mPaymentBackend;
    public NfcForegroundPreference(Context context, PaymentBackend backend) {
        super(context);
        mPaymentBackend = backend;
        mPaymentBackend.registerCallback(this);

        setTitle(getContext().getString(R.string.nfc_payment_use_default));
        setEntries(new CharSequence[] {
                getContext().getString(R.string.nfc_payment_favor_open),
                getContext().getString(R.string.nfc_payment_favor_default)
        });
        setEntryValues(new CharSequence[] { "1", "0" });
        refresh();
        setOnPreferenceChangeListener(this);
    }

    @Override
    public void onPaymentAppsChanged() {
        refresh();
    }

    void refresh() {
        boolean foregroundMode = mPaymentBackend.isForegroundMode();
        if (foregroundMode) {
            setValue("1");
        } else {
            setValue("0");
        }
        setSummary(getEntry());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String newValueString = (String) newValue;
        setSummary(getEntries()[findIndexOfValue(newValueString)]);
        mPaymentBackend.setForegroundMode(Integer.parseInt(newValueString) != 0);
        return true;
    }
}
