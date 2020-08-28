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

package com.android.settings.media;

import static android.app.slice.Slice.EXTRA_RANGE_VALUE;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_GROUP_SLICE_URI;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settingslib.media.MediaDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Show the Media device that can be transfer the media.
 */
public class MediaOutputGroupSlice implements CustomSliceable {

    @VisibleForTesting
    static final String GROUP_DEVICES = "group_devices";
    @VisibleForTesting
    static final String MEDIA_DEVICE_ID = "media_device_id";
    @VisibleForTesting
    static final String CUSTOMIZED_ACTION = "customized_action";
    @VisibleForTesting
    static final int ACTION_VOLUME_ADJUSTMENT = 1;
    @VisibleForTesting
    static final int ACTION_MEDIA_SESSION_OPERATION = 2;
    @VisibleForTesting
    static final int ERROR = -1;

    private static final String TAG = "MediaOutputGroupSlice";
    private static final int COLOR_DISABLED = (int) (255 * 0.3);

    private final Context mContext;
    private MediaDeviceUpdateWorker mWorker;

    public MediaOutputGroupSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        final ListBuilder listBuilder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .setAccentColor(COLOR_NOT_TINTED);
        // Add "Group" row
        final IconCompat titleIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_speaker_group_black_24dp);
        final Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        if (getWorker() == null) {
            return listBuilder.build();
        }
        final int maxVolume = getWorker().getSessionVolumeMax();
        final String title = mContext.getString(R.string.media_output_group);
        final SliceAction primaryAction = SliceAction.createDeeplink(
                getBroadcastIntent(GROUP_DEVICES,
                        GROUP_DEVICES.hashCode(),
                        ACTION_MEDIA_SESSION_OPERATION),
                titleIcon, ListBuilder.ICON_IMAGE, GROUP_DEVICES);
        final SliceAction endItemAction = SliceAction.createDeeplink(
                getBroadcastIntent(GROUP_DEVICES,
                        GROUP_DEVICES.hashCode() + ACTION_MEDIA_SESSION_OPERATION,
                        ACTION_MEDIA_SESSION_OPERATION),
                IconCompat.createWithBitmap(emptyBitmap), ListBuilder.ICON_IMAGE, "");
        if (maxVolume > 0 && !getWorker().hasAdjustVolumeUserRestriction()) {
            // Add InputRange row
            listBuilder.addInputRange(new ListBuilder.InputRangeBuilder()
                    .setTitleItem(titleIcon, ListBuilder.ICON_IMAGE)
                    .addEndItem(endItemAction)
                    .setTitle(title)
                    .setPrimaryAction(primaryAction)
                    .setInputAction(getBroadcastIntent(GROUP_DEVICES,
                            GROUP_DEVICES.hashCode() + ACTION_VOLUME_ADJUSTMENT,
                            ACTION_VOLUME_ADJUSTMENT))
                    .setMax(maxVolume)
                    .setValue(getWorker().getSessionVolume()));
        } else {    // No max volume information. Add generic Row
            listBuilder.addRow(new ListBuilder.RowBuilder()
                    .setTitleItem(titleIcon, ListBuilder.ICON_IMAGE)
                    .setTitle(title)
                    .setPrimaryAction(primaryAction));
        }
        // Add device row
        addRow(listBuilder, getWorker().getSelectedMediaDevice(), true);
        addRow(listBuilder, getWorker().getSelectableMediaDevice(), false);
        return listBuilder.build();
    }

    private void addRow(ListBuilder listBuilder, List<MediaDevice> mediaDevices, boolean selected) {
        final boolean adjustVolumeUserRestriction = getWorker().hasAdjustVolumeUserRestriction();
        List<MediaDevice> deselectableMediaDevices = new ArrayList<>();
        if (selected) {
            deselectableMediaDevices = getWorker().getDeselectableMediaDevice();
        }
        for (MediaDevice device : mediaDevices) {
            final int maxVolume = device.getMaxVolume();
            final IconCompat titleIcon = Utils.createIconWithDrawable(device.getIcon());
            final String title = device.getName();
            final SliceAction disabledIconSliceAction = SliceAction.createDeeplink(
                    getBroadcastIntent(null, 0, 0),
                    getDisabledCheckboxIcon(), ListBuilder.ICON_IMAGE, "");
            final SliceAction enabledIconSliceAction = SliceAction.createToggle(
                    getBroadcastIntent(device.getId(),
                            device.hashCode() + ACTION_MEDIA_SESSION_OPERATION,
                            ACTION_MEDIA_SESSION_OPERATION),
                    IconCompat.createWithResource(mContext, R.drawable.ic_check_box_anim),
                    "",
                    selected);
            if (maxVolume > 0 && selected && !adjustVolumeUserRestriction) {
                // Add InputRange row
                final ListBuilder.InputRangeBuilder builder = new ListBuilder.InputRangeBuilder()
                        .setTitleItem(titleIcon, ListBuilder.ICON_IMAGE)
                        .setTitle(title)
                        .setInputAction(getBroadcastIntent(device.getId(),
                                device.hashCode() + ACTION_VOLUME_ADJUSTMENT,
                                ACTION_VOLUME_ADJUSTMENT))
                        .setMax(device.getMaxVolume())
                        .setValue(device.getCurrentVolume());
                // Add endItem with different icons
                if (selected && (!getWorker().isDeviceIncluded(deselectableMediaDevices, device)
                        || mediaDevices.size() == 1)) {
                    builder.addEndItem(disabledIconSliceAction);
                } else {
                    builder.addEndItem(enabledIconSliceAction);
                }
                listBuilder.addInputRange(builder);
            } else {    // No max volume information. Add generic Row
                final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                        .setTitleItem(titleIcon, ListBuilder.ICON_IMAGE)
                        .setTitle(title);
                // Add endItem with different icons
                if (selected && (!getWorker().isDeviceIncluded(deselectableMediaDevices, device)
                        || mediaDevices.size() == 1)) {
                    rowBuilder.addEndItem(disabledIconSliceAction);
                } else {
                    rowBuilder.addEndItem(enabledIconSliceAction);
                }
                listBuilder.addRow(rowBuilder);
            }
        }
    }

    private IconCompat getDisabledCheckboxIcon() {
        final Drawable drawable = mContext.getDrawable(R.drawable.ic_check_box_blue_24dp).mutate();
        final Bitmap checkbox = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(checkbox);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.setAlpha(COLOR_DISABLED);
        drawable.draw(canvas);

        return IconCompat.createWithBitmap(checkbox);
    }

    private PendingIntent getBroadcastIntent(String id, int requestCode, int action) {
        final Intent intent = new Intent(getUri().toString());
        intent.setClass(mContext, SliceBroadcastReceiver.class);
        intent.putExtra(MEDIA_DEVICE_ID, id);
        intent.putExtra(CUSTOMIZED_ACTION, action);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(mContext, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private MediaDeviceUpdateWorker getWorker() {
        if (mWorker == null) {
            mWorker = SliceBackgroundWorker.getInstance(getUri());
        }
        return mWorker;
    }

    @Override
    public Uri getUri() {
        return MEDIA_OUTPUT_GROUP_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final String id = intent.getStringExtra(MEDIA_DEVICE_ID);
        if (TextUtils.isEmpty(id)) {
            Log.e(TAG, "Unable to handle notification. The device is unavailable");
            return;
        }
        final MediaDeviceUpdateWorker worker = getWorker();
        final MediaDevice device = worker.getMediaDeviceById(id);
        switch (intent.getIntExtra(CUSTOMIZED_ACTION, ERROR)) {
            case ACTION_VOLUME_ADJUSTMENT:
                final int newPosition = intent.getIntExtra(EXTRA_RANGE_VALUE, ERROR);
                if (newPosition == ERROR) {
                    Log.e(TAG, "Unable to adjust volume. The volume value is unavailable");
                    return;
                }
                // Group volume adjustment
                if (TextUtils.equals(id, GROUP_DEVICES)) {
                    worker.adjustSessionVolume(newPosition);
                } else {
                    if (device == null) {
                        Log.e(TAG, "Unable to adjust volume. The device(" + id
                                + ") is unavailable");
                        return;
                    }
                    // Single device volume adjustment
                    worker.adjustVolume(device, newPosition);
                }
                break;
            case ACTION_MEDIA_SESSION_OPERATION:
                if (device == null) {
                    Log.e(TAG, "Unable to adjust session volume. The device(" + id
                            + ") is unavailable");
                    return;
                }
                if (worker.isDeviceIncluded(worker.getSelectableMediaDevice(), device)) {
                    worker.addDeviceToPlayMedia(device);
                } else if (worker.isDeviceIncluded(worker.getDeselectableMediaDevice(), device)) {
                    worker.removeDeviceFromPlayMedia(device);
                } else {
                    // Do nothing
                    Log.d(TAG, device.getName() + " is not selectable nor deselectable");
                }
                break;
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
}
