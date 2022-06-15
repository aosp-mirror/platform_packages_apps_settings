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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.TextServicesManager;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SpellCheckerPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private TextServicesManager mTextServicesManager;
    @Mock
    private Resources mResources;

    private Context mAppContext;
    private Preference mPreference;
    private SpellCheckerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAppContext = RuntimeEnvironment.application;
        when(mContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE))
                .thenReturn(mTextServicesManager);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(R.bool.config_show_spellcheckers_settings)).thenReturn(true);
        mPreference = new Preference(mAppContext);
        mController = new SpellCheckerPreferenceController(mContext);
    }

    @Test
    public void testSpellChecker_byDefault_shouldBeShown() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testSpellChecker_ifDisabled_shouldNotBeShown() {
        when(mResources.getBoolean(R.bool.config_show_spellcheckers_settings)).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_NoSpellerChecker_shouldSetSummaryToDefault() {
        when(mTextServicesManager.isSpellCheckerEnabled()).thenReturn(true);
        when(mTextServicesManager.getCurrentSpellChecker()).thenReturn(null);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mAppContext.getString(R.string.spell_checker_not_selected));
    }

    @Test
    public void updateState_spellerCheckerDisabled_shouldSetSummaryToDisabledText() {
        when(mTextServicesManager.isSpellCheckerEnabled()).thenReturn(false);

        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mAppContext.getString(R.string.switch_off_text));
    }

    @Test
    public void updateState_hasSpellerChecker_shouldSetSummaryToAppName() {
        final String spellCheckerAppLabel = "test";
        final SpellCheckerInfo sci = mock(SpellCheckerInfo.class);
        when(mTextServicesManager.isSpellCheckerEnabled()).thenReturn(true);
        when(mTextServicesManager.getCurrentSpellChecker()).thenReturn(sci);
        when(sci.loadLabel(mContext.getPackageManager())).thenReturn(spellCheckerAppLabel);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(spellCheckerAppLabel);
    }
}
