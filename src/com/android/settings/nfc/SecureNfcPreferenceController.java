/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.os.UserManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class SecureNfcPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private final NfcAdapter mNfcAdapter;
    private SecureNfcEnabler mSecureNfcEnabler;
    private final UserManager mUserManager;

    public SecureNfcPreferenceController(Context context, String key) {
        super(context, key);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (!isAvailable()) {
            mSecureNfcEnabler = null;
            return;
        }

        final TwoStatePreference switchPreference = screen.findPreference(getPreferenceKey());

        mSecureNfcEnabler = new SecureNfcEnabler(mContext, switchPreference);
    }

    @Override
    public boolean isChecked() {
        return mNfcAdapter.isSecureNfcEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isToggleable()) {
            return mNfcAdapter.enableSecureNfc(isChecked);
        } else {
            return false;
        }
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        if (mNfcAdapter == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return mNfcAdapter.isSecureNfcSupported()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean hasAsyncUpdate() {
        return true;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_connected_devices;
    }

    @Override
    public void onResume() {
        if (mSecureNfcEnabler != null) {
            mSecureNfcEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        if (mSecureNfcEnabler != null) {
            mSecureNfcEnabler.pause();
        }
    }

    private boolean isToggleable() {
        if (!mUserManager.isPrimaryUser()) {
            return false;
        }
        return true;
    }

}
