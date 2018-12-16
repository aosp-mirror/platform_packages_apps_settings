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

package com.android.settings.development.autofill;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.autofill.AutofillManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AutofillResetOptionsPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private AutofillResetOptionsPreferenceController mController;
    private AutofillTestingHelper mHelper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this); // TODO: use @Rule
        mContext = RuntimeEnvironment.application;
        mHelper = new AutofillTestingHelper(mContext);
        mController = new AutofillResetOptionsPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void handlePreferenceTreeClick_differentPreferenceKey_shouldNotReset() throws Exception {
        when(mPreference.getKey()).thenReturn("SomeRandomKey");

        mHelper.setLoggingLevel(4);
        mHelper.setMaxPartitionsSize(8);
        mHelper.setMaxVisibleDatasets(15);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();

        assertThat(mHelper.getLoggingLevel()).isEqualTo(4);
        assertThat(mHelper.getMaxPartitionsSize()).isEqualTo(8);
        assertThat(mHelper.getMaxVisibleDatasets()).isEqualTo(15);
    }

    @Test
    public void handlePreferenceTreeClick_correctPreferenceKey_shouldReset() throws Exception {
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());

        mHelper.setMaxPartitionsSize(16);
        mHelper.setMaxVisibleDatasets(23);
        mHelper.setLoggingLevel(42);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();

        assertThat(mHelper.getLoggingLevel()).isEqualTo(AutofillManager.DEFAULT_LOGGING_LEVEL);
        assertThat(mHelper.getMaxPartitionsSize())
                .isEqualTo(AutofillManager.DEFAULT_MAX_PARTITIONS_SIZE);
        assertThat(mHelper.getMaxVisibleDatasets()).isEqualTo(0);
    }
}
