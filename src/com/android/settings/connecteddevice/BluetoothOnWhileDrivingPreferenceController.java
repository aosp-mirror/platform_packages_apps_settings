/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.connecteddevice;

import android.content.Context;
import android.provider.Settings;
import android.util.FeatureFlagUtils;

import com.android.settings.core.FeatureFlags;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;

/** Handles a toggle for a setting to turn on Bluetooth while driving. * */
public class BluetoothOnWhileDrivingPreferenceController extends TogglePreferenceController
        implements PreferenceControllerMixin {
    static final String KEY_BLUETOOTH_ON_DRIVING = "bluetooth_on_while_driving";

    public BluetoothOnWhileDrivingPreferenceController(Context context) {
        super(context, KEY_BLUETOOTH_ON_DRIVING);
    }

    @Override
    public int getAvailabilityStatus() {
        if (FeatureFlagUtils.isEnabled(mContext, FeatureFlags.BLUETOOTH_WHILE_DRIVING)) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        Settings.Secure.BLUETOOTH_ON_WHILE_DRIVING,
                        0)
                != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final int value = isChecked ? 1 : 0;
        return Settings.Secure.putInt(
                mContext.getContentResolver(), Settings.Secure.BLUETOOTH_ON_WHILE_DRIVING, value);
    }
}
