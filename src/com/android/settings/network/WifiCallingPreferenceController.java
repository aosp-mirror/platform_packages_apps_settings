/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.telephony.TelephonyManager;

import com.android.ims.ImsManager;
import com.android.settings.WifiCallingSettings;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class WifiCallingPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_WFC_SETTINGS = "wifi_calling_settings";
    private TelephonyManager mTm;

    public WifiCallingPreferenceController(Context context) {
        super(context);
        mTm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(WifiCallingSettings.getWfcModeSummary(
                mContext, ImsManager.getWfcMode(mContext, mTm.isNetworkRoaming())));
    }

    @Override
    public boolean isAvailable() {
        return ImsManager.isWfcEnabledByPlatform(mContext)
                && ImsManager.isWfcProvisionedOnDevice(mContext);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_WFC_SETTINGS;
    }
}
