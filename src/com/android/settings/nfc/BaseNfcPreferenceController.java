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
import android.nfc.NfcManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

public abstract class BaseNfcPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    protected BaseNfcEnabler mNfcEnabler;
    private NfcAdapter mNfcAdapter;
    private int mAirplaneMode;
    private AirplaneModeObserver mAirplaneModeObserver;
    protected Preference mPreference;

    public BaseNfcPreferenceController(Context context) {
        super(context);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        if (!isAvailable()) {
            mNfcEnabler = null;
            return;
        }

        mPreference = screen.findPreference(getPreferenceKey());

        // Manually set dependencies for NFC when not toggleable.
        if (!isToggleableInAirplaneMode(mContext)) {
            mAirplaneModeObserver = new AirplaneModeObserver();
            updateNfcPreference();
        }
    }

    @Override
    public void updateNonIndexableKeys(List<String> keys) {
        if (isAvailable()) {
            keys.add(getPreferenceKey());
        }
    }

    @Override
    public boolean isAvailable() {
        return mNfcAdapter != null;
    }

    public abstract String getPreferenceKey();

    @Override
    public void onResume() {
        if (mAirplaneModeObserver != null) {
            mAirplaneModeObserver.register();
        }
        if (mNfcEnabler != null) {
            mNfcEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        if (mAirplaneModeObserver != null) {
            mAirplaneModeObserver.unregister();
        }
        if (mNfcEnabler != null) {
            mNfcEnabler.pause();
        }
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

    public static boolean isToggleableInAirplaneMode(Context context) {
        String toggleable = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        return toggleable != null && toggleable.contains(Settings.Global.RADIO_NFC);
    }

    private final class AirplaneModeObserver extends ContentObserver {
        private final Uri AIRPLANE_MODE_URI =
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON);

        private AirplaneModeObserver() {
            super(new Handler());
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
    }

}
