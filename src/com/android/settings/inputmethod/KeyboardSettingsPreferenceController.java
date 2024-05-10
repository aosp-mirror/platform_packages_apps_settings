/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.FeatureFlagUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.inputmethod.PhysicalKeyboardFragment.HardKeyboardDeviceInfo;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import java.util.List;

public class KeyboardSettingsPreferenceController extends BasePreferenceController {

    private CachedBluetoothDevice mCachedDevice;

    public KeyboardSettingsPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void init(@NonNull CachedBluetoothDevice cachedDevice) {
        mCachedDevice = cachedDevice;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }
        List<HardKeyboardDeviceInfo> newHardKeyboards = getHardKeyboardList();
        for (HardKeyboardDeviceInfo hardKeyboardDeviceInfo : newHardKeyboards) {
            if (mCachedDevice.getAddress().equals(hardKeyboardDeviceInfo.mBluetoothAddress)) {
                Intent intent = new Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS);
                intent.putExtra(
                        Settings.EXTRA_ENTRYPOINT, SettingsEnums.CONNECTED_DEVICES_SETTINGS);
                intent.putExtra(
                        Settings.EXTRA_INPUT_DEVICE_IDENTIFIER,
                        hardKeyboardDeviceInfo.mDeviceIdentifier);
                mContext.startActivity(intent);
                break;
            }
        }
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        List<HardKeyboardDeviceInfo> newHardKeyboards = getHardKeyboardList();
        if (FeatureFlagUtils.isEnabled(mContext, FeatureFlagUtils.SETTINGS_NEW_KEYBOARD_UI)
                && !newHardKeyboards.isEmpty()) {
            for (HardKeyboardDeviceInfo hardKeyboardDeviceInfo : newHardKeyboards) {
                if (mCachedDevice.getAddress() != null
                        && hardKeyboardDeviceInfo.mBluetoothAddress != null
                        && mCachedDevice.getAddress().equals(
                                hardKeyboardDeviceInfo.mBluetoothAddress)) {
                    return AVAILABLE;
                }
            }
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @VisibleForTesting
    List<HardKeyboardDeviceInfo> getHardKeyboardList() {
        return PhysicalKeyboardFragment.getHardKeyboards(mContext);
    }
}
