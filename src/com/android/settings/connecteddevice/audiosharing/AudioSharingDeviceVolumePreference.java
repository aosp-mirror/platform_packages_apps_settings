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
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

public class AudioSharingDeviceVolumePreference extends SeekBarPreference {
    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 255;

    protected SeekBar mSeekBar;
    private final CachedBluetoothDevice mCachedDevice;

    public AudioSharingDeviceVolumePreference(
            Context context, @NonNull CachedBluetoothDevice device) {
        super(context);
        setLayoutResource(R.layout.preference_volume_slider);
        mCachedDevice = device;
    }

    @Nullable
    public CachedBluetoothDevice getCachedDevice() {
        return mCachedDevice;
    }

    /**
     * Initialize {@link AudioSharingDeviceVolumePreference}.
     * Need to be called after creating the preference.
     */
    public void initialize() {
        setMax(MAX_VOLUME);
        setMin(MIN_VOLUME);
    }
}
