/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

/** Feature provider for the audio sharing related features, */
public interface AudioSharingFeatureProvider {

    /** Create audio sharing device preference controller. */
    @Nullable
    AbstractPreferenceController createAudioSharingDevicePreferenceController(
            @NonNull Context context,
            @Nullable DashboardFragment fragment,
            @Nullable Lifecycle lifecycle);

    /** Create available media device preference controller. */
    AbstractPreferenceController createAvailableMediaDeviceGroupController(
            @NonNull Context context,
            @Nullable DashboardFragment fragment,
            @Nullable Lifecycle lifecycle);

    /**
     * Check if the device match the audio sharing filter.
     *
     * <p>The filter is used to filter device in "Media devices" section.
     */
    boolean isAudioSharingFilterMatched(
            @NonNull CachedBluetoothDevice cachedDevice, LocalBluetoothManager localBtManager);

    /** Handle preference onClick in "Media devices" section. */
    void handleMediaDeviceOnClick(LocalBluetoothManager localBtManager);
}
