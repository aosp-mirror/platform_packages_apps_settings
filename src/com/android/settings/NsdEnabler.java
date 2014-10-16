/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.nsd.NsdManager;
import android.preference.Preference;

import android.preference.SwitchPreference;

/**
 * NsdEnabler is a helper to manage network service discovery on/off checkbox state.
 */
public class NsdEnabler implements Preference.OnPreferenceChangeListener {
    private final Context mContext;
    private final SwitchPreference mSwitchPreference;
    private final IntentFilter mIntentFilter;
    private NsdManager mNsdManager;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NsdManager.ACTION_NSD_STATE_CHANGED.equals(action)) {
                handleNsdStateChanged(intent.getIntExtra(NsdManager.EXTRA_NSD_STATE,
                        NsdManager.NSD_STATE_DISABLED));
            }
        }
    };

    public NsdEnabler(Context context, SwitchPreference pref) {
        mContext = context;
        mSwitchPreference = pref;
        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
        mIntentFilter = new IntentFilter(NsdManager.ACTION_NSD_STATE_CHANGED);
    }

    public void resume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitchPreference.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mSwitchPreference.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {

        final boolean desiredState = (Boolean) value;
        mSwitchPreference.setEnabled(false);
        mNsdManager.setEnabled(desiredState);
        return false;
    }

    private void handleNsdStateChanged(int newState) {
        switch (newState) {
            case NsdManager.NSD_STATE_DISABLED:
                mSwitchPreference.setChecked(false);
                mSwitchPreference.setEnabled(true);
                break;
            case NsdManager.NSD_STATE_ENABLED:
                mSwitchPreference.setChecked(true);
                mSwitchPreference.setEnabled(true);
                break;
        }
    }
}
