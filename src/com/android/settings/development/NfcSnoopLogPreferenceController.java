/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller to control NFCSNOOP data payload
 */
public class NfcSnoopLogPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "NfcSnoopLog";
    private static final String NFC_NFCSNOOP_LOG_KEY =
            "nfc_snoop_log";
    @VisibleForTesting
    static final String NFC_NFCSNOOP_LOG_MODE_PROPERTY =
            "persist.nfc.snoop_log_mode";
    @VisibleForTesting
    static final String NFCSNOOP_MODE_FILTERED = "filtered";
    @VisibleForTesting
    static final String NFCSNOOP_MODE_FULL = "full";

    @VisibleForTesting
    boolean mChanged = false;

    private final DevelopmentSettingsDashboardFragment mFragment;

    public NfcSnoopLogPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public String getPreferenceKey() {
        return NFC_NFCSNOOP_LOG_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        NfcRebootDialog.show(mFragment);
        mChanged = true;
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        try {
            final String currentValue = SystemProperties.get(NFC_NFCSNOOP_LOG_MODE_PROPERTY);
            ((TwoStatePreference) mPreference).setChecked(currentValue.equals(NFCSNOOP_MODE_FULL));
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to get nfc system property: " + e.getMessage());
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        try {
            SystemProperties.set(NFC_NFCSNOOP_LOG_MODE_PROPERTY, NFCSNOOP_MODE_FILTERED);
            ((TwoStatePreference) mPreference).setChecked(false);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to set nfc system property: " + e.getMessage());
        }
    }

    /**
     * Check whether the current setting is the default value or not.
     */
    public boolean isDefaultValue() {
        try {
            final String currentValue = SystemProperties.get(NFC_NFCSNOOP_LOG_MODE_PROPERTY);
            return !currentValue.equals(NFCSNOOP_MODE_FULL);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to get nfc system property: " + e.getMessage());
        }
        return true;
    }

    /**
     * Called when the NfcRebootDialog confirm is clicked.
     */
    public void onNfcRebootDialogConfirmed() {
        if (!mChanged) {
            return;
        }
        try {
            final String currentValue =
                    SystemProperties.get(NFC_NFCSNOOP_LOG_MODE_PROPERTY, NFCSNOOP_MODE_FILTERED);
            if (currentValue.equals(NFCSNOOP_MODE_FILTERED)) {
                SystemProperties.set(NFC_NFCSNOOP_LOG_MODE_PROPERTY, NFCSNOOP_MODE_FULL);
            } else {
                SystemProperties.set(NFC_NFCSNOOP_LOG_MODE_PROPERTY, NFCSNOOP_MODE_FILTERED);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to set nfc system property: " + e.getMessage());
        }

    }

    /**
     * Called when the NfcRebootDialog cancel is clicked.
     */
    public void onNfcRebootDialogCanceled() {
        mChanged = false;
    }
}
