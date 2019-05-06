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
 * limitations under the License.
 */

package com.android.settings.homepage.contextualcards.slices;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.bluetooth.BluetoothDeviceDetailsFragment;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BluetoothDevicesSlice implements CustomSliceable {

    @VisibleForTesting
    static final String BLUETOOTH_DEVICE_HASH_CODE = "bluetooth_device_hash_code";

    /**
     * Add the "Pair new device" in the end of slice, when the number of Bluetooth devices is less
     * than {@link #DEFAULT_EXPANDED_ROW_COUNT}.
     */
    @VisibleForTesting
    static final int DEFAULT_EXPANDED_ROW_COUNT = 3;

    /**
     * Refer {@link com.android.settings.bluetooth.BluetoothDevicePreference#compareTo} to sort the
     * Bluetooth devices by {@link CachedBluetoothDevice}.
     */
    private static final Comparator<CachedBluetoothDevice> COMPARATOR
            = Comparator.naturalOrder();

    private static final String TAG = "BluetoothDevicesSlice";

    private final Context mContext;

    public BluetoothDevicesSlice(Context context) {
        mContext = context;
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        // Reload theme for switching dark mode on/off
        mContext.getTheme().applyStyle(R.style.Theme_Settings_Home, true /* force */);

        final IconCompat icon = IconCompat.createWithResource(mContext,
                com.android.internal.R.drawable.ic_settings_bluetooth);
        final CharSequence title = mContext.getText(R.string.bluetooth_devices);
        final CharSequence titleNoBluetoothDevices = mContext.getText(
                R.string.no_bluetooth_devices);
        final PendingIntent primaryActionIntent = PendingIntent.getActivity(mContext, 0,
                getIntent(), 0);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryActionIntent, icon,
                ListBuilder.ICON_IMAGE, title);
        final ListBuilder listBuilder =
                new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                        .setAccentColor(COLOR_NOT_TINTED);

        // Get row builders by Bluetooth devices.
        final List<ListBuilder.RowBuilder> rows = getBluetoothRowBuilder();

        // Return a header with IsError flag, if no Bluetooth devices.
        if (rows.isEmpty()) {
            return listBuilder.setHeader(new ListBuilder.HeaderBuilder()
                    .setTitle(titleNoBluetoothDevices)
                    .setPrimaryAction(primarySliceAction))
                    .setIsError(true)
                    .build();
        }

        // Get displayable device count.
        final int deviceCount = Math.min(rows.size(), DEFAULT_EXPANDED_ROW_COUNT);

        // According to the displayable device count to set sub title of header.
        listBuilder.setHeader(new ListBuilder.HeaderBuilder()
                .setTitle(title)
                .setSubtitle(getSubTitle(deviceCount))
                .setPrimaryAction(primarySliceAction));

        // According to the displayable device count to add bluetooth device rows.
        for (int i = 0; i < deviceCount; i++) {
            listBuilder.addRow(rows.get(i));
        }

        return listBuilder.build();
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.connected_devices_dashboard_title)
                .toString();

        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                ConnectedDeviceDashboardFragment.class.getName(), "" /* key */,
                screenTitle,
                SettingsEnums.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(getUri());
    }

    @Override
    public void onNotifyChange(Intent intent) {
        // Activate available media device.
        final int bluetoothDeviceHashCode = intent.getIntExtra(BLUETOOTH_DEVICE_HASH_CODE, -1);
        for (CachedBluetoothDevice cachedBluetoothDevice : getConnectedBluetoothDevices()) {
            if (cachedBluetoothDevice.hashCode() == bluetoothDeviceHashCode) {
                cachedBluetoothDevice.setActive();
                return;
            }
        }
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return BluetoothUpdateWorker.class;
    }

    @VisibleForTesting
    List<CachedBluetoothDevice> getConnectedBluetoothDevices() {
        final List<CachedBluetoothDevice> bluetoothDeviceList = new ArrayList<>();

        // If Bluetooth is disable, skip to get the Bluetooth devices.
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Log.i(TAG, "Cannot get Bluetooth devices, Bluetooth is disabled.");
            return bluetoothDeviceList;
        }

        // Get the Bluetooth devices from LocalBluetoothManager.
        final LocalBluetoothManager bluetoothManager =
                com.android.settings.bluetooth.Utils.getLocalBtManager(mContext);
        if (bluetoothManager == null) {
            Log.i(TAG, "Cannot get Bluetooth devices, Bluetooth is unsupported.");
            return bluetoothDeviceList;
        }
        final Collection<CachedBluetoothDevice> cachedDevices =
                bluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();

        // Get all connected devices and sort them.
        return cachedDevices.stream()
                .filter(device -> device.getDevice().isConnected())
                .sorted(COMPARATOR).collect(Collectors.toList());
    }

    @VisibleForTesting
    PendingIntent getBluetoothDetailIntent(CachedBluetoothDevice device) {
        final Bundle args = new Bundle();
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS,
                device.getDevice().getAddress());
        final SubSettingLauncher subSettingLauncher = new SubSettingLauncher(mContext);
        subSettingLauncher.setDestination(BluetoothDeviceDetailsFragment.class.getName())
                .setArguments(args)
                .setTitleRes(R.string.device_details_title)
                .setSourceMetricsCategory(SettingsEnums.BLUETOOTH_DEVICE_DETAILS);

        // The requestCode should be unique, use the hashcode of device as request code.
        return PendingIntent
                .getActivity(mContext, device.hashCode()  /* requestCode */,
                        subSettingLauncher.toIntent(),
                        0  /* flags */);
    }

    @VisibleForTesting
    IconCompat getBluetoothDeviceIcon(CachedBluetoothDevice device) {
        final Pair<Drawable, String> pair =
                BluetoothUtils.getBtRainbowDrawableWithDescription(mContext, device);
        final Drawable drawable = pair.first;

        // Use default bluetooth icon if can't get icon.
        if (drawable == null) {
            return IconCompat.createWithResource(mContext,
                    com.android.internal.R.drawable.ic_settings_bluetooth);
        }

        return Utils.createIconWithDrawable(drawable);
    }

    private List<ListBuilder.RowBuilder> getBluetoothRowBuilder() {
        // According to Bluetooth devices to create row builders.
        final List<ListBuilder.RowBuilder> bluetoothRows = new ArrayList<>();
        final List<CachedBluetoothDevice> bluetoothDevices = getConnectedBluetoothDevices();
        for (CachedBluetoothDevice bluetoothDevice : bluetoothDevices) {
            final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                    .setTitleItem(getBluetoothDeviceIcon(bluetoothDevice), ListBuilder.ICON_IMAGE)
                    .setTitle(bluetoothDevice.getName())
                    .setSubtitle(bluetoothDevice.getConnectionSummary());

            if (bluetoothDevice.isConnectedA2dpDevice()) {
                // For available media devices, the primary action is to activate audio stream and
                // add setting icon to the end to link detail page.
                rowBuilder.setPrimaryAction(buildMediaBluetoothAction(bluetoothDevice));
                rowBuilder.addEndItem(buildBluetoothDetailDeepLinkAction(bluetoothDevice));
            } else {
                // For other devices, the primary action is to link detail page.
                rowBuilder.setPrimaryAction(buildBluetoothDetailDeepLinkAction(bluetoothDevice));
            }

            bluetoothRows.add(rowBuilder);
        }

        return bluetoothRows;
    }

    @VisibleForTesting
    SliceAction buildMediaBluetoothAction(CachedBluetoothDevice bluetoothDevice) {
        // Send broadcast to activate available media device.
        final Intent intent = new Intent(getUri().toString())
                .setClass(mContext, SliceBroadcastReceiver.class)
                .putExtra(BLUETOOTH_DEVICE_HASH_CODE, bluetoothDevice.hashCode());

        return SliceAction.create(
                PendingIntent.getBroadcast(mContext, bluetoothDevice.hashCode(), intent, 0),
                getBluetoothDeviceIcon(bluetoothDevice),
                ListBuilder.ICON_IMAGE,
                bluetoothDevice.getName());
    }

    @VisibleForTesting
    SliceAction buildBluetoothDetailDeepLinkAction(CachedBluetoothDevice bluetoothDevice) {
        return SliceAction.createDeeplink(
                getBluetoothDetailIntent(bluetoothDevice),
                IconCompat.createWithResource(mContext, R.drawable.ic_settings_accent),
                ListBuilder.ICON_IMAGE,
                bluetoothDevice.getName());
    }

    private CharSequence getSubTitle(int deviceCount) {
        return mContext.getResources().getQuantityString(R.plurals.show_bluetooth_devices,
                deviceCount, deviceCount);
    }
}
