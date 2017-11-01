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

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TtsEngines;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class TtsPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private TtsEngines mTtsEngines;
    @Mock
    private PreferenceScreen mScreen;

    private TtsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new TtsPreferenceController(mContext, mTtsEngines);
    }

    @Test
    public void testIsAvailable_ttsEngineEmpty_shouldReturnFalse() {

        // Not available when there is no engine.
        when(mTtsEngines.getEngines()).thenReturn(new ArrayList<>());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_ttsEngineInstalled_shouldReturnTrue() {
        final List<TextToSpeech.EngineInfo> infolist = new ArrayList<>();
        infolist.add(mock(TextToSpeech.EngineInfo.class));
        when(mTtsEngines.getEngines()).thenReturn(infolist);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void displayPreference_notAvailable_shouldRemoveCategory() {
        final Preference preference = mock(Preference.class);
        final Preference category = mock(Preference.class);
        when(mScreen.getPreferenceCount()).thenReturn(2);
        when(mScreen.getPreference(0)).thenReturn(preference);
        when(mScreen.getPreference(1)).thenReturn(category);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());
        when(category.getKey()).thenReturn("voice_category");

        mController.displayPreference(mScreen);

        // Remove preference.
        verify(mScreen).removePreference(any(Preference.class));
    }
}
