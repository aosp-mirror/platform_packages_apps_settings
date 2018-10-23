/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import android.annotation.MainThread;
import android.content.ContentResolver;
import android.net.Uri;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Slice background worker is used to make Settings Slices be able to work with data that is
 * changing continuously, e.g. available Wi-Fi networks.
 *
 * The background worker will be started at {@link SettingsSliceProvider#onSlicePinned(Uri)}, and be
 * stopped at {@link SettingsSliceProvider#onSliceUnpinned(Uri)}.
 *
 * {@link SliceBackgroundWorker} caches the results, uses the cache to compare if there is any data
 * changed, and then notifies the Slice {@link Uri} to update.
 */
public abstract class SliceBackgroundWorker<E> implements Closeable {

    private final ContentResolver mContentResolver;
    private final Uri mUri;

    private List<E> mCachedResults;

    protected SliceBackgroundWorker(ContentResolver cr, Uri uri) {
        mContentResolver = cr;
        mUri = uri;
    }

    /**
     * Called when the Slice is pinned. This is the place to register callbacks or initialize scan
     * tasks.
     */
    @MainThread
    protected abstract void onSlicePinned();

    /**
     * Called when the Slice is unpinned. This is the place to unregister callbacks or perform any
     * final cleanup.
     */
    @MainThread
    protected abstract void onSliceUnpinned();

    /**
     * @return a {@link List} of cached results
     */
    public final List<E> getResults() {
        return mCachedResults == null ? null : new ArrayList<>(mCachedResults);
    }

    /**
     * Update the results when data changes
     */
    protected final void updateResults(List<E> results) {
        boolean needNotify = false;

        if (results == null) {
            if (mCachedResults != null) {
                needNotify = true;
            }
        } else {
            needNotify = !results.equals(mCachedResults);
        }

        if (needNotify) {
            mCachedResults = results;
            mContentResolver.notifyChange(mUri, null);
        }
    }
}
