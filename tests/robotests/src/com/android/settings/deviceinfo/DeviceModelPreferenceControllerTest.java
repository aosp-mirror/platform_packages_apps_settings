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
package com.android.settings.deviceinfo;


import android.app.Fragment;
import android.content.Context;
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DeviceModelPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Fragment mFragment;
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    private DeviceModelPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DeviceModelPreferenceController(mContext, mFragment);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void displayPref_shouldSetSummary() {
        mController.displayPreference(mPreferenceScreen);

        verify(mPreference).setSummary(anyString());
    }

    @Test
    public void clickPreference_shouldLaunchHardwareInfoDialog() {
        assertThat(mController.handlePreferenceTreeClick(mPreference))
                .isTrue();
        verify(mFragment).getFragmentManager();
        verify(mFragment.getFragmentManager().beginTransaction())
                .add(any(HardwareInfoDialogFragment.class), eq(HardwareInfoDialogFragment.TAG));
    }
}
