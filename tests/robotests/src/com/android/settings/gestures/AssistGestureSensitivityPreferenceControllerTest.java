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

import android.content.Context;
import android.provider.Settings;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AssistGestureSensitivityPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFactory;
    private AssistGestureSensitivityPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mController = new AssistGestureSensitivityPreferenceController(mContext, null);
    }

    @Test
    public void isAvailable_whenSupportedAndEnabled_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_GESTURE_ENABLED, 1);
        when(mFactory.assistGestureFeatureProvider.isSupported(mContext)).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_whenSupportedAndDisabled_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_GESTURE_ENABLED, 0);
        when(mFactory.assistGestureFeatureProvider.isSupported(mContext)).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenUnsupportedAndEnabled_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_GESTURE_ENABLED, 1);
        when(mFactory.assistGestureFeatureProvider.isSupported(mContext)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenUnsupportedAndDisabled_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_GESTURE_ENABLED, 0);
        when(mFactory.assistGestureFeatureProvider.isSupported(mContext)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }
}

