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
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.SwitchPreference;

/**
 * NfcEnabler is a helper to manage the Nfc on/off checkbox preference. It turns on/off Nfc
 * and ensures the summary of the preference reflects the current state.
 */
public class NfcEnabler extends BaseNfcEnabler {
    private final SwitchPreference mPreference;

    public NfcEnabler(Context context, SwitchPreference preference) {
        super(context);
        mPreference = preference;
    }

    @Override
    protected void handleNfcStateChanged(int newState) {
        switch (newState) {
            case NfcAdapter.STATE_OFF:
                mPreference.setChecked(false);
                mPreference.setEnabled(isToggleable());
                break;
            case NfcAdapter.STATE_ON:
                mPreference.setChecked(true);
                mPreference.setEnabled(true);
                break;
            case NfcAdapter.STATE_TURNING_ON:
                mPreference.setChecked(true);
                mPreference.setEnabled(false);
                break;
            case NfcAdapter.STATE_TURNING_OFF:
                mPreference.setChecked(false);
                mPreference.setEnabled(false);
                break;
        }
    }

    @VisibleForTesting
    boolean isToggleable() {
        if (NfcPreferenceController.isToggleableInAirplaneMode(mContext)
                || !NfcPreferenceController.shouldTurnOffNFCInAirplaneMode(mContext)) {
            return true;
        }
        final int airplaneMode = Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        return airplaneMode != 1;
    }
}
