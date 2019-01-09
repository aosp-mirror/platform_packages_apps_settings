/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.widget.LayoutPreference;

/**
 * This class adds a header with device name and status (connected/disconnected, etc.).
 */
public class AdvancedBluetoothDetailsHeaderController extends BasePreferenceController {

    private LayoutPreference mLayoutPreference;
    private CachedBluetoothDevice mCachedDevice;

    public AdvancedBluetoothDetailsHeaderController(Context context, String prefKey) {
        super(context, prefKey);
    }

    @Override
    public int getAvailabilityStatus() {
        //TODO(b/122460277): decide whether it is available by {@code bluetoothDevice}
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mLayoutPreference = screen.findPreference(getPreferenceKey());
        mLayoutPreference.setVisible(isAvailable());
        refresh();
    }

    public void init(CachedBluetoothDevice cachedBluetoothDevice) {
        mCachedDevice = cachedBluetoothDevice;
    }

    private void refresh() {
        if (mLayoutPreference != null && mCachedDevice != null) {
            final TextView title = mLayoutPreference.findViewById(R.id.entity_header_title);
            title.setText(mCachedDevice.getName());
            final TextView summary = mLayoutPreference.findViewById(R.id.entity_header_summary);
            summary.setText(mCachedDevice.getConnectionSummary());
        }
    }

    @VisibleForTesting
    Drawable createBtBatteryIcon(Context context, int level) {
        final BatteryMeterView.BatteryMeterDrawable drawable =
                new BatteryMeterView.BatteryMeterDrawable(context,
                        context.getColor(R.color.meter_background_color));
        drawable.setBatteryLevel(level);
        drawable.setShowPercent(false);
        drawable.setBatteryColorFilter(new PorterDuffColorFilter(
                Utils.getColorAttrDefaultColor(context, android.R.attr.colorControlNormal),
                PorterDuff.Mode.SRC_IN));

        return drawable;
    }

}
