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
 * limitations under the License
 */
package com.android.settings.bluetooth;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.SettingsSlicesContract;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.connecteddevice.BluetoothDashboardFragment;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceBuilderUtils;

/**
 * Utility class to build a Bluetooth Slice, and handle all associated actions.
 */
public class BluetoothSliceBuilder {

    private static final String TAG = "BluetoothSliceBuilder";

    /**
     * Action notifying a change on the BluetoothSlice.
     */
    public static final String ACTION_BLUETOOTH_SLICE_CHANGED =
            "com.android.settings.bluetooth.action.BLUETOOTH_MODE_CHANGED";

    public static final IntentFilter INTENT_FILTER = new IntentFilter();

    static {
        INTENT_FILTER.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        INTENT_FILTER.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    private BluetoothSliceBuilder() {
    }

    /**
     * Return a Bluetooth Slice bound to {@link CustomSliceRegistry#BLUETOOTH_URI}.
     * <p>
     * Note that you should register a listener for {@link #INTENT_FILTER} to get changes for
     * Bluetooth.
     */
    public static Slice getSlice(Context context) {
        final boolean isBluetoothEnabled = isBluetoothEnabled();
        final CharSequence title = context.getText(R.string.bluetooth_settings);
        final IconCompat icon = IconCompat.createWithResource(context,
                com.android.internal.R.drawable.ic_settings_bluetooth);
        @ColorInt final int color = com.android.settings.Utils.getColorAccent(
                context).getDefaultColor();
        final PendingIntent toggleAction = getBroadcastIntent(context);
        final PendingIntent primaryAction = getPrimaryAction(context);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryAction, icon,
                ListBuilder.ICON_IMAGE, title);
        final SliceAction toggleSliceAction = SliceAction.createToggle(toggleAction,
                null /* actionTitle */, isBluetoothEnabled);

        return new ListBuilder(context, CustomSliceRegistry.BLUETOOTH_URI, ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new RowBuilder()
                        .setTitle(title)
                        .addEndItem(toggleSliceAction)
                        .setPrimaryAction(primarySliceAction))
                .build();
    }

    public static Intent getIntent(Context context) {
        final String screenTitle = context.getText(R.string.bluetooth_settings_title).toString();
        final Uri contentUri = new Uri.Builder().appendPath(
                SettingsSlicesContract.KEY_BLUETOOTH).build();
        return SliceBuilderUtils.buildSearchResultPageIntent(context,
                BluetoothDashboardFragment.class.getName(), null /* key */, screenTitle,
                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY,
                R.string.menu_key_connected_devices)
                .setClassName(context.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);
    }

    /**
     * Update the current Bluetooth status to the boolean value keyed by
     * {@link android.app.slice.Slice#EXTRA_TOGGLE_STATE} on {@param intent}.
     */
    public static void handleUriChange(Context context, Intent intent) {
        final boolean newBluetoothState = intent.getBooleanExtra(EXTRA_TOGGLE_STATE, false);
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (newBluetoothState) {
            adapter.enable();
        } else {
            adapter.disable();
        }
        // Do not notifyChange on Uri. The service takes longer to update the current value than it
        // does for the Slice to check the current value again. Let {@link SliceBroadcastRelay}
        // handle it.
    }

    private static boolean isBluetoothEnabled() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter.getState() == BluetoothAdapter.STATE_ON
                || adapter.getState() == BluetoothAdapter.STATE_TURNING_ON;
    }

    private static PendingIntent getPrimaryAction(Context context) {
        final Intent intent = getIntent(context);
        return PendingIntent.getActivity(context, 0 /* requestCode */,
                intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent getBroadcastIntent(Context context) {
        final Intent intent = new Intent(ACTION_BLUETOOTH_SLICE_CHANGED)
                .setClass(context, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }
}
