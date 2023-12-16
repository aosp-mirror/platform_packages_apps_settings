/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

public abstract class AudioSharingBasePreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private final BluetoothAdapter mBluetoothAdapter;
    private final LocalBluetoothManager mBtManager;
    protected final LocalBluetoothLeBroadcast mBroadcast;
    protected Preference mPreference;

    public AudioSharingBasePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtManager = Utils.getLocalBtManager(context);
        mBroadcast =
                mBtManager == null
                        ? null
                        : mBtManager.getProfileManager().getLeAudioBroadcastProfile();
    }

    @Override
    public int getAvailabilityStatus() {
        return AudioSharingUtils.isFeatureEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (isAvailable()) {
            updateVisibility();
        }
    }

    /** Update the visibility of the preference. */
    protected void updateVisibility() {
        if (mPreference != null) {
            var unused =
                    ThreadUtils.postOnBackgroundThread(
                            () -> {
                                boolean isVisible = isBroadcasting() && isBluetoothStateOn();
                                ThreadUtils.postOnMainThread(
                                        () -> mPreference.setVisible(isVisible));
                            });
        }
    }

    protected boolean isBroadcasting() {
        return mBroadcast != null && mBroadcast.isEnabled(null);
    }

    protected boolean isBluetoothStateOn() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }
}
