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
package com.android.settings.nfc;

import android.content.Context;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

public class NfcPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    public static final String KEY_TOGGLE_NFC = "toggle_nfc";
    private final NfcAdapter mNfcAdapter;
    private NfcEnabler mNfcEnabler;
    private NfcAirplaneModeObserver mAirplaneModeObserver;

    public NfcPreferenceController(Context context, String key) {
        super(context, key);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (!isAvailable()) {
            mNfcEnabler = null;
            return;
        }

        final SwitchPreference switchPreference =
                (SwitchPreference) screen.findPreference(getPreferenceKey());

        mNfcEnabler = new NfcEnabler(mContext, switchPreference);

        // Manually set dependencies for NFC when not toggleable.
        if (!isToggleableInAirplaneMode(mContext)) {
            mAirplaneModeObserver = new NfcAirplaneModeObserver(mContext,
                    mNfcAdapter, (Preference) switchPreference);
        }
    }

    @Override
    public boolean isChecked() {
        return mNfcAdapter.isEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            mNfcAdapter.enable();
        } else {
            mNfcAdapter.disable();
        }
        return true;
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return mNfcAdapter != null
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public IntentFilter getIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        filter.addAction(NfcAdapter.EXTRA_ADAPTER_STATE);
        return filter;
    }

    @Override
    public boolean hasAsyncUpdate() {
        return true;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_TOGGLE_NFC);
    }

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

    public static boolean isToggleableInAirplaneMode(Context context) {
        final String toggleable = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        return toggleable != null && toggleable.contains(Settings.Global.RADIO_NFC);
    }
}
