/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixinCompat;

/**
 * This class adds the device MAC address to a footer.
 */
public class BluetoothDetailsMacAddressController extends BluetoothDetailsController {
    FooterPreferenceMixinCompat mFooterPreferenceMixin;
    FooterPreference mFooterPreference;

    public BluetoothDetailsMacAddressController(Context context,
            PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mFooterPreferenceMixin = new FooterPreferenceMixinCompat(fragment, lifecycle);
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mFooterPreference = mFooterPreferenceMixin.createFooterPreference();
        mFooterPreference.setTitle(mContext.getString(
                R.string.bluetooth_device_mac_address, mCachedDevice.getAddress()));
    }

    @Override
    protected void refresh() {
        mFooterPreference.setTitle(mContext.getString(
                R.string.bluetooth_device_mac_address, mCachedDevice.getAddress()));
    }

    @Override
    public String getPreferenceKey() {
        if (mFooterPreference == null) {
            return null;
        }
        return mFooterPreference.getKey();
    }
}