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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * The controller of the hearing device controls.
 *
 * <p><b>Note:</b> It is responsible for creating the sub-controllers inside this preference
 * category controller.
 */
public class BluetoothDetailsHearingDeviceController extends BluetoothDetailsController {
    static final String KEY_HEARING_DEVICE_GROUP = "hearing_device_group";

    private final List<BluetoothDetailsController> mControllers = new ArrayList<>();
    private Lifecycle mLifecycle;

    public BluetoothDetailsHearingDeviceController(@NonNull Context context,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mLifecycle = lifecycle;
    }

    @VisibleForTesting
    void setSubControllers(
            BluetoothDetailsHearingDeviceSettingsController hearingDeviceSettingsController) {
        mControllers.clear();
        mControllers.add(hearingDeviceSettingsController);
    }

    @Override
    public boolean isAvailable() {
        return mControllers.stream().anyMatch(BluetoothDetailsController::isAvailable);
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return KEY_HEARING_DEVICE_GROUP;
    }

    @Override
    protected void init(PreferenceScreen screen) {

    }

    @Override
    protected void refresh() {

    }

    /**
     * Initiates the sub controllers controlled by this group controller.
     *
     * <p><b>Note:</b> The caller must call this method when creating this class.
     *
     * @param isLaunchFromHearingDevicePage a boolean that determines if the caller is launch from
     *                                      hearing device page
     */
    void initSubControllers(boolean isLaunchFromHearingDevicePage) {
        mControllers.clear();
        // Don't need to show the entrance to hearing device page when launched from the same page
        if (!isLaunchFromHearingDevicePage) {
            mControllers.add(new BluetoothDetailsHearingDeviceSettingsController(mContext,
                    mFragment, mCachedDevice, mLifecycle));
        }
    }

    @NonNull
    public List<BluetoothDetailsController> getSubControllers() {
        return mControllers;
    }
}
