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

package com.android.settings.bluetooth;


import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * This class adds related tools preference.
 */
public class BluetoothDetailsRelatedToolsController extends BluetoothDetailsController{
    private static final String KEY_RELATED_TOOLS_GROUP = "bluetooth_related_tools";
    private static final String KEY_LIVE_CAPTION = "live_caption";

    public BluetoothDetailsRelatedToolsController(Context context,
            PreferenceFragmentCompat fragment, CachedBluetoothDevice device, Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return mCachedDevice.isHearingAidDevice();
    }

    @Override
    protected void init(PreferenceScreen screen) {
        if (!mCachedDevice.isHearingAidDevice()) {
            return;
        }

        final PreferenceCategory preferenceCategory = screen.findPreference(getPreferenceKey());
        final Preference liveCaptionPreference = screen.findPreference(KEY_LIVE_CAPTION);
        if (!liveCaptionPreference.isVisible()) {
            preferenceCategory.removePreference(liveCaptionPreference);
        }

        if (preferenceCategory.getPreferenceCount() == 0) {
            screen.removePreference(preferenceCategory);
        }
    }

    @Override
    protected void refresh() {}

    @Override
    public String getPreferenceKey() {
        return KEY_RELATED_TOOLS_GROUP;
    }
}
