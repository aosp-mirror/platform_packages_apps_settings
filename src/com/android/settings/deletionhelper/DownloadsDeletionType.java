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

package com.android.settings.deletionhelper;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.settings.deletionhelper.FetchDownloadsLoader.DownloadsResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The DownloadsDeletionType provides stale download file information to the
 * {@link DownloadsDeletionPreferenceGroup}.
 */
public class DownloadsDeletionType implements DeletionType, LoaderCallbacks<DownloadsResult> {
    private long mBytes;
    private long mMostRecent;
    private FreeableChangedListener mListener;
    private Context mContext;
    private ArrayMap<File, Boolean> mFiles;

    public DownloadsDeletionType(Context context) {
        mContext = context;
        mFiles = new ArrayMap<>();
    }

    @Override
    public void registerFreeableChangedListener(FreeableChangedListener listener) {
        mListener = listener;
        if (mFiles != null) {
            maybeUpdateListener();
        }
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void clearFreeableData() {
        if (mFiles != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<File, Boolean> entry : mFiles.entrySet()) {
                        if (entry.getValue()) {
                            entry.getKey().delete();
                        }
                    }
                }
            });
        }
    }

    @Override
    public Loader<DownloadsResult> onCreateLoader(int id, Bundle args) {
        return new FetchDownloadsLoader(mContext,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
    }

    @Override
    public void onLoadFinished(Loader<DownloadsResult> loader, DownloadsResult data) {
        mMostRecent = data.youngestLastModified;
        for (File file : data.files) {
            if (mFiles.containsKey(file)) {
                continue;
            }
            mFiles.put(file, false);
        }
        mBytes = data.totalSize;
        maybeUpdateListener();
    }

    @Override
    public void onLoaderReset(Loader<DownloadsResult> loader) {
    }

    /**
     * Returns the most recent last modified time for any clearable file.
     * @return The last modified time.
     */
    public long getMostRecentLastModified() {
        return mMostRecent;
    }

    /**
     * Returns the files in the Downloads folder after the loader task finishes.
     */
    public Set<File> getFiles() {
        if (mFiles == null) {
            return null;
        }
        return mFiles.keySet();
    }

    /**
     * Toggle if a file should be deleted when the service is asked to clear files.
     */
    public void toggleFile(File file, boolean checked) {
        mFiles.put(file, checked);
    }

    /**
     * Returns the number of bytes that would be cleared if the deletion tasks runs.
     */
    public long getFreeableBytes() {
        long freedBytes = 0;
        for (Map.Entry<File, Boolean> entry : mFiles.entrySet()) {
            if (entry.getValue()) {
                freedBytes += entry.getKey().length();
            }
        }
        return freedBytes;
    }

    private void maybeUpdateListener() {
        if (mListener != null) {
            mListener.onFreeableChanged(mFiles.size(), mBytes);
        }
    }
}
