/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AddSourceBadCodeState.AUDIO_STREAM_ADD_SOURCE_BAD_CODE_STATE_SUMMARY;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AddSourceBadCodeStateTest {
    private AddSourceBadCodeState mInstance;

    @Before
    public void setUp() {
        mInstance = AddSourceBadCodeState.getInstance();
    }

    @Test
    public void testGetInstance() {
        assertThat(mInstance).isNotNull();
        assertThat(mInstance).isInstanceOf(SyncedState.class);
    }

    @Test
    public void testGetSummary() {
        int summary = mInstance.getSummary();
        assertThat(summary).isEqualTo(AUDIO_STREAM_ADD_SOURCE_BAD_CODE_STATE_SUMMARY);
    }

    @Test
    public void testGetStateEnum() {
        AudioStreamsProgressCategoryController.AudioStreamState stateEnum =
                mInstance.getStateEnum();
        assertThat(stateEnum)
                .isEqualTo(
                        AudioStreamsProgressCategoryController.AudioStreamState
                                .ADD_SOURCE_BAD_CODE);
    }
}
