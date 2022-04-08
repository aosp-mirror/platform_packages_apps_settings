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
import android.bluetooth.BluetoothDevice;
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
import com.android.settings.bluetooth.AvailableMediaBluetoothDeviceUpdater;
import com.android.settings.bluetooth.BluetoothDeviceDetailsFragment;
import com.android.settings.bluetooth.BluetoothPairingDetail;
import com.android.settings.bluetooth.SavedBluetoothDeviceUpdater;
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

    @VisibleForTesting
    static final int DEFAULT_EXPANDED_ROW_COUNT = 2;

    @VisibleForTesting
    static final String EXTRA_ENABLE_BLUETOOTH = "enable_bluetooth";

    /**
     * Refer {@link com.android.settings.bluetooth.BluetoothDevicePreference#compareTo} to sort the
     * Bluetooth devices by {@link CachedBluetoothDevice}.
     */
    private static final Comparator<CachedBluetoothDevice> COMPARATOR = Comparator.naturalOrder();

    private static final String TAG = "BluetoothDevicesSlice";

    // For seamless UI transition after tapping this slice to enable Bluetooth, this flag is to
    // update the layout promptly since it takes time for Bluetooth to reflect the enabling state.
    private static boolean sBluetoothEnabling;

    private final Context mContext;
    private AvailableMediaBluetoothDeviceUpdater mAvailableMediaBtDeviceUpdater;
    private SavedBluetoothDeviceUpdater mSavedBtDeviceUpdater;

    public BluetoothDevicesSlice(Context context) {
        mContext = context;
        BluetoothUpdateWorker.initLocalBtManager(context);
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.i(TAG, "Bluetooth is not supported on this hardware platform");
            return null;
        }

        final ListBuilder listBuilder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .setAccentColor(COLOR_NOT_TINTED);

        // Only show this header when Bluetooth is off and not turning on.
        if (!isBluetoothEnabled(btAdapter) && !sBluetoothEnabling) {
            return listBuilder.addRow(getBluetoothOffHeader()).build();
        }

        // Always reset this flag when showing the layout of Bluetooth on
        sBluetoothEnabling = false;

        // Add the header of Bluetooth on
        listBuilder.addRow(getBluetoothOnHeader());

        // Add row builders of Bluetooth devices.
        getBluetoothRowBuilders().forEach(row -> listBuilder.addRow(row));

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
        final boolean enableBluetooth = intent.getBooleanExtra(EXTRA_ENABLE_BLUETOOTH, false);
        if (enableBluetooth) {
            final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!isBluetoothEnabled(btAdapter)) {
                sBluetoothEnabling = true;
                btAdapter.enable();
                mContext.getContentResolver().notifyChange(getUri(), null);
            }
            return;
        }

        final int bluetoothDeviceHashCode = intent.getIntExtra(BLUETOOTH_DEVICE_HASH_CODE, -1);
        for (CachedBluetoothDevice device : getPairedBluetoothDevices()) {
            if (device.hashCode() == bluetoothDeviceHashCode) {
                if (device.isConnected()) {
                    device.setActive();
                } else if (!device.isBusy()) {
                    device.connect();
                }
                return;
            }
        }
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return BluetoothUpdateWorker.class;
    }

    @VisibleForTesting
    List<CachedBluetoothDevice> getPairedBluetoothDevices() {
        final List<CachedBluetoothDevice> bluetoothDeviceList = new ArrayList<>();

        // If Bluetooth is disable, skip getting the Bluetooth devices.
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Log.i(TAG, "Cannot get Bluetooth devices, Bluetooth is disabled.");
            return bluetoothDeviceList;
        }

        final LocalBluetoothManager localBtManager = BluetoothUpdateWorker.getLocalBtManager();
        if (localBtManager == null) {
            Log.i(TAG, "Cannot get Bluetooth devices, Bluetooth is not ready.");
            return bluetoothDeviceList;
        }

        final Collection<CachedBluetoothDevice> cachedDevices =
                localBtManager.getCachedDeviceManager().getCachedDevicesCopy();

        // Get all paired devices and sort them.
        return cachedDevices.stream()
                .filter(device -> device.getDevice().getBondState() == BluetoothDevice.BOND_BONDED)
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
                .getActivity(mContext, device.hashCode() /* requestCode */,
                        subSettingLauncher.toIntent(),
                        0  /* flags */);
    }

    @VisibleForTesting
    IconCompat getBluetoothDeviceIcon(CachedBluetoothDevice device) {
        final Pair<Drawable, String> pair =
                BluetoothUtils.getBtRainbowDrawableWithDescription(mContext, device);
        final Drawable drawable = pair.first;

        // Use default Bluetooth icon if we can't get one.
        if (drawable == null) {
            return IconCompat.createWithResource(mContext,
                    com.android.internal.R.drawable.ic_settings_bluetooth);
        }

        return Utils.createIconWithDrawable(drawable);
    }

    private ListBuilder.RowBuilder getBluetoothOffHeader() {
        final Drawable drawable = mContext.getDrawable(R.drawable.ic_bluetooth_disabled);
        final int tint = Utils.getDisabled(mContext, Utils.getColorAttrDefaultColor(mContext,
                android.R.attr.colorControlNormal));
        drawable.setTint(tint);
        final IconCompat icon = Utils.createIconWithDrawable(drawable);
        final CharSequence title = mContext.getText(R.string.bluetooth_devices_card_off_title);
        final CharSequence summary = mContext.getText(R.string.bluetooth_devices_card_off_summary);
        final Intent intent = new Intent(getUri().toString())
                .setClass(mContext, SliceBroadcastReceiver.class)
                .putExtra(EXTRA_ENABLE_BLUETOOTH, true);
        final SliceAction action = SliceAction.create(PendingIntent.getBroadcast(mContext,
                0 /* requestCode */, intent, 0 /* flags */), icon, ListBuilder.ICON_IMAGE, title);

        return new ListBuilder.RowBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setSubtitle(summary)
                .setPrimaryAction(action);
    }

    private ListBuilder.RowBuilder getBluetoothOnHeader() {
        final Drawable drawable = mContext.getDrawable(
                com.android.internal.R.drawable.ic_settings_bluetooth);
        drawable.setTint(Utils.getColorAccentDefaultColor(mContext));
        final IconCompat icon = Utils.createIconWithDrawable(drawable);
        final CharSequence title = mContext.getText(R.string.bluetooth_devices);
        final PendingIntent primaryActionIntent = PendingIntent.getActivity(mContext,
                0 /* requestCode */, getIntent(), 0 /* flags */);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryActionIntent, icon,
                ListBuilder.ICON_IMAGE, title);

        return new ListBuilder.RowBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setPrimaryAction(primarySliceAction)
                .addEndItem(getPairNewDeviceAction());
    }

    private SliceAction getPairNewDeviceAction() {
        final Drawable drawable = mContext.getDrawable(R.drawable.ic_add_24dp);
        drawable.setTint(Utils.getColorAccentDefaultColor(mContext));
        final IconCompat icon = Utils.createIconWithDrawable(drawable);
        final String title = mContext.getString(R.string.bluetooth_pairing_pref_title);
        final Intent intent = new SubSettingLauncher(mContext)
                .setDestination(BluetoothPairingDetail.class.getName())
                .setTitleRes(R.string.bluetooth_pairing_page_title)
                .setSourceMetricsCategory(SettingsEnums.BLUETOOTH_PAIRING)
                .toIntent();
        final PendingIntent pi = PendingIntent.getActivity(mContext, intent.hashCode(), intent,
                0 /* flags */);
        return SliceAction.createDeeplink(pi, icon, ListBuilder.ICON_IMAGE, title);
    }

    private List<ListBuilder.RowBuilder> getBluetoothRowBuilders() {
        final List<ListBuilder.RowBuilder> bluetoothRows = new ArrayList<>();
        final List<CachedBluetoothDevice> pairedDevices = getPairedBluetoothDevices();
        if (pairedDevices.isEmpty()) {
            return bluetoothRows;
        }

        // Initialize updaters without being blocked after paired devices is available because
        // LocalBluetoothManager is ready.
        lazyInitUpdaters();

        // Create row builders based on paired devices.
        for (CachedBluetoothDevice device : pairedDevices) {
            if (bluetoothRows.size() >= DEFAULT_EXPANDED_ROW_COUNT) {
                break;
            }

            String summary = device.getConnectionSummary();
            if (summary == null) {
                summary = mContext.getString(
                        R.string.connected_device_previously_connected_screen_title);
            }
            final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                    .setTitleItem(getBluetoothDeviceIcon(device), ListBuilder.ICON_IMAGE)
                    .setTitle(device.getName())
                    .setSubtitle(summary);

            if (mAvailableMediaBtDeviceUpdater.isFilterMatched(device)
                    || mSavedBtDeviceUpdater.isFilterMatched(device)) {
                // For all available media devices and previously connected devices, the primary
                // action is to activate or connect, and the end gear icon links to detail page.
                rowBuilder.setPrimaryAction(buildPrimaryBluetoothAction(device));
                rowBuilder.addEndItem(buildBluetoothDetailDeepLinkAction(device));
            } else {
                // For other devices, the primary action is to link to detail page.
                rowBuilder.setPrimaryAction(buildBluetoothDetailDeepLinkAction(device));
            }

            bluetoothRows.add(rowBuilder);
        }

        return bluetoothRows;
    }

    private void lazyInitUpdaters() {
        if (mAvailableMediaBtDeviceUpdater == null) {
            mAvailableMediaBtDeviceUpdater = new AvailableMediaBluetoothDeviceUpdater(mContext,
                    null /* fragment */, null /* devicePreferenceCallback */);
        }

        if (mSavedBtDeviceUpdater == null) {
            mSavedBtDeviceUpdater = new SavedBluetoothDeviceUpdater(mContext,
                    null /* fragment */, null /* devicePreferenceCallback */);
        }
    }

    @VisibleForTesting
    SliceAction buildPrimaryBluetoothAction(CachedBluetoothDevice bluetoothDevice) {
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

    private boolean isBluetoothEnabled(BluetoothAdapter btAdapter) {
        switch (btAdapter.getState()) {
            case BluetoothAdapter.STATE_ON:
            case BluetoothAdapter.STATE_TURNING_ON:
                return true;
            default:
                return false;
        }
    }
}
