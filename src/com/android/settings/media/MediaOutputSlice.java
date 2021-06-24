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

package com.android.settings.media;

import static android.app.slice.Slice.EXTRA_RANGE_VALUE;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.bluetooth.BluetoothPairingDetail;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaOutputSliceConstants;

import java.util.Collection;

/**
 * Show the Media device that can be transfer the media.
 */
public class MediaOutputSlice implements CustomSliceable {

    private static final String TAG = "MediaOutputSlice";
    private static final String MEDIA_DEVICE_ID = "media_device_id";
    private static final String MEDIA_GROUP_DEVICE = "media_group_device";
    private static final String MEDIA_GROUP_REQUEST = "media_group_request";
    private static final int NON_SLIDER_VALUE = -1;

    public static final String MEDIA_PACKAGE_NAME = "media_package_name";

    private final Context mContext;

    private MediaDeviceUpdateWorker mWorker;

    public MediaOutputSlice(Context context) {
        mContext = context;
    }

    @VisibleForTesting
    void init(MediaDeviceUpdateWorker worker) {
        mWorker = worker;
    }

    @Override
    public Slice getSlice() {
        final ListBuilder listBuilder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .setAccentColor(COLOR_NOT_TINTED);
        if (!isVisible()) {
            Log.d(TAG, "getSlice() is not visible");
            return listBuilder.build();
        }

        final Collection<MediaDevice> devices = getMediaDevices();
        final MediaDeviceUpdateWorker worker = getWorker();

        if (worker.getSelectedMediaDevice().size() > 1) {
            // Insert group item to the first when it is available
            if (worker.getSessionVolumeMax() > 0 && !worker.hasAdjustVolumeUserRestriction()) {
                listBuilder.addInputRange(getGroupSliderRow());
            } else {
                listBuilder.addRow(getGroupRow());
            }
            // Add all other devices
            for (MediaDevice device : devices) {
                addRow(device, null /* connectedDevice */, listBuilder);
            }
        } else {
            final MediaDevice connectedDevice = worker.getCurrentConnectedMediaDevice();
            if (devices.size() == 1) {
                // Zero state
                final MediaDevice device = devices.iterator().next();
                addRow(device, device, listBuilder);
                // Add "pair new" only when local output device exists
                final int type = device.getDeviceType();
                if (type == MediaDevice.MediaDeviceType.TYPE_PHONE_DEVICE
                        || type == MediaDevice.MediaDeviceType.TYPE_3POINT5_MM_AUDIO_DEVICE
                        || type == MediaDevice.MediaDeviceType.TYPE_USB_C_AUDIO_DEVICE) {
                    listBuilder.addRow(getPairNewRow());
                }
            } else {
                final boolean isTouched = worker.getIsTouched();
                // Fix the last top device when user press device to transfer.
                final MediaDevice topDevice = isTouched ? worker.getTopDevice() : connectedDevice;

                if (topDevice != null) {
                    addRow(topDevice, connectedDevice, listBuilder);
                    worker.setTopDevice(topDevice);
                }

                for (MediaDevice device : devices) {
                    if (topDevice == null || !TextUtils.equals(topDevice.getId(), device.getId())) {
                        addRow(device, connectedDevice, listBuilder);
                    }
                }
            }
        }
        return listBuilder.build();
    }

    private ListBuilder.RowBuilder getPairNewRow() {
        final Drawable d = mContext.getDrawable(R.drawable.ic_add_24dp);
        d.setColorFilter(new PorterDuffColorFilter(Utils.getColorAccentDefaultColor(mContext),
                PorterDuff.Mode.SRC_IN));
        final IconCompat icon = Utils.createIconWithDrawable(d);
        final String title = mContext.getString(R.string.bluetooth_pairing_pref_title);
        final Intent intent = new SubSettingLauncher(mContext)
                .setDestination(BluetoothPairingDetail.class.getName())
                .setTitleRes(R.string.bluetooth_pairing_page_title)
                .setSourceMetricsCategory(SettingsEnums.PANEL_MEDIA_OUTPUT)
                .toIntent();
        final SliceAction primarySliceAction = SliceAction.createDeeplink(
                PendingIntent.getActivity(mContext, 0 /* requestCode */, intent,
                        PendingIntent.FLAG_IMMUTABLE),
                IconCompat.createWithResource(mContext, R.drawable.ic_add_24dp/*ic_add_blue_24dp*/),
                ListBuilder.ICON_IMAGE,
                mContext.getText(R.string.bluetooth_pairing_pref_title));
        final ListBuilder.RowBuilder builder = new ListBuilder.RowBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setPrimaryAction(primarySliceAction);
        return builder;
    }

