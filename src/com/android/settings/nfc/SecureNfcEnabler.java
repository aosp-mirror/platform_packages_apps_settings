/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.nfc.NfcAdapter;
import android.os.UserManager;

import androidx.preference.SwitchPreference;

import com.android.settings.R;

/**
 * SecureNfcEnabler is a helper to manage the Secure Nfc on/off checkbox preference
 * It turns on/off Secure NFC and ensures the summary of the preference reflects
 * the current state.
 */
public class SecureNfcEnabler extends BaseNfcEnabler {
    private final SwitchPreference mPreference;
    private final UserManager mUserManager;

    public SecureNfcEnabler(Context context, SwitchPreference preference) {
        super(context);
        mPreference = preference;
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    protected void handleNfcStateChanged(int newState) {
        switch (newState) {
            case NfcAdapter.STATE_OFF:
                mPreference.setSummary(R.string.nfc_disabled_summary);
                mPreference.setEnabled(false);
                break;
            case NfcAdapter.STATE_ON:
                mPreference.setSummary(R.string.nfc_secure_toggle_summary);
                mPreference.setChecked(mPreference.isChecked());
                mPreference.setEnabled(isToggleable());
                break;
            case NfcAdapter.STATE_TURNING_ON:
                mPreference.setEnabled(false);
                break;
            case NfcAdapter.STATE_TURNING_OFF:
                mPreference.setEnabled(false);
                break;
        }
    }

    private boolean isToggleable() {
        if (mUserManager.isGuestUser()) {
            return false;
        }
        return true;
    }
}
