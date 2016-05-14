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
import com.android.settings.deletionhelper.FetchDownloadsLoader.DownloadsResult;

import java.io.File;
import java.util.ArrayList;

/**
 * The DownloadsDeletionType provides stale download file information to the
 * {@link DownloadsDeletionPreference}.
 */
public class DownloadsDeletionType implements DeletionType, LoaderCallbacks<DownloadsResult> {
    private int mItems;
    private long mBytes;
    private long mMostRecent;
    private FreeableChangedListener mListener;
    private FetchDownloadsLoader mTask;
    private ArrayList<File> mFiles;
    private Context mContext;

    public DownloadsDeletionType(Context context) {
        mContext = context;
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
                    for (File file : mFiles) {
                        file.delete();
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
        mFiles = data.files;
        mBytes = data.totalSize;
        mItems = mFiles.size();
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

    private void maybeUpdateListener() {
        if (mListener != null) {
            mListener.onFreeableChanged(mItems, mBytes);
        }
    }
}
