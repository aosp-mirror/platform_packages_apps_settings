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

package com.android.settings.development;

import android.content.Context;
import android.util.Log;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class CbrsDataSwitchPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private static final String CBRS_DATA_SWITCH_KEY = "cbrs_data_switch";
    private static final String TAG = "CbrsDataSwitchPreferenceController";
    private Context mContext;

    private TelephonyManager mTelephonyManager;

    public CbrsDataSwitchPreferenceController(Context context) {
        super(context);
        mContext = context;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
    }

    @Override
    public String getPreferenceKey() {
        return CBRS_DATA_SWITCH_KEY;
    }

    @Override
    public boolean isAvailable() {
        return mTelephonyManager != null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean state = (Boolean)newValue;
        return mTelephonyManager.setAlternativeNetworkState(state);
    }

    @Override
    public void updateState(Preference preference) {
        boolean state = mTelephonyManager.isAlternativeNetworkEnabled();
        ((SwitchPreference) mPreference).setChecked(state);
    }

}
