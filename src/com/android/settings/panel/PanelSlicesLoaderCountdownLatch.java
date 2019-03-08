/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.panel;

import android.net.Uri;

import androidx.slice.Slice;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Helper class to isolate the work tracking all of the {@link Slice Slices} being loaded.
 * <p>
 *     Uses a {@link CountDownLatch} and a {@link Set} of Slices to track how many
 *     Slices have been loaded. A Slice can only be counted as being loaded a single time, even
 *     when they get updated later.
 * <p>
 *     To use the class, pass the number of expected Slices to load into the constructor. For
 *     every Slice that loads, call {@link #markSliceLoaded(Uri)} with the corresponding
 *     {@link Uri}. Then check if all of the Slices have loaded with
 *     {@link #isPanelReadyToLoad()}, which will return {@code true} the first time after all
 *     Slices have loaded.
 */
public class PanelSlicesLoaderCountdownLatch {
    private final Set<Uri> mLoadedSlices;
    private final CountDownLatch mCountDownLatch;
    private boolean slicesReadyToLoad = false;

    public PanelSlicesLoaderCountdownLatch(int countdownSize) {
        mLoadedSlices = new HashSet<>();
        mCountDownLatch = new CountDownLatch(countdownSize);
    }

    /**
     * Checks if the {@param sliceUri} has been loaded: if not, then decrement the countdown
     * latch, and if so, then do nothing.
     */
    public void markSliceLoaded(Uri sliceUri) {
        if (mLoadedSlices.contains(sliceUri)) {
            return;
        }
        mLoadedSlices.add(sliceUri);
        mCountDownLatch.countDown();
    }

    /**
     * @return {@code true} if the Slice has already been loaded.
     */
    public boolean isSliceLoaded(Uri uri) {
        return mLoadedSlices.contains(uri);
    }

    /**
     * @return {@code true} when all Slices have loaded, and the Panel has not yet been loaded.
     */
    public boolean isPanelReadyToLoad() {
        /**
         * Use {@link slicesReadyToLoad} to track whether or not the Panel has been loaded. We
         * only want to animate the Panel a single time.
         */
        if ((mCountDownLatch.getCount() == 0) && !slicesReadyToLoad) {
            slicesReadyToLoad = true;
            return true;
        }
        return false;
    }
}