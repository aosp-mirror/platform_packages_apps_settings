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
import android.annotation.Nullable;
import android.content.Context;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Slice background worker is used to make Settings Slices be able to work with data that is
 * changing continuously, e.g. available Wi-Fi networks.
 *
 * The background worker will be started at {@link SettingsSliceProvider#onSlicePinned(Uri)}, be
 * stopped at {@link SettingsSliceProvider#onSliceUnpinned(Uri)}, and be closed at {@link
 * SettingsSliceProvider#shutdown()}.
 *
 * {@link SliceBackgroundWorker} caches the results, uses the cache to compare if there is any data
 * changed, and then notifies the Slice {@link Uri} to update.
 *
 * It also stores all instances of all workers to ensure each worker is a Singleton.
 */
public abstract class SliceBackgroundWorker<E> implements Closeable {

    private static final String TAG = "SliceBackgroundWorker";

    private static final Map<Uri, SliceBackgroundWorker> LIVE_WORKERS = new ArrayMap<>();

    private final Context mContext;
    private final Uri mUri;

    private List<E> mCachedResults;

    protected SliceBackgroundWorker(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
    }

    protected Uri getUri() {
        return mUri;
    }

    protected Context getContext() {
        return mContext;
    }

    /**
     * Returns the singleton instance of {@link SliceBackgroundWorker} for specified {@link Uri} if
     * exists
     */
    @Nullable
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T extends SliceBackgroundWorker> T getInstance(Uri uri) {
        return (T) LIVE_WORKERS.get(uri);
    }

    /**
     * Returns the singleton instance of {@link SliceBackgroundWorker} for specified {@link
     * CustomSliceable}
     */
    static SliceBackgroundWorker getInstance(Context context, Sliceable sliceable, Uri uri) {
        SliceBackgroundWorker worker = getInstance(uri);
        if (worker == null) {
            final Class<? extends SliceBackgroundWorker> workerClass =
                    sliceable.getBackgroundWorkerClass();
            worker = createInstance(context.getApplicationContext(), uri, workerClass);
            LIVE_WORKERS.put(uri, worker);
        }
        return worker;
    }

    private static SliceBackgroundWorker createInstance(Context context, Uri uri,
            Class<? extends SliceBackgroundWorker> clazz) {
        Log.d(TAG, "create instance: " + clazz);
        try {
            return clazz.getConstructor(Context.class, Uri.class).newInstance(context, uri);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
                InvocationTargetException e) {
            throw new IllegalStateException(
                    "Invalid slice background worker: " + clazz, e);
        }
    }

    static void shutdown() {
        for (SliceBackgroundWorker worker : LIVE_WORKERS.values()) {
            try {
                worker.close();
            } catch (IOException e) {
                Log.w(TAG, "Shutting down worker failed", e);
            }
        }
        LIVE_WORKERS.clear();
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
            needNotify = !areListsTheSame(results, mCachedResults);
        }

        if (needNotify) {
            mCachedResults = results;
            notifySliceChange();
        }
    }

    protected boolean areListsTheSame(List<E> a, List<E> b) {
        return a.equals(b);
    }

    /**
     * Notify that data was updated and attempt to sync changes to the Slice.
     */
    protected final void notifySliceChange() {
        mContext.getContentResolver().notifyChange(mUri, null);
    }
}
