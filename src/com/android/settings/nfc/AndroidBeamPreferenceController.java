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
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AndroidBeamPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    public static final String KEY_ANDROID_BEAM_SETTINGS = "android_beam_settings";
    private final NfcAdapter mNfcAdapter;
    private AndroidBeamEnabler mAndroidBeamEnabler;

    public AndroidBeamPreferenceController(Context context, String key) {
        super(context, key);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (!isAvailable()) {
            mAndroidBeamEnabler = null;
            return;
        }

        final RestrictedPreference restrictedPreference = screen.findPreference(getPreferenceKey());
        mAndroidBeamEnabler = new AndroidBeamEnabler(mContext, restrictedPreference);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        PackageManager pm = mContext.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC_BEAM)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return mNfcAdapter != null
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onResume() {
        if (mAndroidBeamEnabler != null) {
            mAndroidBeamEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        if (mAndroidBeamEnabler != null) {
            mAndroidBeamEnabler.pause();
        }
    }
}
