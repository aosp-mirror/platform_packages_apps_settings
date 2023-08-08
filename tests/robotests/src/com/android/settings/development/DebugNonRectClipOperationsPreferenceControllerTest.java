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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;
import android.view.ThreadedRenderer;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DebugNonRectClipOperationsPreferenceControllerTest {

    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    /**
     * 0: off
     * 1: Draw non-rectangular clip region in blue
     * 2: Highlight tested drawing commands in green
     */
    private String[] mListValues;
    private String[] mListSummaries;
    private Context mContext;
    private DebugNonRectClipOperationsPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mListValues = mContext.getResources().getStringArray(
                com.android.settingslib.R.array.show_non_rect_clip_values);
        mListSummaries = mContext.getResources().getStringArray(
                com.android.settingslib.R.array.show_non_rect_clip_entries);
        mController = new DebugNonRectClipOperationsPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_noValueSet_shouldSetEmptyString() {
        mController.onPreferenceChange(mPreference, null /* new value */);

        String mode = SystemProperties.get(
                ThreadedRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY);
        assertThat(mode).isEqualTo("");
    }

    @Test
    public void onPreferenceChange_option1Selected_shouldSetOption1() {
        mController.onPreferenceChange(mPreference, mListValues[1]);

        String mode = SystemProperties.get(
                ThreadedRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY);
        assertThat(mode).isEqualTo(mListValues[1]);
    }

    @Test
    public void updateState_option1Set_shouldUpdatePreferenceToOption1() {
        SystemProperties.set(ThreadedRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY,
                mListValues[1]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[1]);
        verify(mPreference).setSummary(mListSummaries[1]);
    }

    @Test
    public void updateState_option2Set_shouldUpdatePreferenceToOption2() {
        SystemProperties.set(ThreadedRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY,
                mListValues[2]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[2]);
        verify(mPreference).setSummary(mListSummaries[2]);
    }

    @Test
    public void updateState_noOptionSet_shouldDefaultToOption0() {
        SystemProperties.set(ThreadedRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY, null);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[0]);
        verify(mPreference).setSummary(mListSummaries[0]);
    }
}
