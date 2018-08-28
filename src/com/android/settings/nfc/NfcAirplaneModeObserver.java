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
import android.database.ContentObserver;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

/**
 * NfcAirplaneModeObserver is a helper to manage the Nfc on/off when airplane mode status
 * is changed.
 */
public class NfcAirplaneModeObserver extends ContentObserver {

    private final Context mContext;
    private final NfcAdapter mNfcAdapter;
    private final Preference mPreference;
    private int mAirplaneMode;

    @VisibleForTesting
    final static Uri AIRPLANE_MODE_URI =
            Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON);

    public NfcAirplaneModeObserver(Context context, NfcAdapter nfcAdapter, Preference preference) {
        super(new Handler(Looper.getMainLooper()));
        mContext = context;
        mNfcAdapter = nfcAdapter;
        mPreference = preference;
        updateNfcPreference();
    }

    public void register() {
        mContext.getContentResolver().registerContentObserver(AIRPLANE_MODE_URI, false, this);
    }

    public void unregister() {
        mContext.getContentResolver().unregisterContentObserver(this);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        updateNfcPreference();
    }

    private void updateNfcPreference() {
        final int airplaneMode = Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, mAirplaneMode);
        if (airplaneMode == mAirplaneMode) {
            return;
        }

        mAirplaneMode = airplaneMode;
        boolean toggleable = mAirplaneMode != 1;
        if (toggleable) {
            mNfcAdapter.enable();
        } else {
            mNfcAdapter.disable();
        }
        mPreference.setEnabled(toggleable);
    }
}