    private ListBuilder.InputRangeBuilder getGroupSliderRow() {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_speaker_group_black_24dp);
        final CharSequence sessionName = getWorker().getSessionName();
        final CharSequence title = TextUtils.isEmpty(sessionName)
                ? mContext.getString(R.string.media_output_group) : sessionName;
        final PendingIntent broadcastAction =
                getBroadcastIntent(mContext, MEDIA_GROUP_DEVICE, MEDIA_GROUP_DEVICE.hashCode());
        final SliceAction primarySliceAction = SliceAction.createDeeplink(broadcastAction, icon,
                ListBuilder.ICON_IMAGE, title);
        final ListBuilder.InputRangeBuilder builder = new ListBuilder.InputRangeBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setPrimaryAction(primarySliceAction)
                .setInputAction(getSliderInputAction(MEDIA_GROUP_DEVICE.hashCode(),
                        MEDIA_GROUP_DEVICE))
                .setMax(getWorker().getSessionVolumeMax())
                .setValue(getWorker().getSessionVolume())
                .addEndItem(getEndItemSliceAction());
        return builder;
    }

    private ListBuilder.RowBuilder getGroupRow() {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_speaker_group_black_24dp);
        final CharSequence sessionName = getWorker().getSessionName();
        final CharSequence title = TextUtils.isEmpty(sessionName)
                ? mContext.getString(R.string.media_output_group) : sessionName;
        final PendingIntent broadcastAction =
                getBroadcastIntent(mContext, MEDIA_GROUP_DEVICE, MEDIA_GROUP_DEVICE.hashCode());
        final SliceAction primarySliceAction = SliceAction.createDeeplink(broadcastAction, icon,
                ListBuilder.ICON_IMAGE, title);
        final ListBuilder.RowBuilder builder = new ListBuilder.RowBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setPrimaryAction(primarySliceAction)
                .addEndItem(getEndItemSliceAction());
        return builder;
    }

    private void addRow(MediaDevice device, MediaDevice connectedDevice, ListBuilder listBuilder) {
        if (connectedDevice != null && TextUtils.equals(device.getId(), connectedDevice.getId())) {
            final String title = device.getName();
            final IconCompat icon = getDeviceIconCompat(device);

            final PendingIntent broadcastAction =
                    getBroadcastIntent(mContext, device.getId(), device.hashCode());
            final SliceAction primarySliceAction = SliceAction.createDeeplink(broadcastAction, icon,
                    ListBuilder.ICON_IMAGE, title);

            if (device.getMaxVolume() > 0 && !getWorker().hasAdjustVolumeUserRestriction()) {
                final ListBuilder.InputRangeBuilder builder = new ListBuilder.InputRangeBuilder()
                        .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                        .setTitle(title)
                        .setPrimaryAction(primarySliceAction)
                        .setInputAction(getSliderInputAction(device.hashCode(), device.getId()))
                        .setMax(device.getMaxVolume())
                        .setValue(device.getCurrentVolume());
                // Check end item visibility
                if (device.getDeviceType() == MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE
                        && !getWorker().getSelectableMediaDevice().isEmpty()) {
                    builder.addEndItem(getEndItemSliceAction());
                }
                listBuilder.addInputRange(builder);
            } else {
                Log.d(TAG, "addRow device = " + device.getName() + " MaxVolume = "
                        + device.getMaxVolume());
                final ListBuilder.RowBuilder builder = getMediaDeviceRow(device);
                // Check end item visibility
                if (device.getDeviceType() == MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE
                        && !getWorker().getSelectableMediaDevice().isEmpty()) {
                    builder.addEndItem(getEndItemSliceAction());
                }
                listBuilder.addRow(builder);
            }
        } else {
            if (device.getState() == LocalMediaManager.MediaDeviceState.STATE_CONNECTING) {
                listBuilder.addRange(getTransferringMediaDeviceRow(device));
            } else {
                listBuilder.addRow(getMediaDeviceRow(device));
            }
        }
    }

    private PendingIntent getSliderInputAction(int requestCode, String id) {
        final Intent intent = new Intent(getUri().toString())
                .setData(getUri())
                .putExtra(MEDIA_DEVICE_ID, id)
                .setClass(mContext, SliceBroadcastReceiver.class);

        return PendingIntent.getBroadcast(mContext, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE);
    }

    private SliceAction getEndItemSliceAction() {
        final Intent intent = new Intent()
                .setAction(MediaOutputSliceConstants.ACTION_MEDIA_OUTPUT_GROUP)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MediaOutputSliceConstants.EXTRA_PACKAGE_NAME,
                        getWorker().getPackageName());
        final int requestCode = TextUtils.isEmpty(getWorker().getPackageName())
                ? 0
                : getWorker().getPackageName().hashCode();
        return SliceAction.createDeeplink(
                PendingIntent.getActivity(mContext, requestCode, intent,
                        PendingIntent.FLAG_IMMUTABLE),
                IconCompat.createWithResource(mContext, R.drawable.ic_add_blue_24dp),
                ListBuilder.ICON_IMAGE,
                mContext.getText(R.string.add));
    }

    private IconCompat getDeviceIconCompat(MediaDevice device) {
        Drawable drawable = device.getIcon();
        if (drawable == null) {
            Log.d(TAG, "getDeviceIconCompat() device : " + device.getName() + ", drawable is null");
            // Use default Bluetooth device icon to handle getIcon() is null case.
            drawable = mContext.getDrawable(com.android.internal.R.drawable.ic_bt_headphones_a2dp);
        }

        return Utils.createIconWithDrawable(drawable);
    }

    private MediaDeviceUpdateWorker getWorker() {
        if (mWorker == null) {
            mWorker = SliceBackgroundWorker.getInstance(getUri());
        }
        return mWorker;
    }

    private Collection<MediaDevice> getMediaDevices() {
        final Collection<MediaDevice> devices = getWorker().getMediaDevices();
        return devices;
    }

    private ListBuilder.RangeBuilder getTransferringMediaDeviceRow(MediaDevice device) {
        final IconCompat deviceIcon = getDeviceIconCompat(device);
        final SliceAction sliceAction = SliceAction.create(getBroadcastIntent(mContext,
                device.getId(), device.hashCode()), deviceIcon, ListBuilder.ICON_IMAGE,
                mContext.getText(R.string.media_output_switching));

        return new ListBuilder.RangeBuilder()
                .setTitleItem(deviceIcon, ListBuilder.ICON_IMAGE)
                .setMode(ListBuilder.RANGE_MODE_INDETERMINATE)
                .setTitle(device.getName())
                .setPrimaryAction(sliceAction);
    }

    private ListBuilder.RowBuilder getMediaDeviceRow(MediaDevice device) {
        final String deviceName = device.getName();
        final PendingIntent broadcastAction =
                getBroadcastIntent(mContext, device.getId(), device.hashCode());
        final IconCompat deviceIcon = getDeviceIconCompat(device);
        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                .setTitleItem(deviceIcon, ListBuilder.ICON_IMAGE);

        if (device.getDeviceType() == MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE
                && !device.isConnected()) {
            final int state = device.getState();
            if (state == LocalMediaManager.MediaDeviceState.STATE_CONNECTING_FAILED) {
                rowBuilder.setTitle(deviceName);
                rowBuilder.setPrimaryAction(SliceAction.create(broadcastAction, deviceIcon,
                        ListBuilder.ICON_IMAGE, deviceName));
                rowBuilder.setSubtitle(mContext.getText(R.string.bluetooth_connect_failed));
            } else {
                // Append status to title only for the disconnected Bluetooth device.
                final SpannableString spannableTitle = new SpannableString(
                        mContext.getString(R.string.media_output_disconnected_status, deviceName));
                spannableTitle.setSpan(new ForegroundColorSpan(
                                Utils.getColorAttrDefaultColor(mContext,
                                        android.R.attr.textColorSecondary)),
                        deviceName.length(),
                        spannableTitle.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
                rowBuilder.setTitle(spannableTitle);
                rowBuilder.setPrimaryAction(SliceAction.create(broadcastAction, deviceIcon,
                        ListBuilder.ICON_IMAGE, spannableTitle));
            }
        } else {
            rowBuilder.setTitle(deviceName);
            rowBuilder.setPrimaryAction(SliceAction.create(broadcastAction, deviceIcon,
                    ListBuilder.ICON_IMAGE, deviceName));
            if (device.getState() == LocalMediaManager.MediaDeviceState.STATE_CONNECTING_FAILED) {
                rowBuilder.setSubtitle(mContext.getText(R.string.media_output_switch_error_text));
            }
        }

        return rowBuilder;
    }

    private PendingIntent getBroadcastIntent(Context context, String id, int requestCode) {
        final Intent intent = new Intent(getUri().toString());
        intent.setClass(context, SliceBroadcastReceiver.class);
        intent.putExtra(MEDIA_DEVICE_ID, id);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public Uri getUri() {
        return MEDIA_OUTPUT_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final MediaDeviceUpdateWorker worker = getWorker();
        final String id = intent != null ? intent.getStringExtra(MEDIA_DEVICE_ID) : "";
        if (TextUtils.isEmpty(id)) {
            return;
        }

        final int newPosition = intent.getIntExtra(EXTRA_RANGE_VALUE, NON_SLIDER_VALUE);
        if (TextUtils.equals(id, MEDIA_GROUP_DEVICE)) {
            // Session volume adjustment
            worker.adjustSessionVolume(newPosition);
        } else {
            final MediaDevice device = worker.getMediaDeviceById(id);
            if (device == null) {
                Log.d(TAG, "onNotifyChange: Unable to get device " + id);
                return;
            }

            if (newPosition == NON_SLIDER_VALUE) {
                // Intent for device connection
                Log.d(TAG, "onNotifyChange: Switch to " + device.getName());
                worker.setIsTouched(true);
                worker.connectDevice(device);
            } else {
                // Single device volume adjustment
                worker.adjustVolume(device, newPosition);
            }
        }
    }

    @Override
    public Intent getIntent() {
        return null;
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return MediaDeviceUpdateWorker.class;
    }

    private boolean isVisible() {
        // To decide Slice's visibility.
        // Return true if
        // 1. AudioMode is not in on-going call
        // 2. worker is not null
        // 3. Available devices are more than 0
        return getWorker() != null
                && !com.android.settingslib.Utils.isAudioModeOngoingCall(mContext)
                && getWorker().getMediaDevices().size() > 0;
    }
}
