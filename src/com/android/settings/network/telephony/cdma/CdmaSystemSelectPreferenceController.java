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

package com.android.settings.network.telephony.cdma;

import static com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA;
import static com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

/**
 * Preference controller for "System Select"
 */
public class CdmaSystemSelectPreferenceController extends CdmaBasePreferenceController
        implements ListPreference.OnPreferenceChangeListener {

    public CdmaSystemSelectPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setVisible(getAvailabilityStatus() == AVAILABLE);
        final int mode = mTelephonyManager.getCdmaRoamingMode();
        if (mode != TelephonyManager.CDMA_ROAMING_MODE_RADIO_DEFAULT) {
            if (mode == TelephonyManager.CDMA_ROAMING_MODE_HOME
                    || mode == TelephonyManager.CDMA_ROAMING_MODE_ANY) {
                listPreference.setValue(Integer.toString(mode));
            } else {
                resetCdmaRoamingModeToDefault();
            }
        }
        final int settingsNetworkMode = Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                TelephonyManager.DEFAULT_PREFERRED_NETWORK_MODE);
        final boolean enableList = settingsNetworkMode != NETWORK_MODE_LTE_GSM_WCDMA
                && settingsNetworkMode != NETWORK_MODE_NR_LTE_GSM_WCDMA;
        listPreference.setEnabled(enableList);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        int newMode = Integer.parseInt((String) object);
        //TODO(b/117611981): only set it in one place
        if (mTelephonyManager.setCdmaRoamingMode(newMode)) {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.CDMA_ROAMING_MODE, newMode);
            return true;
        }

        return false;
    }

    private void resetCdmaRoamingModeToDefault() {
        final ListPreference listPreference = (ListPreference) mPreference;
        //set the mButtonCdmaRoam
        listPreference.setValue(Integer.toString(TelephonyManager.CDMA_ROAMING_MODE_ANY));
        //set the Settings.System
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.CDMA_ROAMING_MODE,
                TelephonyManager.CDMA_ROAMING_MODE_ANY);
        //Set the Status
        mTelephonyManager.setCdmaRoamingMode(TelephonyManager.CDMA_ROAMING_MODE_ANY);
    }
}
