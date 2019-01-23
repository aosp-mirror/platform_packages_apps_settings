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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.widget.LayoutPreference;

/**
 * This class adds a header with device name and status (connected/disconnected, etc.).
 */
public class AdvancedBluetoothDetailsHeaderController extends BasePreferenceController {

    @VisibleForTesting
    LayoutPreference mLayoutPreference;
    private CachedBluetoothDevice mCachedDevice;

    public AdvancedBluetoothDetailsHeaderController(Context context, String prefKey) {
        super(context, prefKey);
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean unthetheredHeadset = Utils.getBooleanMetaData(mCachedDevice.getDevice(),
                BluetoothDevice.METADATA_IS_UNTHETHERED_HEADSET);
        return unthetheredHeadset ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
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

    @VisibleForTesting
    void refresh() {
        if (mLayoutPreference != null && mCachedDevice != null) {
            final TextView title = mLayoutPreference.findViewById(R.id.entity_header_title);
            title.setText(mCachedDevice.getName());
            final TextView summary = mLayoutPreference.findViewById(R.id.entity_header_summary);
            summary.setText(mCachedDevice.getConnectionSummary());

            updateSubLayout(mLayoutPreference.findViewById(R.id.layout_left),
                    BluetoothDevice.METADATA_UNTHETHERED_LEFT_ICON,
                    BluetoothDevice.METADATA_UNTHETHERED_LEFT_BATTERY,
                    R.string.bluetooth_left_name);

            updateSubLayout(mLayoutPreference.findViewById(R.id.layout_middle),
                    BluetoothDevice.METADATA_UNTHETHERED_CASE_ICON,
                    BluetoothDevice.METADATA_UNTHETHERED_CASE_BATTERY,
                    R.string.bluetooth_middle_name);

            updateSubLayout(mLayoutPreference.findViewById(R.id.layout_right),
                    BluetoothDevice.METADATA_UNTHETHERED_RIGHT_ICON,
                    BluetoothDevice.METADATA_UNTHETHERED_RIGHT_BATTERY,
                    R.string.bluetooth_right_name);
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
                com.android.settings.Utils.getColorAttrDefaultColor(context,
                        android.R.attr.colorControlNormal),
                PorterDuff.Mode.SRC_IN));

        return drawable;
    }

    private void updateSubLayout(LinearLayout linearLayout, int iconMetaKey, int batteryMetaKey,
            int titleResId) {
        if (linearLayout == null) {
            return;
        }
        final BluetoothDevice bluetoothDevice = mCachedDevice.getDevice();
        final String iconUri = Utils.getStringMetaData(bluetoothDevice, iconMetaKey);
        if (iconUri != null) {
            final ImageView imageView = linearLayout.findViewById(R.id.header_icon);
            final IconCompat iconCompat = IconCompat.createWithContentUri(iconUri);
            imageView.setImageBitmap(iconCompat.getBitmap());
        }

        final int batteryLevel = Utils.getIntMetaData(bluetoothDevice, batteryMetaKey);
        if (batteryLevel != Utils.META_INT_ERROR) {
            final ImageView imageView = linearLayout.findViewById(R.id.bt_battery_icon);
            imageView.setImageDrawable(createBtBatteryIcon(mContext, batteryLevel));
            final TextView textView = linearLayout.findViewById(R.id.bt_battery_summary);
            textView.setText(com.android.settings.Utils.formatPercentage(batteryLevel));
        }

        final TextView textView = linearLayout.findViewById(R.id.header_title);
        textView.setText(titleResId);
    }
}
