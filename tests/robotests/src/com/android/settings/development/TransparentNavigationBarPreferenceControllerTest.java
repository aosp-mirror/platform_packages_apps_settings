/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
public class TransparentNavigationBarPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private TransparentNavigationBarPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new TransparentNavigationBarPreferenceController(
                RuntimeEnvironment.application) {
            private boolean mEnabled;

            protected boolean isEnabled() {
                return mEnabled;
            }

            protected void setEnabled(boolean enabled) {
                mEnabled = enabled;
            }
        };
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void updateState_enabled_shouldCheckedPreference() {
        mController.setEnabled(true);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_disabled_shouldUncheckedPreference() {
        mController.setEnabled(false);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_preferenceChecked_shouldBeEnabled() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        assertTrue(mController.isEnabled());
    }

    @Test
    public void onPreferenceChange__preferenceUnchecked_shouldNotBeEnabled() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        assertFalse(mController.isEnabled());
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldNotBeEnabled() {
        mController.onDeveloperOptionsSwitchDisabled();

        assertFalse(mController.isEnabled());
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
