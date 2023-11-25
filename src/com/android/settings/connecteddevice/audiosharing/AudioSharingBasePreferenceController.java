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

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

public abstract class AudioSharingBasePreferenceController extends BasePreferenceController {
    private final LocalBluetoothManager mBtManager;
    protected final LocalBluetoothLeBroadcast mBroadcast;
    protected Preference mPreference;

    public AudioSharingBasePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBtManager = Utils.getLocalBtManager(context);
        mBroadcast =
                mBtManager == null
                        ? null
                        : mBtManager.getProfileManager().getLeAudioBroadcastProfile();
    }

    @Override
    public int getAvailabilityStatus() {
        return mBtManager != null && Flags.enableLeAudioSharing()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateVisibility(isBroadcasting());
    }

    /**
     * Update the visibility of the preference.
     *
     * @param isVisible the latest visibility state for the preference.
     */
    public void updateVisibility(boolean isVisible) {
        mPreference.setVisible(isVisible);
    }

    protected boolean isBroadcasting() {
        return mBroadcast != null && mBroadcast.isEnabled(null);
    }
}
