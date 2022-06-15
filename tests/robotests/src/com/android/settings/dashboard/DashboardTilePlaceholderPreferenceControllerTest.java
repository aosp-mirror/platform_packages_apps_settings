/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;

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
public class DashboardTilePlaceholderPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    private DashboardTilePlaceholderPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DashboardTilePlaceholderPreferenceController(mContext);
    }

    @Test
    public void display_hasPlaceholderPref_shouldUseOrderFromPlaceholder() {
        final int baseOrder = 15;
        when(mScreen.findPreference(anyString()).getOrder()).thenReturn(baseOrder);

        mController.displayPreference(mScreen);

        assertThat(mController.getOrder()).isEqualTo(baseOrder);
    }

    @Test
    public void display_noPlaceholderPref_shouldUseDefaultOrder() {
        when(mScreen.findPreference(anyString())).thenReturn(null);

        mController.displayPreference(mScreen);

        assertThat(mController.getOrder()).isEqualTo(Preference.DEFAULT_ORDER);
    }
}
