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
 * limitations under the License
 */

package com.android.settings.applications;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.InsetDrawable;
import android.os.UserHandle;
import android.support.annotation.WorkerThread;
import android.text.format.Formatter;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.applications.StorageStatsSource;

import java.io.IOException;

/** PhotosViewHolderController controls an Audio/Music file view in the ManageApplications view. */
public class PhotosViewHolderController implements FileViewHolderController {
    private static final String TAG = "PhotosViewHolderController";

    private static final String IMAGE_MIME_TYPE = "image/*";
    private static final int INSET_SIZE = 24; // dp

    private Context mContext;
    private StorageStatsSource mSource;
    private String mVolumeUuid;
    private long mFilesSize;
    private UserHandle mUser;

    public PhotosViewHolderController(
            Context context, StorageStatsSource source, String volumeUuid, UserHandle user) {
        mContext = context;
        mSource = source;
        mVolumeUuid = volumeUuid;
        mUser = user;
    }

    @Override
    @WorkerThread
    public void queryStats() {
        try {
            StorageStatsSource.ExternalStorageStats stats =
                    mSource.getExternalStorageStats(mVolumeUuid, mUser);
            mFilesSize = stats.imageBytes + stats.videoBytes;
        } catch (IOException e) {
            mFilesSize = 0;
            Log.w(TAG, e);
        }
    }

    @Override
    public boolean shouldShow() {
        return true;
    }

    @Override
    public void setupView(AppViewHolder holder) {
        holder.appIcon.setImageDrawable(
                new InsetDrawable(mContext.getDrawable(R.drawable.ic_photo_library), INSET_SIZE));
        holder.appName.setText(mContext.getText(R.string.storage_detail_images));
        holder.summary.setText(Formatter.formatFileSize(mContext, mFilesSize));
    }

    @Override
    public void onClick(Fragment fragment) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.setType(IMAGE_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_FROM_STORAGE, true);
        Utils.launchIntent(fragment, intent);
    }
}
