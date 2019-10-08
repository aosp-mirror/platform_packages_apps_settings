/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.specialaccess;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.UserManager;

import com.android.settings.core.BasePreferenceController;

public class DefaultPaymentSettingsPreferenceController extends BasePreferenceController {

    private final NfcAdapter mNfcAdapter;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;

    public DefaultPaymentSettingsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mPackageManager = context.getPackageManager();
        mUserManager = context.getSystemService(UserManager.class);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
                || !mPackageManager.hasSystemFeature(
                        PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!mUserManager.isAdminUser()) {
            return DISABLED_FOR_USER;
        }
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }
}
