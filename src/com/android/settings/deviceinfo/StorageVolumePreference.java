/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.deviceinfo.StorageSettings.UnmountTask;
import com.android.settingslib.Utils;

import java.io.File;
import java.io.IOException;

/**
 * Preference line representing a single {@link VolumeInfo}, possibly including
 * quick actions like unmounting.
 */
public class StorageVolumePreference extends Preference {
    private static final String TAG = StorageVolumePreference.class.getSimpleName();

    private final StorageManager mStorageManager;
    private final VolumeInfo mVolume;

    private int mUsedPercent = -1;
    private ColorStateList mColorTintList;

    // TODO: ideally, VolumeInfo should have a total physical size.
    public StorageVolumePreference(Context context, VolumeInfo volume, long totalBytes) {
        super(context);

        mStorageManager = context.getSystemService(StorageManager.class);
        mVolume = volume;
        mColorTintList = Utils.getColorAttr(context, android.R.attr.colorControlNormal);

        setLayoutResource(R.layout.storage_volume);

        setKey(volume.getId());
        setTitle(mStorageManager.getBestVolumeDescription(volume));

        Drawable icon;
        if (VolumeInfo.ID_PRIVATE_INTERNAL.equals(volume.getId())) {
            icon = context.getDrawable(R.drawable.ic_storage);
        } else {
            icon = context.getDrawable(R.drawable.ic_sim_sd);
        }

        if (volume.isMountedReadable()) {
            // TODO: move statfs() to background thread
            final File path = volume.getPath();

            long freeBytes = 0;
            long usedBytes = 0;
            if (volume.getType() == VolumeInfo.TYPE_PRIVATE) {
                final StorageStatsManager stats =
                        context.getSystemService(StorageStatsManager.class);
                try {
                    totalBytes = stats.getTotalBytes(volume.getFsUuid());
                    freeBytes = stats.getFreeBytes(volume.getFsUuid());
                    usedBytes = totalBytes - freeBytes;
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
            } else {
                // StorageStatsManager can only query private volumes.
                // Default to previous storage calculation for public volumes.
                if (totalBytes <= 0) {
                    totalBytes = path.getTotalSpace();
                }
                freeBytes = path.getFreeSpace();
                usedBytes = totalBytes - freeBytes;
            }

            final String used = Formatter.formatFileSize(context, usedBytes);
            final String total = Formatter.formatFileSize(context, totalBytes);
            setSummary(context.getString(R.string.storage_volume_summary, used, total));
            if (totalBytes > 0) {
                mUsedPercent = (int) ((usedBytes * 100) / totalBytes);
            }

            if (freeBytes < mStorageManager.getStorageLowBytes(path)) {
                mColorTintList = Utils.getColorAttr(context, android.R.attr.colorError);
                icon = context.getDrawable(R.drawable.ic_warning_24dp);
                icon.mutate();
                icon.setTintList(mColorTintList);
            }

        } else {
            setSummary(volume.getStateDescription());
            mUsedPercent = -1;
        }

        setIcon(icon);

        if (volume.getType() == VolumeInfo.TYPE_PUBLIC
                && volume.isMountedReadable()) {
            setWidgetLayoutResource(R.layout.preference_storage_action);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        final ImageView unmount = (ImageView) view.findViewById(R.id.unmount);
        if (unmount != null) {
            unmount.setOnClickListener(mUnmountListener);
        }

        final ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        if (mVolume.getType() == VolumeInfo.TYPE_PRIVATE && mUsedPercent != -1) {
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(mUsedPercent);
            progress.setProgressTintList(mColorTintList);
        } else {
            progress.setVisibility(View.GONE);
        }

        super.onBindViewHolder(view);
    }

    private final View.OnClickListener mUnmountListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            new UnmountTask(getContext(), mVolume).execute();
        }
    };
}
