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

package com.android.settings.deviceinfo.firmwareversion;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class FirmwareVersionPreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Fragment mFragment;

    private Context mContext;
    private FirmwareVersionPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new FirmwareVersionPreferenceController(mContext, mFragment);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void displayPreference_shouldSetSummaryToBuildNumber() {
        mController.displayPreference(mScreen);

        verify(mPreference).setSummary(Build.VERSION.RELEASE);
    }

    @Test
    public void handlePreferenceTreeClick_samePreferenceKey_shouldStartDialogFragment() {
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
        when(mFragment.getChildFragmentManager()).thenReturn(
                mock(FragmentManager.class, Answers.RETURNS_DEEP_STUBS));

        mController.handlePreferenceTreeClick(mPreference);

        verify(mFragment).getChildFragmentManager();
    }

    @Test
    public void handlePreferenceTreeClick_unknownPreferenceKey_shouldDoNothingAndReturnFalse() {
        when(mPreference.getKey()).thenReturn("foobar");

        final boolean result = mController.handlePreferenceTreeClick(mPreference);

        assertThat(result).isFalse();
        verify(mFragment, never()).getChildFragmentManager();
    }
}
