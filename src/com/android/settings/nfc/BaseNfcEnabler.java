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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;

/**
 * BaseNfcEnabler is a abstract helper to manage the Nfc state for Nfc and Android Beam
 * preference. It will receive intent and update state to ensure preference show correct state.
 */
public abstract class BaseNfcEnabler {
    protected final Context mContext;
    protected final NfcAdapter mNfcAdapter;
    private final IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF));
            }
        }
    };

    public BaseNfcEnabler(Context context) {
        mContext = context;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);

        if (!isNfcAvailable()) {
            // NFC is not supported
            mIntentFilter = null;
            return;
        }
        mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
    }

    public void resume() {
        if (!isNfcAvailable()) {
            return;
        }
        handleNfcStateChanged(mNfcAdapter.getAdapterState());
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    public void pause() {
        if (!isNfcAvailable()) {
            return;
        }
        mContext.unregisterReceiver(mReceiver);
    }

    public boolean isNfcAvailable() {
        return mNfcAdapter != null;
    }

    protected abstract void handleNfcStateChanged(int newState);
}
