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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FeedbackPreferenceControllerTest {

    @Mock
    private Fragment mFragment;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private FeedbackPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new FeedbackPreferenceController(mFragment, mContext);
        final String prefKey = mController.getPreferenceKey();
        when(mScreen.findPreference(prefKey)).thenReturn(mPreference);
    }

    @Test
    public void isAvailable_noReporterPackage_shouldReturnFalse() {
        when(mContext.getResources().getString(anyInt())).thenReturn("");
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isVisible_afterUpdateState_shouldBeSameAsIsAvailable() {
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isEqualTo(mController.isAvailable());
    }
}
