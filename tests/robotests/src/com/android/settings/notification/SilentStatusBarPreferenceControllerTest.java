/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SilentStatusBarPreferenceControllerTest {

    @Mock
    private NotificationBackend mBackend;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private SilentStatusBarPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new SilentStatusBarPreferenceController(mContext);
        mController.setBackend(mBackend);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isChecked_settingIsOff() {
        when(mBackend.shouldHideSilentStatusBarIcons(any())).thenReturn(false);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingIsOn() {
        when(mBackend.shouldHideSilentStatusBarIcons(any())).thenReturn(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_Off() {
        mController.setChecked(false);
        verify(mBackend, times(1)).setHideSilentStatusIcons(false);
    }

    @Test
    public void setChecked_On() {
        mController.setChecked(true);
        verify(mBackend, times(1)).setHideSilentStatusIcons(true);
    }
}