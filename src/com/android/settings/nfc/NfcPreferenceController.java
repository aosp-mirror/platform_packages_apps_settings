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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.MainSwitchPreference;

import java.io.IOException;

public class NfcPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnResume, OnPause, OnCheckedChangeListener {

    public static final String KEY_TOGGLE_NFC = "toggle_nfc";
    private final NfcAdapter mNfcAdapter;
    private NfcEnabler mNfcEnabler;
    private MainSwitchPreference mPreference;

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

        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.addOnSwitchChangeListener(this);
        mNfcEnabler = new NfcEnabler(mContext, mPreference);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != mNfcAdapter.isEnabled()) {
            setChecked(isChecked);
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
    public Class<? extends SliceBackgroundWorker> getBackgroundWorkerClass() {
        return NfcSliceWorker.class;
    }

    @Override
    public void onResume() {
        if (mNfcEnabler != null) {
            mNfcEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        if (mNfcEnabler != null) {
            mNfcEnabler.pause();
        }
    }

    /**
     * Listener for background changes to NFC.
     *
     * <p>
     * Listen to broadcasts from {@link NfcAdapter}. The worker will call notify changed on the
     * NFC Slice only when the following extras are present in the broadcast:
     * <ul>
     * <li>{@link NfcAdapter#STATE_ON}</li>
     * <li>{@link NfcAdapter#STATE_OFF}</li>
     * </ul>
     */
    public static class NfcSliceWorker extends SliceBackgroundWorker<Void> {

        private static final String TAG = "NfcSliceWorker";

        private static final IntentFilter NFC_FILTER =
                new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);

        private NfcUpdateReceiver mUpdateObserver;

        public NfcSliceWorker(Context context, Uri uri) {
            super(context, uri);
            mUpdateObserver = new NfcUpdateReceiver(this);
        }

        @Override
        protected void onSlicePinned() {
            getContext().registerReceiver(mUpdateObserver, NFC_FILTER);
        }

        @Override
        protected void onSliceUnpinned() {
            getContext().unregisterReceiver(mUpdateObserver);
        }

        @Override
        public void close() throws IOException {
            mUpdateObserver = null;
        }

        public void updateSlice() {
            notifySliceChange();
        }

        public class NfcUpdateReceiver extends BroadcastReceiver {

            private final int NO_EXTRA = -1;

            private final NfcSliceWorker mSliceBackgroundWorker;

            public NfcUpdateReceiver(NfcSliceWorker sliceWorker) {
                mSliceBackgroundWorker = sliceWorker;
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                final int nfcStateExtra = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NO_EXTRA);

                // Do nothing if state change is empty, or an intermediate step.
                if ((nfcStateExtra == NO_EXTRA)
                        || (nfcStateExtra == NfcAdapter.STATE_TURNING_ON)
                        || (nfcStateExtra == NfcAdapter.STATE_TURNING_OFF)) {
                    Log.d(TAG, "Transitional update, dropping broadcast");
                    return;
                }

                Log.d(TAG, "Nfc broadcast received, updating Slice.");
                mSliceBackgroundWorker.updateSlice();
            }
        }
    }
}
