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

package com.android.settings.wifi.p2p;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class P2pCategoryPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceCategory mCategory;
    private P2pCategoryPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mCategory);

        mController = new P2pCategoryPreferenceController(RuntimeEnvironment.application) {
            @Override
            public String getPreferenceKey() {
                return "test_key";
            }
        };
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void isAvailable_withInitialEmptyGroup_shouldBeFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void removeAllChildren_shouldRemove() {
        mController.removeAllChildren();

        verify(mCategory).removeAll();
        verify(mCategory, times(2)).setVisible(false);
    }

    @Test
    public void addChild_shouldAdd() {
        final Preference pref = new Preference(RuntimeEnvironment.application);
        mController.addChild(pref);

        verify(mCategory).addPreference(pref);
        verify(mCategory, atLeastOnce()).setVisible(true);
        verify(mCategory).setVisible(false);
    }

    @Test
    public void shouldToggleEnable() {
        mController.setEnabled(false);

        verify(mCategory).setEnabled(false);

        mController.setEnabled(true);

        verify(mCategory).setEnabled(true);
    }
}
