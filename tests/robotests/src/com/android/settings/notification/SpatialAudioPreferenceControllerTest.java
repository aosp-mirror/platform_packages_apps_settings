/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.notification;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.AudioManager;
import android.media.Spatializer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@Ignore("b/200896161")
@RunWith(RobolectricTestRunner.class)
public class SpatialAudioPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private AudioManager mAudioManager;
    @Mock
    private Spatializer mSpatializer;

    private SpatialAudioPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(AudioManager.class)).thenReturn(mAudioManager);
        when(mAudioManager.getSpatializer()).thenReturn(mSpatializer);
        mController = new SpatialAudioPreferenceController(mContext);
    }

    @Test
    public void getAvailabilityStatus_levelNone_shouldReturnUnsupported() {
        when(mSpatializer.getImmersiveAudioLevel()).thenReturn(
                Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_levelMultiChannel_shouldReturnAvailable() {
        when(mSpatializer.getImmersiveAudioLevel()).thenReturn(
                Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setChecked_withTrue_shouldEnableSpatializer() {
        mController.setChecked(true);

        verify(mSpatializer).setEnabled(true);
    }

    @Test
    public void setChecked_withFalse_shouldDisableSpatializer() {
        mController.setChecked(false);

        verify(mSpatializer).setEnabled(false);
    }
}
