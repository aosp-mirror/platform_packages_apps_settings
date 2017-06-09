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
import android.support.v7.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PhoneLanguagePreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private Preference mPreference;
    private FakeFeatureFactory mFeatureFactory;
    private PhoneLanguagePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mController = new PhoneLanguagePreferenceController(mContext);
    }

    @Test
    public void testIsAvailable_hasMultipleLocales_shouldReturnTrue() {
        when(mContext.getAssets().getLocales()).thenReturn(new String[]{"en", "de"});

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_hasSingleLocales_shouldReturnFalse() {
        when(mContext.getAssets().getLocales()).thenReturn(new String[]{"en"});

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testUpdateState_shouldUpdateSummary() {
        final String testSummary = "test";
        when(mFeatureFactory.localeFeatureProvider.getLocaleNames()).thenReturn(testSummary);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(testSummary);
    }
}
