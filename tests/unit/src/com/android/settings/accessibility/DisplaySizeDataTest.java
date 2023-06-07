/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.AsyncTask;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class DisplaySizeDataTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private DisplaySizeData mDisplaySizeData;
    private int mInitialIndex;

    @Before
    public void setUp() {
        mDisplaySizeData = new DisplaySizeData(mContext);
        mInitialIndex = mDisplaySizeData.getInitialIndex();
    }

    @After
    public void cleanUp() throws InterruptedException {
        mDisplaySizeData.commit(mInitialIndex);
        waitForDisplayChangesSynchronously();
    }

    @Test
    public void commit_success() throws InterruptedException {
        final int progress = mDisplaySizeData.getValues().size() - 1;
        Assume.assumeTrue("We need more default display size to make the test effective",
                mInitialIndex != progress && progress > 0);

        mDisplaySizeData.commit(progress);
        waitForDisplayChangesSynchronously();

        final int density = mContext.getResources().getDisplayMetrics().densityDpi;
        assertThat(density).isEqualTo(mDisplaySizeData.getValues().get(progress));
    }

    /**
     * Wait for the display change propagated synchronously.
     * <p/>
     * Note: Currently, DisplayDensityUtils uses AsyncTask to change the display density
     * asynchronously. If in the future we stop using the deprecated AsyncTask, we will need to
     * update the wait mechanism in the test.
     */
    private void waitForDisplayChangesSynchronously() throws InterruptedException {
        // The default AsyncTask.execute run tasks in serial order.
        // Posting a new runnable and wait for it to finish means the previous tasks are all done.
        CountDownLatch latch = new CountDownLatch(1);
        AsyncTask.execute(latch::countDown);
        latch.await(5, TimeUnit.SECONDS);
    }
}
