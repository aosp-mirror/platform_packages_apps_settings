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

package com.android.settings.language;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TtsEngines;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TtsPreferenceControllerTest {

    @Mock
    private TtsEngines mTtsEngines;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private TtsPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mController = new TtsPreferenceController(mContext, "test_key");
        mController.mTtsEngines = mTtsEngines;
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    public void testIsAvailable_ttsEngineEmpty_shouldReturnFalse() {
        // Not available when there is no engine.
        when(mTtsEngines.getEngines()).thenReturn(new ArrayList<>());

        assertThat(mController.isAvailable()).isFalse();

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void testIsAvailable_ttsEngineInstalled_shouldReturnTrue() {
        final List<TextToSpeech.EngineInfo> infolist = new ArrayList<>();
        infolist.add(mock(TextToSpeech.EngineInfo.class));
        when(mTtsEngines.getEngines()).thenReturn(infolist);

        assertThat(mController.isAvailable()).isTrue();

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testIsAvailable_ifDisabled_shouldReturnFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }
}
