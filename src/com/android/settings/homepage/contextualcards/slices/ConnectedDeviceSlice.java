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
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
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
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.bluetooth.BluetoothDeviceDetailsFragment;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.instrumentation.Instrumentable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO(b/114807655): Contextual Home Page - Connected Device
 *
 * Show connected device info if one is currently connected. UI for connected device should
 * match Connected Devices > Currently Connected Devices
 *
 * TODO This class will be refactor for Bluetooth connected devices only.
 */
public class ConnectedDeviceSlice implements CustomSliceable {

    /**
     * To sort the Bluetooth devices by {@link CachedBluetoothDevice}.
     * Refer compareTo method from {@link com.android.settings.bluetooth.BluetoothDevicePreference}.
     */
    private static final Comparator<CachedBluetoothDevice> COMPARATOR
            = Comparator.naturalOrder();

    private static final String TAG = "ConnectedDeviceSlice";

    private final Context mContext;

    public ConnectedDeviceSlice(Context context) {
        mContext = context;
    }

    private static Bitmap getBitmapFromVectorDrawable(Drawable VectorDrawable) {
        final Bitmap bitmap = Bitmap.createBitmap(VectorDrawable.getIntrinsicWidth(),
                VectorDrawable.getIntrinsicHeight(), Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        VectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        VectorDrawable.draw(canvas);

        return bitmap;
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.CONNECTED_DEVICE_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_homepage_connected_device);
        final CharSequence title = mContext.getText(R.string.bluetooth_connected_devices);
        final CharSequence titleNoConnectedDevices = mContext.getText(
                R.string.no_connected_devices);
        final PendingIntent primaryActionIntent = PendingIntent.getActivity(mContext, 0,
                getIntent(), 0);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryActionIntent, icon,
                ListBuilder.ICON_IMAGE, title);
        final ListBuilder listBuilder =
                new ListBuilder(mContext, CustomSliceRegistry.CONNECTED_DEVICE_SLICE_URI,
                        ListBuilder.INFINITY)
                        .setAccentColor(Utils.getColorAccentDefaultColor(mContext));

        // Get row builders by connected devices, e.g. Bluetooth.
        final List<ListBuilder.RowBuilder> rows = getBluetoothRowBuilder(primarySliceAction);

        // Return a header with IsError flag, if no connected devices.
        if (rows.isEmpty()) {
            return listBuilder.setHeader(new ListBuilder.HeaderBuilder()
                    .setTitle(titleNoConnectedDevices)
                    .setPrimaryAction(primarySliceAction))
                    .setIsError(true)
                    .build();
        }

        // According the number of connected devices to set sub title of header.
        listBuilder.setHeader(new ListBuilder.HeaderBuilder()
                .setTitle(title)
                .setSubtitle(getSubTitle(rows.size()))
                .setPrimaryAction(primarySliceAction));

        // Add rows.
        for (ListBuilder.RowBuilder rowBuilder : rows) {
            listBuilder.addRow(rowBuilder);
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
                MetricsProto.MetricsEvent.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName());
    }

    @Override
    public void onNotifyChange(Intent intent) {
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return BluetoothUpdateWorker.class;
    }

    @VisibleForTesting
    List<CachedBluetoothDevice> getBluetoothConnectedDevices() {
        final List<CachedBluetoothDevice> connectedBluetoothList = new ArrayList<>();

        // If Bluetooth is disable, skip to get the bluetooth devices.
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Log.i(TAG, "Cannot get Bluetooth connected devices, Bluetooth is disabled.");
            return connectedBluetoothList;
        }

        // Get the Bluetooth devices from LocalBluetoothManager.
        final LocalBluetoothManager bluetoothManager =
                com.android.settings.bluetooth.Utils.getLocalBtManager(mContext);
        if (bluetoothManager == null) {
            Log.i(TAG, "Cannot get Bluetooth connected devices, Bluetooth is unsupported.");
            return connectedBluetoothList;
        }
        final Collection<CachedBluetoothDevice> cachedDevices =
                bluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();

        // Get connected Bluetooth devices and sort them.
        return cachedDevices.stream().filter(device -> device.isConnected()).sorted(
                COMPARATOR).collect(Collectors.toList());
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
                .setSourceMetricsCategory(Instrumentable.METRICS_CATEGORY_UNKNOWN);

        // The requestCode should be unique, use the hashcode of device as request code.
        return PendingIntent
                .getActivity(mContext, device.hashCode()  /* requestCode */,
                        subSettingLauncher.toIntent(),
                        0  /* flags */);
    }

    @VisibleForTesting
    IconCompat getConnectedDeviceIcon(CachedBluetoothDevice device) {
        final Pair<Drawable, String> pair = BluetoothUtils
                .getBtClassDrawableWithDescription(mContext, device);

        if (pair.first != null) {
            return IconCompat.createWithBitmap(getBitmapFromVectorDrawable(pair.first));
        } else {
            return IconCompat.createWithResource(mContext, R.drawable.ic_homepage_connected_device);
        }
    }

    private List<ListBuilder.RowBuilder> getBluetoothRowBuilder(SliceAction primarySliceAction) {
        final List<ListBuilder.RowBuilder> bluetoothRows = new ArrayList<>();

        // According Bluetooth connected device to create row builders.
        final List<CachedBluetoothDevice> bluetoothDevices = getBluetoothConnectedDevices();
        for (CachedBluetoothDevice bluetoothDevice : bluetoothDevices) {
            bluetoothRows.add(new ListBuilder.RowBuilder()
                    .setTitleItem(getConnectedDeviceIcon(bluetoothDevice), ListBuilder.ICON_IMAGE)
                    .setTitle(bluetoothDevice.getName())
                    .setSubtitle(bluetoothDevice.getConnectionSummary())
                    .setPrimaryAction(primarySliceAction)
                    .addEndItem(buildBluetoothDetailDeepLinkAction(bluetoothDevice)));
        }

        return bluetoothRows;
    }

    private SliceAction buildBluetoothDetailDeepLinkAction(CachedBluetoothDevice bluetoothDevice) {
        return SliceAction.createDeeplink(
                getBluetoothDetailIntent(bluetoothDevice),
                IconCompat.createWithResource(mContext, R.drawable.ic_settings),
                ListBuilder.ICON_IMAGE,
                bluetoothDevice.getName());
    }

    private CharSequence getSubTitle(int deviceCount) {
        return mContext.getResources().getQuantityString(R.plurals.show_connected_devices,
                deviceCount, deviceCount);
    }
}