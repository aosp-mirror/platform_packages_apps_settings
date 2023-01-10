/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.bluetooth.BluetoothBroadcastDialog;
import com.android.settings.media.MediaOutputIndicatorWorker;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.media.BluetoothMediaDevice;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaOutputConstants;

public class MediaVolumePreferenceController extends VolumeSeekBarPreferenceController {
    private static final String TAG = "MediaVolumePreCtrl";
    private static final String KEY_MEDIA_VOLUME = "media_volume";

    private MediaOutputIndicatorWorker mWorker;
    private MediaDevice mMediaDevice;
    private static final String ACTION_LAUNCH_BROADCAST_DIALOG =
            "android.settings.MEDIA_BROADCAST_DIALOG";

    public MediaVolumePreferenceController(Context context) {
        super(context, KEY_MEDIA_VOLUME);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_media_volume)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_MEDIA_VOLUME);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MEDIA_VOLUME;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_MUSIC;
    }

    @Override
    public int getMuteIcon() {
        return R.drawable.ic_media_stream_off;
    }

    @VisibleForTesting
    boolean isSupportEndItem() {
        return getWorker() != null && getWorker().isBroadcastSupported() && isConnectedBLEDevice();
    }

    private boolean isConnectedBLEDevice() {
        if (getWorker() == null) {
            Log.d(TAG, "The Worker is null");
            return false;
        }
        mMediaDevice = getWorker().getCurrentConnectedMediaDevice();
        if (mMediaDevice != null) {
            return mMediaDevice.isBLEDevice();
        }
        return false;
    }

    @Override
    public SliceAction getSliceEndItem(Context context) {
        if (!isSupportEndItem()) {
            Log.d(TAG, "The slice doesn't support end item");
            return null;
        }

        final Intent intent = new Intent();
        PendingIntent pi = null;
        if (getWorker().isDeviceBroadcasting()) {
            intent.setPackage(MediaOutputConstants.SYSTEMUI_PACKAGE_NAME);
            intent.setAction(MediaOutputConstants.ACTION_LAUNCH_MEDIA_OUTPUT_BROADCAST_DIALOG);
            intent.putExtra(MediaOutputConstants.EXTRA_PACKAGE_NAME,
                    getWorker().getActiveLocalMediaController().getPackageName());

            pi = PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            final CachedBluetoothDevice bluetoothDevice =
                    ((BluetoothMediaDevice) mMediaDevice).getCachedDevice();
            if (bluetoothDevice == null) {
                Log.d(TAG, "The bluetooth device is null");
                return null;
            }
            intent.setAction(ACTION_LAUNCH_BROADCAST_DIALOG);
            intent.putExtra(BluetoothBroadcastDialog.KEY_APP_LABEL,
                    Utils.getApplicationLabel(mContext, getWorker().getPackageName()));
            intent.putExtra(BluetoothBroadcastDialog.KEY_DEVICE_ADDRESS,
                    bluetoothDevice.getAddress());
            intent.putExtra(BluetoothBroadcastDialog.KEY_MEDIA_STREAMING, getWorker() != null
                    && getWorker().getActiveLocalMediaController() != null);

            pi = PendingIntent.getActivity(context, 0 /* requestCode */, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        final IconCompat icon = getBroadcastIcon(context);

        return SliceAction.createDeeplink(pi, icon, ListBuilder.ICON_IMAGE, getPreferenceKey());
    }

    private IconCompat getBroadcastIcon(Context context) {
        final Drawable drawable = context.getDrawable(
                com.android.settingslib.R.drawable.settings_input_antenna);
        if (drawable != null) {
            drawable.setTint(Utils.getColorAccentDefaultColor(context));
            return Utils.createIconWithDrawable(drawable);
        }
        return null;
    }

    private MediaOutputIndicatorWorker getWorker() {
        if (mWorker == null) {
            mWorker = SliceBackgroundWorker.getInstance(getUri());
        }
        return mWorker;
    }

    private Uri getUri() {
        return CustomSliceRegistry.VOLUME_MEDIA_URI;
    }

    @Override
    public Class<? extends SliceBackgroundWorker> getBackgroundWorkerClass() {
        return MediaOutputIndicatorWorker.class;
    }
}
