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

package com.android.settings.widget;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class VideoPreferenceControllerTest {

    @Mock
    private VideoPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private VideoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new VideoPreferenceController(RuntimeEnvironment.application, "test_pref");
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_isAvailableUnsearchable() {
        final VideoPreference videoPreference = mock(VideoPreference.class);

        // Assign mock object to mVideoPreference in controller
        ReflectionHelpers.setField(mController, "mVideoPreference", videoPreference);
        doReturn(true).when(videoPreference).isAnimationAvailable();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_isUnsupportedOnDevice() {
        final VideoPreference videoPreference = mock(VideoPreference.class);

        // Assign mock object to mVideoPreference in controller
        ReflectionHelpers.setField(mController, "mVideoPreference", videoPreference);
        doReturn(false).when(videoPreference).isAnimationAvailable();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void onStop_shouldCallOnViewInvisibleOnPrefernece() {
        mController.displayPreference(mScreen);

        mController.onStop();

        verify(mPreference).onViewInvisible();
    }

    @Test
    public void onStart_shouldCallOnViewVisibleOnPrefernece() {
        mController.displayPreference(mScreen);

        mController.onStart();

        verify(mPreference).onViewVisible();
    }
}
