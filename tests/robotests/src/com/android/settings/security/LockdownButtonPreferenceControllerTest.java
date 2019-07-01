/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import com.android.internal.widget.LockPatternUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class LockdownButtonPreferenceControllerTest {

    @Mock
    private LockPatternUtils mLockPatternUtils;
    private SwitchPreference mPreference;

    private Context mContext;
    private LockdownButtonPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);

        mController = spy(new LockdownButtonPreferenceController(mContext, "TestKey"));
        ReflectionHelpers.setField(mController, "mLockPatternUtils", mLockPatternUtils);
    }

    @Test
    public void isAvailable_lockSet_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_lockUnset_shouldReturnFalse() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceChange_settingIsUpdated() {
        boolean state = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCKDOWN_IN_POWER_MENU, 0) != 0;
        assertThat(mController.onPreferenceChange(mPreference, !state)).isTrue();
        boolean newState = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCKDOWN_IN_POWER_MENU, 0) != 0;
        assertThat(newState).isEqualTo(!state);
    }

    @Test
    public void onSettingChange_preferenceIsUpdated() {
        boolean state = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCKDOWN_IN_POWER_MENU, 0) != 0;
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isEqualTo(state);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCKDOWN_IN_POWER_MENU, state ? 0 : 1);

        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isEqualTo(!state);
    }
}
