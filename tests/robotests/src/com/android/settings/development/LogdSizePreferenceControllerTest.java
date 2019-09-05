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

package com.android.settings.development;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LogdSizePreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private ListPreference mPreference;

    /**
     * List Values
     *
     * 0: off
     * 1: 64k
     * 2: 256k
     * 3: 1M
     * 4: 4M
     * 5: 16M
     */
    private String[] mListValues;
    private String[] mListSummaries;
    private Context mContext;
    private LogdSizePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mListValues = mContext.getResources().getStringArray(R.array.select_logd_size_values);
        mListSummaries = mContext.getResources().getStringArray(R.array.select_logd_size_summaries);
        mController = new LogdSizePreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisableAndResetPreferenceToDefault() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setValue(mListValues[2]);
        verify(mPreference).setSummary(mListSummaries[2]);
        verify(mPreference).setEnabled(false);
    }
}
