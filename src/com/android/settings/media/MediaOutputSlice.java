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

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.UserHandle;
import android.util.IconDrawableFactory;
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

import java.util.List;

/**
 * Show the Media device that can be transfer the media.
 */
public class MediaOutputSlice implements CustomSliceable {

    private static final String TAG = "MediaOutputSlice";
    private static final String MEDIA_DEVICE_ID = "media_device_id";

    public static final String MEDIA_PACKAGE_NAME = "media_package_name";

    private final Context mContext;

    private MediaDeviceUpdateWorker mWorker;
    private String mPackageName;
    private IconDrawableFactory mIconDrawableFactory;

    public MediaOutputSlice(Context context) {
        mContext = context;
        mPackageName = getUri().getQueryParameter(MEDIA_PACKAGE_NAME);
        mIconDrawableFactory = IconDrawableFactory.newInstance(mContext);
    }

    @VisibleForTesting
    void init(String packageName, MediaDeviceUpdateWorker worker, IconDrawableFactory factory) {
        mPackageName = packageName;
        mWorker = worker;
        mIconDrawableFactory = factory;
    }

    @Override
    public Slice getSlice() {
        final PackageManager pm = mContext.getPackageManager();

        final List<MediaDevice> devices = getMediaDevices();
        final CharSequence title = Utils.getApplicationLabel(mContext, mPackageName);
        final CharSequence summary =
                mContext.getString(R.string.media_output_panel_summary_of_playing_device,
                        getConnectedDeviceName());

        final Drawable drawable =
                Utils.getBadgedIcon(mIconDrawableFactory, pm, mPackageName, UserHandle.myUserId());
        final IconCompat icon = IconCompat.createWithBitmap(getBitmapFromDrawable(drawable));

        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(getPrimaryAction(), icon,
                ListBuilder.ICON_IMAGE, title);

        final ListBuilder listBuilder = new ListBuilder(mContext, MEDIA_OUTPUT_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new ListBuilder.RowBuilder()
                        .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                        .setTitle(title)
                        .setSubtitle(summary)
                        .setPrimaryAction(primarySliceAction));

        for (MediaDevice device : devices) {
            listBuilder.addRow(getMediaDeviceRow(device));
        }

        return listBuilder.build();
    }

    private MediaDeviceUpdateWorker getWorker() {
        if (mWorker == null) {
            mWorker = (MediaDeviceUpdateWorker) SliceBackgroundWorker.getInstance(getUri());
            mWorker.setPackageName(mPackageName);
        }
        return mWorker;
    }

    private List<MediaDevice> getMediaDevices() {
        List<MediaDevice> devices = getWorker().getMediaDevices();
        return devices;
    }

    private String getConnectedDeviceName() {
        final MediaDevice device = getWorker().getCurrentConnectedMediaDevice();
        return device != null ? device.getName() : "";
    }

    private PendingIntent getPrimaryAction() {
        final PackageManager pm = mContext.getPackageManager();
        final Intent launchIntent = pm.getLaunchIntentForPackage(mPackageName);
        final Intent intent = launchIntent;
        return PendingIntent.getActivity(mContext, 0  /* requestCode */, intent, 0  /* flags */);
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private ListBuilder.RowBuilder getMediaDeviceRow(MediaDevice device) {
        final String title = device.getName();
        final PendingIntent broadcastAction =
                getBroadcastIntent(mContext, device.getId(), device.hashCode());
        final IconCompat deviceIcon = IconCompat.createWithResource(mContext, device.getIcon());
        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                .setTitleItem(deviceIcon, ListBuilder.ICON_IMAGE)
                .setPrimaryAction(SliceAction.create(broadcastAction, deviceIcon,
                        ListBuilder.ICON_IMAGE, title))
                .setTitle(title);

        return rowBuilder;
    }

    private PendingIntent getBroadcastIntent(Context context, String id, int requestCode) {
        final Intent intent = new Intent(getUri().toString());
        intent.setClass(context, SliceBroadcastReceiver.class);
        intent.putExtra(MEDIA_DEVICE_ID, id);
        return PendingIntent.getBroadcast(context, requestCode /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public Uri getUri() {
        return MEDIA_OUTPUT_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final MediaDeviceUpdateWorker worker = getWorker();
        final String id = intent != null ? intent.getStringExtra(MEDIA_DEVICE_ID) : "";
        final MediaDevice device = worker.getMediaDeviceById(id);
        if (device != null) {
            Log.d(TAG, "onNotifyChange() device name : " + device.getName());
            worker.connectDevice(device);
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
