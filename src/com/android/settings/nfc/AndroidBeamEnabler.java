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

package com.android.settings.nfc;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

/**
 * AndroidBeanEnabler is a helper to manage the Android Beam preference. It turns on/off
 * Android Beam and ensures the summary of the preference reflects the current state.
 */
public class AndroidBeamEnabler extends BaseNfcEnabler {
    private final boolean mBeamDisallowedBySystem;
    private final RestrictedPreference mPreference;

    public AndroidBeamEnabler(Context context, RestrictedPreference preference) {
        super(context);
        mPreference = preference;
        mBeamDisallowedBySystem = RestrictedLockUtilsInternal.hasBaseUserRestriction(context,
                UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId());
        if (!isNfcAvailable()) {
            // NFC is not supported
            mPreference.setEnabled(false);
            return;
        }
        if (mBeamDisallowedBySystem) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    protected void handleNfcStateChanged(int newState) {
        switch (newState) {
            case NfcAdapter.STATE_OFF:
                mPreference.setEnabled(false);
                mPreference.setSummary(R.string.nfc_disabled_summary);
                break;
            case NfcAdapter.STATE_ON:
                if (mBeamDisallowedBySystem) {
                    mPreference.setDisabledByAdmin(null);
                    mPreference.setEnabled(false);
                } else {
                    mPreference.checkRestrictionAndSetDisabled(UserManager.DISALLOW_OUTGOING_BEAM);
                }
                if (mNfcAdapter.isNdefPushEnabled() && mPreference.isEnabled()) {
                    mPreference.setSummary(R.string.android_beam_on_summary);
                } else {
                    mPreference.setSummary(R.string.android_beam_off_summary);
                }
                break;
            case NfcAdapter.STATE_TURNING_ON:
                mPreference.setEnabled(false);
                break;
            case NfcAdapter.STATE_TURNING_OFF:
                mPreference.setEnabled(false);
                break;
        }
    }
}
