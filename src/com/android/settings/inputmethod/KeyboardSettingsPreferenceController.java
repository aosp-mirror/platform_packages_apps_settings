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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.FeatureFlagUtils;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.Settings.PhysicalKeyboardActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.inputmethod.PhysicalKeyboardFragment.HardKeyboardDeviceInfo;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import java.util.List;

public class KeyboardSettingsPreferenceController extends BasePreferenceController {

    private Context mContext;
    private CachedBluetoothDevice mCachedDevice;
    private Activity mActivity;

    public KeyboardSettingsPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
    }

    public void init(@NonNull CachedBluetoothDevice cachedDevice, @NonNull Activity activity) {
        mCachedDevice = cachedDevice;
        mActivity = activity;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }

        final Intent intent = new Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS);
        intent.setClass(mContext, PhysicalKeyboardActivity.class);
        intent.putExtra(PhysicalKeyboardFragment.EXTRA_BT_ADDRESS, mCachedDevice.getAddress());
        mActivity.startActivityForResult(intent, 0);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        final List<HardKeyboardDeviceInfo> newHardKeyboards =
                PhysicalKeyboardFragment.getHardKeyboards(mContext);
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
}
