/*
 * Copyright (C) 2023 The Android Open Source Project
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

import com.android.settings.core.BasePreferenceController;

/**
 * A PreferenceController handling the logic for the Nfc Tag App preference
 */
public class NfcTagAppsPreferenceController extends BasePreferenceController {
    private NfcAdapter mNfcAdapter;

    public NfcTagAppsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context.getApplicationContext());
    }

    @Override
    public int getAvailabilityStatus() {
        if (mNfcAdapter != null) {
            return mNfcAdapter.isTagIntentAppPreferenceSupported()
                    ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }
}
