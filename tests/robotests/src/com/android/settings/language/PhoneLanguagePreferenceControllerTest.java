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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;

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
public class PhoneLanguagePreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private AssetManager mAssets;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private PhoneLanguagePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getAssets()).thenReturn(mAssets);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new PhoneLanguagePreferenceController(mContext, "key");
    }

    @Test
    public void testIsAvailable_hasMultipleLocales_shouldReturnTrue() {
        when(mAssets.getLocales()).thenReturn(new String[] {"en", "de"});

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_hasSingleLocales_shouldReturnFalse() {
        when(mAssets.getLocales()).thenReturn(new String[] {"en"});

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testGetAvailabilityStatus_hasMultipleLocales_returnAvailable() {
        when(mAssets.getLocales()).thenReturn(new String[] {"en", "de"});

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
        public void testGetAvailabilityStatus_hasSingleLocales_returnConditionallyUnavailable() {
        when(mAssets.getLocales()).thenReturn(new String[] {"en"});

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testIsAvailable_ifDisabled_shouldReturnFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testUpdateState_shouldUpdateSummary() {
        final String testSummary = "test";
        when(mFeatureFactory.localeFeatureProvider.getLocaleNames()).thenReturn(testSummary);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(testSummary);
    }

    @Test
    public void testUpdateNonIndexable_shouldAddKey() {
        final List<String> niks = new ArrayList<>();
        mController.updateNonIndexableKeys(niks);
        assertThat(niks).containsExactly(mController.getPreferenceKey());
    }
}
