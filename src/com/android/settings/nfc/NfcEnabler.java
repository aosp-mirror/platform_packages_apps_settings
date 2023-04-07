/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.settingslib.widget.MainSwitchPreference;

/**
 * NfcEnabler is a helper to manage the Nfc on/off checkbox preference. It turns on/off Nfc
 * and ensures the summary of the preference reflects the current state.
 */
public class NfcEnabler extends BaseNfcEnabler {
    private final MainSwitchPreference mPreference;

    public NfcEnabler(Context context, MainSwitchPreference preference) {
        super(context);
        mPreference = preference;
    }

    @Override
    protected void handleNfcStateChanged(int newState) {
        switch (newState) {
            case NfcAdapter.STATE_OFF:
                mPreference.updateStatus(false);
                mPreference.setEnabled(true);
                break;
            case NfcAdapter.STATE_ON:
                mPreference.updateStatus(true);
                mPreference.setEnabled(true);
                break;
            case NfcAdapter.STATE_TURNING_ON:
                mPreference.updateStatus(true);
                mPreference.setEnabled(false);
                break;
            case NfcAdapter.STATE_TURNING_OFF:
                mPreference.updateStatus(false);
                mPreference.setEnabled(false);
                break;
        }
    }
}
