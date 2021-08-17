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
 * limitations under the License.
 */

package com.android.settings.dashboard;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class UiBlockerControllerTest {
    private static final long TIMEOUT = 600;
    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key2";

    private Instrumentation mInstrumentation;
    private UiBlockerController mSyncableController;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();

        mSyncableController = new UiBlockerController(Arrays.asList(KEY_1, KEY_2));
    }

    @Test
    public void start_isSyncedReturnFalseUntilAllWorkDone() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mSyncableController.start(() -> latch.countDown());

        // Return false at first
        assertThat(mSyncableController.isBlockerFinished()).isFalse();

        // Return false if only one job is done
        mSyncableController.countDown(KEY_1);
        assertThat(mSyncableController.isBlockerFinished()).isFalse();

        // Return true if all jobs done
        mSyncableController.countDown(KEY_2);
        assertThat(latch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(mSyncableController.isBlockerFinished()).isTrue();
    }
}
