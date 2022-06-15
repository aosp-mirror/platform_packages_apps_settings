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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings.Global;

import androidx.fragment.app.FragmentActivity;
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
public class DockingSoundPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private FragmentActivity mActivity;
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private SoundSettings mSetting;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;

    private DockingSoundPreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mSetting.getActivity()).thenReturn(mActivity);
        when(mActivity.getContentResolver()).thenReturn(mContentResolver);
        mPreference = new SwitchPreference(RuntimeEnvironment.application);
        when(mActivity.getResources().getBoolean(com.android.settings.R.bool.has_dock_settings))
            .thenReturn(true);
        mController = new DockingSoundPreferenceController(mContext, mSetting, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        doReturn(mScreen).when(mSetting).getPreferenceScreen();
    }

    @Test
    public void isAvailable_hasDockSettings_shouldReturnTrue() {
        when(mContext.getResources().getBoolean(com.android.settings.R.bool.has_dock_settings))
            .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noDockSettings_shouldReturnFalse() {
        when(mContext.getResources().getBoolean(com.android.settings.R.bool.has_dock_settings))
            .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_dockingSoundEnabled_shouldCheckedPreference() {
        Global.putInt(mContentResolver, Global.DOCK_SOUNDS_ENABLED, 1);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void displayPreference_dockingSoundDisabled_shouldUncheckedPreference() {
        Global.putInt(mContentResolver, Global.DOCK_SOUNDS_ENABLED, 0);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceChanged_preferenceChecked_shouldEnabledDockingSound() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, true);

        assertThat(Global.getInt(mContentResolver, Global.DOCK_SOUNDS_ENABLED, 1)).isEqualTo(1);
    }

    @Test
    public void onPreferenceChanged_preferenceUnchecked_shouldDisabledDockingSound() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, false);

        assertThat(Global.getInt(mContentResolver, Global.DOCK_SOUNDS_ENABLED, 1)).isEqualTo(0);
    }
}
