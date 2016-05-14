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

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import com.android.settings.utils.AsyncLoader;

import java.io.File;
import java.util.ArrayList;

/**
 * FetchDownloadsLoader is an asynchronous task which returns files in the Downloads
 * directory which have not been modified in longer than 90 days.
 */
public class FetchDownloadsLoader extends
        AsyncLoader<FetchDownloadsLoader.DownloadsResult> {
    private File mDirectory;

    /**
     * Sets up a FetchDownloadsLoader in any directory.
     * @param directory The directory to look into.
     */
    public FetchDownloadsLoader(Context context, File directory) {
        super(context);
        mDirectory = directory;
    }

    @Override
    protected void onDiscardResult(DownloadsResult result) {}

    @Override
    public DownloadsResult loadInBackground() {
        return collectFiles(mDirectory);
    }

    @VisibleForTesting
    static DownloadsResult collectFiles(File dir) {
        return collectFiles(dir, new DownloadsResult());
    }

    private static DownloadsResult collectFiles(File dir, DownloadsResult result) {
        File downloadFiles[] = dir.listFiles();
        if (downloadFiles == null) {
        }
        if (downloadFiles != null && downloadFiles.length > 0) {
            for (File currentFile : downloadFiles) {
                if (currentFile.isDirectory()) {
                    collectFiles(currentFile, result);
                } else {
                    if (currentFile.lastModified() < result.youngestLastModified) {
                        result.youngestLastModified = currentFile.lastModified();
                    }
                    result.files.add(currentFile);
                    result.totalSize += currentFile.length();
                }
            }
        }

        return result;
    }

    /**
     * The DownloadsResult is the result of a {@link FetchDownloadsLoader} with the files
     * and the amount of space they use.
     */
    public static class DownloadsResult {
        public long totalSize;
        public long youngestLastModified;
        public ArrayList<File> files;

        public DownloadsResult() {
            this(0, Long.MAX_VALUE, new ArrayList<File>());
        }

        public DownloadsResult(long totalSize, long youngestLastModified, ArrayList<File> files) {
            this.totalSize = totalSize;
            this.youngestLastModified = youngestLastModified;
            this.files = files;
        }
    }
}