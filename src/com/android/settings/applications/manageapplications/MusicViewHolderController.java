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

package com.android.settings.applications.manageapplications;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.DocumentsContract;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.applications.StorageStatsSource;

import java.io.IOException;

/**
 * MusicViewHolderController controls an Audio/Music file view in the ManageApplications view.
 */
public class MusicViewHolderController implements FileViewHolderController {
    private static final String TAG = "MusicViewHolderCtrl";

    private static final String AUTHORITY_MEDIA = "com.android.providers.media.documents";

    private Context mContext;
    private StorageStatsSource mSource;
    private String mVolumeUuid;
    private long mMusicSize;
    private UserHandle mUser;

    public MusicViewHolderController(
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
            mMusicSize = mSource.getExternalStorageStats(mVolumeUuid, mUser).audioBytes;
        } catch (IOException e) {
            mMusicSize = 0;
            Log.w(TAG, e);
        }
    }

    @Override
    public boolean shouldShow() {
        return true;
    }

    @Override
    public void setupView(ApplicationViewHolder holder) {
        holder.setIcon(R.drawable.ic_headset_24dp);
        holder.setTitle(mContext.getText(R.string.audio_files_title));
        holder.setSummary(Formatter.formatFileSize(mContext, mMusicSize));
    }

    @Override
    public void onClick(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(
                DocumentsContract.buildRootUri(AUTHORITY_MEDIA, "audio_root"),
                DocumentsContract.Root.MIME_TYPE_ITEM);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(Intent.EXTRA_USER_ID, mUser.getIdentifier());
        Utils.launchIntent(fragment, intent);
    }
}
