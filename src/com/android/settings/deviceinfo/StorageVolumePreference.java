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

import android.content.Context;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.preference.Preference;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.deviceinfo.StorageSettings.UnmountTask;

import java.io.File;

/**
 * Preference line representing a single {@link VolumeInfo}, possibly including
 * quick actions like unmounting.
 */
public class StorageVolumePreference extends Preference {
    private final StorageManager mStorageManager;
    private final VolumeInfo mVolume;

    public StorageVolumePreference(Context context, VolumeInfo volume) {
        super(context);

        mStorageManager = context.getSystemService(StorageManager.class);
        mVolume = volume;

        setKey(volume.getId());
        setTitle(mStorageManager.getBestVolumeDescription(volume));

        switch (volume.getState()) {
            case VolumeInfo.STATE_MOUNTED:
                // TODO: move statfs() to background thread
                final File path = volume.getPath();
                final String free = Formatter.formatFileSize(context, path.getFreeSpace());
                final String total = Formatter.formatFileSize(context, path.getTotalSpace());
                setSummary(context.getString(R.string.storage_volume_summary, free, total));
                break;
        }

        // TODO: better icons
        if (VolumeInfo.ID_PRIVATE_INTERNAL.equals(volume.getId())) {
            setIcon(context.getDrawable(R.drawable.ic_settings_storage));
        } else {
            setIcon(context.getDrawable(R.drawable.ic_sim_sd));
        }

        if (volume.getType() == VolumeInfo.TYPE_PUBLIC
                && volume.getState() == VolumeInfo.STATE_MOUNTED) {
            setWidgetLayoutResource(R.layout.preference_storage_action);
        }
    }

    @Override
    protected void onBindView(View view) {
        final TextView unmount = (TextView) view.findViewById(R.id.unmount);
        if (unmount != null) {
            unmount.setText("\u23CF");
            unmount.setOnClickListener(mUnmountListener);
        }

        super.onBindView(view);
    }

    private final View.OnClickListener mUnmountListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            new UnmountTask(getContext(), mVolume).execute();
        }
    };
}
