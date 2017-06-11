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

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
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
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }


    @Test
    public void removeAllChildren_shouldRemove() {
        mController.removeAllChildren();

        verify(mCategory).removeAll();
        verify(mCategory).setVisible(false);
    }

    @Test
    public void addChild_shouldAdd() {
        final Preference pref = new Preference(RuntimeEnvironment.application);
        mController.addChild(pref);

        verify(mCategory).addPreference(pref);
        verify(mCategory).setVisible(true);
    }

    @Test
    public void shouldToggleEnable() {
        mController.setEnabled(false);

        verify(mCategory).setEnabled(false);

        mController.setEnabled(true);

        verify(mCategory).setEnabled(true);
    }
}
