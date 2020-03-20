/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.debug.PairDevice;
import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * An AP preference for the currently connected AP
 */
public class AdbPairedDevicePreference extends Preference {
    private static final String TAG = "AdbPairedDevicePref";

    private PairDevice mPairedDevice;

    // Extract using getSerializable(PAIRED_DEVICE_EXTRA)
    public static final String PAIRED_DEVICE_EXTRA = "paired_device";

    public AdbPairedDevicePreference(PairDevice pairedDevice, Context context) {
        super(context);

        mPairedDevice = pairedDevice;
        setWidgetLayoutResource(getWidgetLayoutResourceId());
        refresh();
    }

    protected int getWidgetLayoutResourceId() {
        return R.layout.preference_widget_gear_optional_background;
    }

    /**
     * Refreshes the preference bound to the paired device previously passed in.
     */
    public void refresh() {
        setTitle(this, mPairedDevice);
    }

    public void setPairedDevice(PairDevice pairedDevice) {
        mPairedDevice = pairedDevice;
    }

    public PairDevice getPairedDevice() {
        return mPairedDevice;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final View gear = holder.findViewById(R.id.settings_button);
        final View gearNoBg = holder.findViewById(R.id.settings_button_no_background);

        gear.setVisibility(View.INVISIBLE);
        gearNoBg.setVisibility(View.VISIBLE);
    }

    static void setTitle(AdbPairedDevicePreference preference,
                         PairDevice pairedDevice) {
        preference.setTitle(pairedDevice.getDeviceName());
        preference.setSummary(pairedDevice.isConnected()
                ? preference.getContext().getText(R.string.adb_wireless_device_connected_summary)
                : "");
    }

    /**
     * Writes the paired devices bound to this preference to the bundle.
     *
     * @param bundle the bundle to write the paired device to
     */
    public void savePairedDeviceToExtras(Bundle bundle) {
        bundle.putParcelable(PAIRED_DEVICE_EXTRA, mPairedDevice);
    }
}

