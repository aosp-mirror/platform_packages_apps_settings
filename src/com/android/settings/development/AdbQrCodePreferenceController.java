/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.debug.IAdbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;

/**
 * Controller for the "Pair device with QR code" preference in the Wireless debugging
 * fragment.
 */
public class AdbQrCodePreferenceController extends BasePreferenceController {
    private static final String TAG = "AdbQrCodePrefCtrl";

    private IAdbManager mAdbManager;
    private Fragment mParentFragment;

    public AdbQrCodePreferenceController(Context context, String key) {
        super(context, key);

        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(
                Context.ADB_SERVICE));
    }

    public void setParentFragment(Fragment parent) {
        mParentFragment = parent;
    }

    @Override
    public int getAvailabilityStatus() {
        try {
            return mAdbManager.isAdbWifiQrSupported() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to check if adb wifi QR code scanning is supported.", e);
        }

        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        new SubSettingLauncher(preference.getContext())
                .setDestination(AdbQrcodeScannerFragment.class.getName())
                .setSourceMetricsCategory(SettingsEnums.SETTINGS_ADB_WIRELESS)
                .setResultListener(mParentFragment,
                    WirelessDebuggingFragment.PAIRING_DEVICE_REQUEST)
                .launch();
        return true;
    }
}
