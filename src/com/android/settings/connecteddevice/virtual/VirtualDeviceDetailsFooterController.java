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

package com.android.settings.connecteddevice.virtual;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Adds footer text on the virtual device details page. */
public class VirtualDeviceDetailsFooterController extends BasePreferenceController {

    private static final String KEY_VIRTUAL_DEVICE_FOOTER = "virtual_device_details_footer";

    @Nullable
    private CharSequence mDeviceName;

    public VirtualDeviceDetailsFooterController(@NonNull Context context) {
        super(context, KEY_VIRTUAL_DEVICE_FOOTER);
    }

    /** One-time initialization when the controller is first created. */
    void init(@NonNull CharSequence deviceName) {
        mDeviceName = deviceName;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        preference.setTitle(mContext.getString(R.string.virtual_device_details_footer_title,
                mDeviceName));
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
