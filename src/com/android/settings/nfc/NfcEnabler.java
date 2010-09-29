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

import com.android.settings.R;
import com.trustedlogic.trustednfc.android.NfcException;
import com.trustedlogic.trustednfc.android.NfcManager;
import android.content.Context;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;

/**
 * NfcEnabler is a helper to manage the Nfc on/off checkbox preference. It is
 * turns on/off Nfc and ensures the summary of the preference reflects the
 * current state.
 */
public class NfcEnabler implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "NfcEnabler";

    private final Context mContext;
    private final CheckBoxPreference mCheckbox;
    private final NfcManager mNfcManager;

    private boolean mNfcState;

    public NfcEnabler(Context context, CheckBoxPreference checkBoxPreference) {
        mContext = context;
        mCheckbox = checkBoxPreference;
        mNfcManager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);

        if (mNfcManager == null) {
            // NFC is not supported
            mCheckbox.setEnabled(false);
        }
    }

    public void resume() {
        if (mNfcManager == null) {
            return;
        }
		mCheckbox.setOnPreferenceChangeListener(this);
		mNfcState = Settings.System.getInt(mContext.getContentResolver(),
		        Settings.System.NFC_ON, 0) != 0;
		updateUi();
    }

    public void pause() {
        if (mNfcManager == null) {
            return;
        }
        mCheckbox.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn on/off Nfc
        mNfcState = (Boolean) value;
        setEnabled();

        return false;
    }

    private void setEnabled() {
        if (mNfcState) {
            try {
                mNfcManager.enable();
            } catch (NfcException e) {
                Log.w(TAG, "NFC enabling failed: " + e.getMessage());
				mNfcState = false;
            }

        } else {
            try {
                mNfcManager.disable();
            } catch (NfcException e) {
                Log.w(TAG, "NFC disabling failed: " + e.getMessage());
				mNfcState = true;
            }
        }
		updateUi();
	}

    private void updateUi() {
        mCheckbox.setChecked(mNfcState);
        if (mNfcState) {
            mCheckbox.setSummary(R.string.nfc_quick_toggle_summary);
        } else {
            mCheckbox.setSummary("");
        }
    }
}