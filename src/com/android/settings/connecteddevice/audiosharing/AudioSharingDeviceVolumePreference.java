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
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

public class AudioSharingDeviceVolumePreference extends SeekBarPreference {
    private static final String TAG = "AudioSharingDeviceVolumePreference";
    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 255;

    protected SeekBar mSeekBar;
    private final LocalBluetoothManager mLocalBtManager;
    private final CachedBluetoothDevice mCachedDevice;
    private final SeekBar.OnSeekBarChangeListener mListener;

    public AudioSharingDeviceVolumePreference(
            Context context,
            @NonNull CachedBluetoothDevice device,
            SeekBar.OnSeekBarChangeListener listener) {
        super(context);
        setLayoutResource(R.layout.preference_volume_slider);
        mLocalBtManager = Utils.getLocalBtManager(context);
        mCachedDevice = device;
        mListener = listener;
    }

    @Nullable
    public CachedBluetoothDevice getCachedDevice() {
        return mCachedDevice;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mSeekBar.setMax(MAX_VOLUME);
        mSeekBar.setMin(MIN_VOLUME);
        mSeekBar.setOnSeekBarChangeListener(mListener);
    }

    /** Set the progress bar to target progress */
    public void setProgress(int progress) {
        if (mSeekBar != null) {
            mSeekBar.setProgress(progress);
        }
    }
}
