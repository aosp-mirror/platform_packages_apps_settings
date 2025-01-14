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

package com.android.settings.bluetooth;

import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.ORDER_AMBIENT_VOLUME;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.bluetooth.AmbientVolumeUiController;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;

/** A {@link BluetoothDetailsController} that manages ambient volume preference. */
public class BluetoothDetailsAmbientVolumePreferenceController extends BluetoothDetailsController
        implements OnStart, OnStop {

    private static final boolean DEBUG = true;
    private static final String TAG = "AmbientPrefController";

    static final String KEY_AMBIENT_VOLUME = "ambient_volume";
    static final String KEY_AMBIENT_VOLUME_SLIDER = "ambient_volume_slider";

    private final LocalBluetoothManager mBluetoothManager;
    @Nullable
    private AmbientVolumePreference mPreference;
    @Nullable
    private AmbientVolumeUiController mAmbientUiController;

    public BluetoothDetailsAmbientVolumePreferenceController(@NonNull Context context,
            @NonNull LocalBluetoothManager manager,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mBluetoothManager = manager;
    }

    @VisibleForTesting
    public BluetoothDetailsAmbientVolumePreferenceController(@NonNull Context context,
            @NonNull LocalBluetoothManager manager,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle,
            @NonNull AmbientVolumeUiController uiController) {
        super(context, fragment, device, lifecycle);
        mBluetoothManager = manager;
        mAmbientUiController = uiController;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        PreferenceCategory deviceControls = screen.findPreference(KEY_HEARING_DEVICE_GROUP);
        if (deviceControls == null) {
            return;
        }
        mPreference = new AmbientVolumePreference(deviceControls.getContext());
        mPreference.setKey(KEY_AMBIENT_VOLUME);
        mPreference.setOrder(ORDER_AMBIENT_VOLUME);
        deviceControls.addPreference(mPreference);

        mAmbientUiController = new AmbientVolumeUiController(mContext, mBluetoothManager,
                mPreference);
        mAmbientUiController.loadDevice(mCachedDevice);
    }

    @Override
    public void onStart() {
        ThreadUtils.postOnBackgroundThread(() -> {
            if (mAmbientUiController != null) {
                mAmbientUiController.start();
            }
        });
    }

    @Override
    public void onResume() {
        refresh();
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onStop() {
        ThreadUtils.postOnBackgroundThread(() -> {
            if (mAmbientUiController != null) {
                mAmbientUiController.stop();
            }
        });
    }

    @Override
    protected void refresh() {
        if (!isAvailable()) {
            return;
        }
        if (mAmbientUiController != null) {
            mAmbientUiController.refresh();
        }
    }

    @Override
    public boolean isAvailable() {
        return mCachedDevice.isHearingDevice() && mCachedDevice.getProfiles().stream().anyMatch(
                profile -> profile instanceof VolumeControlProfile);
    }

    @Nullable
    @Override
    public String getPreferenceKey() {
        return KEY_AMBIENT_VOLUME;
    }
}
