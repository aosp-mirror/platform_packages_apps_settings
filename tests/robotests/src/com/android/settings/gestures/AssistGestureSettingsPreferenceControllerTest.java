/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSecureSettings.class)
public class AssistGestureSettingsPreferenceControllerTest {

    private static final String KEY_ASSIST = "gesture_assist";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFactory;
    private AssistGestureSettingsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFactory = FakeFeatureFactory.setupForTest();
        mController = new AssistGestureSettingsPreferenceController(mContext, KEY_ASSIST);
        mController.setAssistOnly(false);
    }

    @Test
    public void isAvailable_whenSupported_shouldReturnTrue() {
        mController.mAssistOnly = false;
        when(mFactory.assistGestureFeatureProvider.isSensorAvailable(mContext)).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_whenUnsupported_shouldReturnFalse() {
        when(mFactory.assistGestureFeatureProvider.isSupported(mContext)).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }
}

