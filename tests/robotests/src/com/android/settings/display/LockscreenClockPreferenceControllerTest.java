/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LockscreenClockPreferenceControllerTest {

    private static final String TEST_KEY = "test_key";
    private static final String SETTING_KEY = Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK;

    private Context mContext;
    private ContentResolver mContentResolver;
    private LockscreenClockPreferenceController mController;

    private Resources mResources;

    @Mock
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mController = new LockscreenClockPreferenceController(mContext, TEST_KEY);
        mResources = spy(mContext.getResources());
        Context mClockContext = org.mockito.Mockito.mock(Context.class);
        when(mClockContext.getResources()).thenReturn(mResources);
    }

    @Test
    public void isChecked_SettingIs1_returnTrue() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 1);

        when(mResources.getInteger(com.android.internal.R.integer.config_doublelineClockDefault))
            .thenReturn(1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_SettingIs0_returnFalse() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 0);

        when(mResources.getInteger(com.android.internal.R.integer.config_doublelineClockDefault))
            .thenReturn(0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_SettingIsNotSet_returnTrue() {
        Settings.Secure.putString(mContentResolver, SETTING_KEY, null);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_true_SettingIsNot0() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isNotEqualTo(0);
    }

    @Test
    public void setChecked_false_SettingIs0() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isEqualTo(0);
    }
}
