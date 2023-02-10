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

import static com.android.settings.bluetooth.BluetoothDeviceDetailsFragment.FEATURE_AUDIO_ROUTING_ORDER;
import static com.android.settings.bluetooth.BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS;

import android.content.Context;
import android.os.Bundle;
import android.util.FeatureFlagUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * The controller of the audio routing in the bluetooth detail settings.
 */
public class BluetoothDetailsAudioRoutingController extends BluetoothDetailsController  {

    private static final String KEY_FEATURE_CONTROLS_GROUP = "feature_controls_group";
    @VisibleForTesting
    static final String KEY_AUDIO_ROUTING = "audio_routing";

    public BluetoothDetailsAudioRoutingController(Context context,
            PreferenceFragmentCompat fragment, CachedBluetoothDevice device, Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return mCachedDevice.isHearingAidDevice() && FeatureFlagUtils.isEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING);
    }

    @Override
    protected void init(PreferenceScreen screen) {
        if (!mCachedDevice.isHearingAidDevice()) {
            return;
        }

        final PreferenceCategory prefCategory = screen.findPreference(getPreferenceKey());
        final Preference pref = createAudioRoutingPreference(prefCategory.getContext());
        pref.setOrder(FEATURE_AUDIO_ROUTING_ORDER);
        prefCategory.addPreference(pref);
    }

    @Override
    protected void refresh() {}

    @Override
    public String getPreferenceKey() {
        return KEY_FEATURE_CONTROLS_GROUP;
    }

    private Preference createAudioRoutingPreference(Context context) {
        final Preference preference = new Preference(context);

        preference.setKey(KEY_AUDIO_ROUTING);
        preference.setTitle(context.getString(R.string.bluetooth_audio_routing_title));
        preference.setSummary(context.getString(R.string.bluetooth_audio_routing_summary));
        final Bundle extras = preference.getExtras();
        extras.putString(KEY_DEVICE_ADDRESS, mCachedDevice.getAddress());
        preference.setFragment(BluetoothDetailsAudioRoutingFragment.class.getName());

        return preference;
    }
}
