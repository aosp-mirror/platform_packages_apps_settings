/*
 * Copyright (C) 2020 The Android Open Source Project
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


package com.android.settings.panel;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@Deprecated(forRemoval = true)
@RunWith(AndroidJUnit4.class)
public class PanelSlicesLoaderCountdownLatchTest {

    private Context mContext;
    private PanelSlicesLoaderCountdownLatch mSliceCountdownLatch;

    private static final Uri[] URIS = new Uri[] {
      Uri.parse("content://testUri"),
      Uri.parse("content://wowUri"),
      Uri.parse("content://boxTurtle")
    };

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mSliceCountdownLatch = new PanelSlicesLoaderCountdownLatch(URIS.length);
    }


    @Test
    public void hasLoaded_newObject_returnsFalse() {
        assertThat(mSliceCountdownLatch.isSliceLoaded(URIS[0])).isFalse();
    }

    @Test
    public void hasLoaded_markSliceLoaded_returnsTrue() {
        mSliceCountdownLatch.markSliceLoaded(URIS[0]);

        assertThat(mSliceCountdownLatch.isSliceLoaded(URIS[0])).isTrue();
    }

    @Test
    public void markSliceLoaded_onlyCountsDownUniqueUris() {
        for (int i = 0; i < URIS.length; i++) {
            mSliceCountdownLatch.markSliceLoaded(URIS[0]);
        }

        assertThat(mSliceCountdownLatch.isPanelReadyToLoad()).isFalse();
    }

    @Test
    public void areSlicesReadyToLoad_allSlicesLoaded_returnsTrue() {
        for (int i = 0; i < URIS.length; i++) {
            mSliceCountdownLatch.markSliceLoaded(URIS[i]);
        }

        assertThat(mSliceCountdownLatch.isPanelReadyToLoad()).isTrue();
    }

    @Test
    public void areSlicesReadyToLoad_onlyReturnsTrueOnce() {
        for (int i = 0; i < URIS.length; i++) {
            mSliceCountdownLatch.markSliceLoaded(URIS[i]);
        }

        // Verify that it returns true once
        assertThat(mSliceCountdownLatch.isPanelReadyToLoad()).isTrue();
        // Verify the second call returns false without external state change
        assertThat(mSliceCountdownLatch.isPanelReadyToLoad()).isFalse();
    }
}
