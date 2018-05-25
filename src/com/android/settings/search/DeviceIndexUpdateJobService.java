/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.search;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static com.android.settings.search.DeviceIndexFeatureProvider.createDeepLink;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.SettingsSlicesContract;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.slices.SliceDeepLinkSpringBoard;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceViewManager;
import androidx.slice.SliceViewManager.SliceCallback;
import androidx.slice.SliceMetadata;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.ListContent;

public class DeviceIndexUpdateJobService extends JobService {

    private static final String TAG = "DeviceIndexUpdate";
    private static final boolean DEBUG = false;
    @VisibleForTesting
    protected boolean mRunningJob;

    @Override
    public boolean onStartJob(JobParameters params) {
        if (DEBUG) Log.d(TAG, "onStartJob");
        if (!mRunningJob) {
            mRunningJob = true;
            Thread thread = new Thread(() -> updateIndex(params));
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (DEBUG) Log.d(TAG, "onStopJob " + mRunningJob);
        if (mRunningJob) {
            mRunningJob = false;
            return true;
        }
        return false;
    }

    @VisibleForTesting
    protected void updateIndex(JobParameters params) {
        if (DEBUG) {
            Log.d(TAG, "Starting index");
        }
        final DeviceIndexFeatureProvider indexProvider = FeatureFactory.getFactory(this)
                .getDeviceIndexFeatureProvider();
        final SliceViewManager manager = getSliceViewManager();
        final Uri baseUri = new Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .build();
        final Uri platformBaseUri = new Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .build();
        final Collection<Uri> slices = manager.getSliceDescendants(baseUri);
        slices.addAll(manager.getSliceDescendants(platformBaseUri));

        if (DEBUG) {
            Log.d(TAG, "Indexing " + slices.size() + " slices");
        }

        indexProvider.clearIndex(this /* context */);

        for (Uri slice : slices) {
            if (!mRunningJob) {
                return;
            }
            Slice loadedSlice = bindSliceSynchronous(manager, slice);
            // TODO: Get Title APIs on SliceMetadata and use that.
            SliceMetadata metaData = getMetadata(loadedSlice);
            CharSequence title = findTitle(loadedSlice, metaData);
            if (title != null) {
                if (DEBUG) {
                    Log.d(TAG, "Indexing: " + slice + " " + title + " " + loadedSlice);
                }
                indexProvider.index(this, title, slice, createDeepLink(
                        new Intent(SliceDeepLinkSpringBoard.ACTION_VIEW_SLICE)
                                .setPackage(getPackageName())
                                .putExtra(SliceDeepLinkSpringBoard.EXTRA_SLICE, slice.toString())
                                .toUri(Intent.URI_ANDROID_APP_SCHEME)),
                        metaData.getSliceKeywords());
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Done indexing");
        }
        jobFinished(params, false);
    }

    protected SliceViewManager getSliceViewManager() {
        return SliceViewManager.getInstance(this);
    }

    protected SliceMetadata getMetadata(Slice loadedSlice) {
        return SliceMetadata.from(this, loadedSlice);
    }

    protected CharSequence findTitle(Slice loadedSlice, SliceMetadata metaData) {
        ListContent content = new ListContent(null, loadedSlice);
        SliceItem headerItem = content.getHeaderItem();
        if (headerItem == null) {
            if (content.getRowItems().size() != 0) {
                headerItem = content.getRowItems().get(0);
            } else {
                return null;
            }
        }
        // Look for a title, then large text, then any text at all.
        SliceItem title = SliceQuery.find(headerItem, FORMAT_TEXT, HINT_TITLE, null);
        if (title != null) {
            return title.getText();
        }
        title = SliceQuery.find(headerItem, FORMAT_TEXT, HINT_LARGE, null);
        if (title != null) {
            return title.getText();
        }
        title = SliceQuery.find(headerItem, FORMAT_TEXT);
        if (title != null) {
            return title.getText();
        }
        return null;
    }

    protected Slice bindSliceSynchronous(SliceViewManager manager, Uri slice) {
        final Slice[] returnSlice = new Slice[1];
        CountDownLatch latch = new CountDownLatch(1);
        SliceCallback callback = new SliceCallback() {
            @Override
            public void onSliceUpdated(Slice s) {
                try {
                    SliceMetadata m = SliceMetadata.from(DeviceIndexUpdateJobService.this, s);
                    if (m.getLoadingState() == SliceMetadata.LOADED_ALL) {
                        returnSlice[0] = s;
                        latch.countDown();
                        manager.unregisterSliceCallback(slice, this);
                    }
                } catch (Exception e) {
                    Log.w(TAG, slice + " cannot be indexed", e);
                    returnSlice[0] = s;
                }
            }
        };
        // Register a callback until we get a loaded slice.
        manager.registerSliceCallback(slice, callback);
        // Trigger the first bind in case no loading is needed.
        callback.onSliceUpdated(manager.bindSlice(slice));
        try {
            latch.await();
        } catch (InterruptedException e) {
        }
        return returnSlice[0];
    }
}
