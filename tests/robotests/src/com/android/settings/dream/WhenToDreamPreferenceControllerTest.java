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

package com.android.settings.dream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.WhenToDream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class WhenToDreamPreferenceControllerTest {

    private WhenToDreamPreferenceController mController;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private DreamBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mController = new WhenToDreamPreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
    }

    @Test
    public void updateSummary() {
        // Don't have to test the other settings because DreamSettings tests that all
        // @WhenToDream values map to the correct ResId
        final @WhenToDream int testSetting = DreamBackend.WHILE_CHARGING;
        final Preference mockPref = mock(Preference.class);
        when(mockPref.getContext()).thenReturn(mContext);
        when(mBackend.getWhenToDreamSetting()).thenReturn(testSetting);
        final String expectedString =
                mContext.getString(DreamSettings.getDreamSettingDescriptionResId(testSetting));

        mController.updateState(mockPref);
        verify(mockPref).setSummary(expectedString);
    }
}
