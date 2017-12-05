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
package com.android.settings.deviceinfo;

import android.app.Fragment;
import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class FeedbackPreferenceControllerTest {
    @Mock
    private Fragment mFragment;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FeedbackPreferenceController mController;

    public FeedbackPreferenceControllerTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mController = new FeedbackPreferenceController(this.mFragment, this.mContext);
    }

    @Test
    public void isAvailable_noReporterPackage_shouldReturnFalse() {
        when(this.mContext.getResources().getString(anyInt())).thenReturn("");
        assertThat(Boolean.valueOf(this.mController.isAvailable())).isFalse();
    }
}
