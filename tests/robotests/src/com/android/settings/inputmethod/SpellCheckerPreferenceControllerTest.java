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

import android.content.Context;
import android.support.v7.preference.Preference;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.TextServicesManager;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SpellCheckerPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private TextServicesManager mTextServicesManager;
    private Context mAppContext;
    private Preference mPreference;
    private SpellCheckerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAppContext = RuntimeEnvironment.application;
        when(mContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE))
                .thenReturn(mTextServicesManager);
        mPreference = new Preference(mAppContext);
        mController = new SpellCheckerPreferenceController(mContext);
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
