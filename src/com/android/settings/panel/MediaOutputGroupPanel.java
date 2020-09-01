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

package com.android.settings.panel;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.android.settings.media.MediaOutputSlice.MEDIA_PACKAGE_NAME;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_GROUP_SLICE_URI;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaOutputSliceConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Media output group Panel.
 *
 * <p>
 * Displays Media output group item
 * </p>
 */
public class MediaOutputGroupPanel implements PanelContent, LocalMediaManager.DeviceCallback,
        LifecycleObserver {

    private final Context mContext;
    private final String mPackageName;

    private PanelContentCallback mCallback;
    private LocalMediaManager mLocalMediaManager;

    /**
     * To generate a Media output group Panel instance.
     *
     * @param context the context of the caller.
     * @param packageName media application package name.
     * @return MediaOutputGroupPanel instance.
     */
    public static MediaOutputGroupPanel create(Context context, String packageName) {
        return new MediaOutputGroupPanel(context, packageName);
    }

    private MediaOutputGroupPanel(Context context, String packageName) {
        mContext = context.getApplicationContext();
        mPackageName = packageName;
    }

    @Override
    public CharSequence getTitle() {
        return mContext.getText(R.string.media_output_group_panel_title);
    }

    @Override
    public CharSequence getSubTitle() {
        final int size = mLocalMediaManager.getSelectedMediaDevice().size();
        if (size == 1) {
            return mContext.getText(R.string.media_output_group_panel_single_device_summary);
        }
        return mContext.getString(R.string.media_output_group_panel_multiple_devices_summary, size);
    }

    @Override
    public IconCompat getIcon() {
        return IconCompat.createWithResource(mContext, R.drawable.ic_arrow_back).setTint(
                Color.BLACK);
    }

    @Override
    public List<Uri> getSlices() {
        final List<Uri> uris = new ArrayList<>();
        MEDIA_OUTPUT_GROUP_SLICE_URI =
                MEDIA_OUTPUT_GROUP_SLICE_URI
                        .buildUpon()
                        .clearQuery()
                        .appendQueryParameter(MEDIA_PACKAGE_NAME, mPackageName)
                        .build();
        uris.add(MEDIA_OUTPUT_GROUP_SLICE_URI);
        return uris;
    }

    @Override
    public Intent getSeeMoreIntent() {
        return null;
    }

    @Override
    public Intent getHeaderIconIntent() {
        final Intent intent = new Intent()
                .setAction(MediaOutputSliceConstants.ACTION_MEDIA_OUTPUT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MediaOutputSliceConstants.EXTRA_PACKAGE_NAME, mPackageName);
        return intent;
    }

    @Override
    public void registerCallback(PanelContentCallback callback) {
        mCallback = callback;
    }

    /**
     * Lifecycle callback to initial {@link LocalMediaManager}
     */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        if (mLocalMediaManager == null) {
            mLocalMediaManager = new LocalMediaManager(mContext, mPackageName, null);
        }
        mLocalMediaManager.registerCallback(this);
        mLocalMediaManager.startScan();
    }

    /**
     * Lifecycle callback to de-initial {@link LocalMediaManager}
     */
    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mLocalMediaManager.unregisterCallback(this);
        mLocalMediaManager.stopScan();
    }

    @Override
    public void onDeviceListUpdate(List<MediaDevice> devices) {
        if (mCallback != null) {
            mCallback.onHeaderChanged();
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_MEDIA_OUTPUT_GROUP;
    }

    @Override
    public int getViewType() {
        return PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON;
    }
}
