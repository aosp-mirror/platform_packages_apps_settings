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

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.utils.ThreadUtils;

import java.util.HashSet;

/**
 * This controller helps to manage the switch state and visibility of ethernet tether switch
 * preference.
 */
public final class EthernetTetherPreferenceController extends TetherBasePreferenceController {

    private final HashSet<String> mAvailableInterfaces = new HashSet<>();
    private final EthernetManager mEthernetManager;

    @VisibleForTesting
    EthernetManager.InterfaceStateListener mEthernetListener;

    public EthernetTetherPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mEthernetManager = context.getSystemService(EthernetManager.class);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mEthernetListener = (iface, state, role, configuration) -> {
            if (state != EthernetManager.STATE_ABSENT) {
                mAvailableInterfaces.add(iface);
            } else {
                mAvailableInterfaces.remove(iface);
            }
            updateState(mPreference);
        };
        final Handler handler = new Handler(Looper.getMainLooper());
        // Executor will execute to post the updateState event to a new handler which is created
        // from the main looper when the {@link EthernetManager.Listener.onAvailabilityChanged}
        // is triggerd.
        if (mEthernetManager != null) {
            mEthernetManager.addInterfaceStateListener(r -> handler.post(r), mEthernetListener);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        if (mEthernetManager != null) {
            mEthernetManager.removeInterfaceStateListener(mEthernetListener);
        }
    }

    @Override
    public boolean shouldEnable() {
        ThreadUtils.ensureMainThread();
        String[] available = mTm.getTetherableIfaces();
        for (String s : available) {
            if (mAvailableInterfaces.contains(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldShow() {
        return mEthernetManager != null;
    }

    @Override
    public int getTetherType() {
        return TetheringManager.TETHERING_ETHERNET;
    }

}
