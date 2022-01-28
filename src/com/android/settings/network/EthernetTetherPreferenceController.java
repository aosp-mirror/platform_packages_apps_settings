/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import android.content.Context;
import android.net.EthernetManager;
import android.net.TetheringManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.internal.annotations.VisibleForTesting;

/**
 * This controller helps to manage the switch state and visibility of ethernet tether switch
 * preference.
 */
public final class EthernetTetherPreferenceController extends TetherBasePreferenceController {

    private final String mEthernetRegex;
    private final EthernetManager mEthernetManager;
    @VisibleForTesting
    EthernetManager.Listener mEthernetListener;

    public EthernetTetherPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mEthernetRegex = context.getString(
                com.android.internal.R.string.config_ethernet_iface_regex);
        mEthernetManager = (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mEthernetListener = (iface, isAvailable) -> updateState(mPreference);
        final Handler handler = new Handler(Looper.getMainLooper());
        // Executor will execute to post the updateState event to a new handler which is created
        // from the main looper when the {@link EthernetManager.Listener.onAvailabilityChanged}
        // is triggerd.
        mEthernetManager.addListener(mEthernetListener, r -> handler.post(r));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mEthernetManager.removeListener(mEthernetListener);
        mEthernetListener = null;
    }

    @Override
    public boolean shouldEnable() {
        String[] available = mTm.getTetherableIfaces();
        for (String s : available) {
            if (s.matches(mEthernetRegex)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldShow() {
        return !TextUtils.isEmpty(mEthernetRegex);
    }

    @Override
    public int getTetherType() {
        return TetheringManager.TETHERING_ETHERNET;
    }
}
