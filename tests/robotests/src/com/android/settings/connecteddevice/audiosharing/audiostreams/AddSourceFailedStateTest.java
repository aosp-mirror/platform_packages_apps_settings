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

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AddSourceFailedState.AUDIO_STREAM_ADD_SOURCE_FAILED_STATE_SUMMARY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AddSourceFailedStateTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private AudioStreamPreference mPreference;
    @Mock private AudioStreamsProgressCategoryController mController;
    @Mock private AudioStreamsHelper mHelper;
    private FakeFeatureFactory mFeatureFactory;
    private AddSourceFailedState mInstance;

    @Before
    public void setUp() {
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mInstance = new AddSourceFailedState();
    }

    @Test
    public void testGetInstance() {
        mInstance = AddSourceFailedState.getInstance();
        assertThat(mInstance).isNotNull();
        assertThat(mInstance).isInstanceOf(SyncedState.class);
    }

    @Test
    public void testGetSummary() {
        int summary = mInstance.getSummary();
        assertThat(summary).isEqualTo(AUDIO_STREAM_ADD_SOURCE_FAILED_STATE_SUMMARY);
    }

    @Test
    public void testGetStateEnum() {
        AudioStreamsProgressCategoryController.AudioStreamState stateEnum =
                mInstance.getStateEnum();
        assertThat(stateEnum)
                .isEqualTo(
                        AudioStreamsProgressCategoryController.AudioStreamState.ADD_SOURCE_FAILED);
    }

    @Test
    public void testPerformAction() {
        when(mPreference.getContext()).thenReturn(mContext);
        when(mPreference.getSourceOriginForLogging())
                .thenReturn(SourceOriginForLogging.QR_CODE_SCAN_SETTINGS);

        mInstance.performAction(mPreference, mController, mHelper);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        eq(mContext),
                        eq(SettingsEnums.ACTION_AUDIO_STREAM_JOIN_FAILED_OTHER),
                        eq(SourceOriginForLogging.QR_CODE_SCAN_SETTINGS.ordinal()));
    }
}
