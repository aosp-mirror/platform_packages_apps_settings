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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class StayAwakePreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private RestrictedSwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private Lifecycle mLifecycle;
    private ContentResolver mContentResolver;
    private StayAwakePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mController = new StayAwakePreferenceController(mContext, mLifecycle);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_turnOnStayAwake() {
        mController.onPreferenceChange(null, true);

        final int mode = Settings.Global.getInt(mContentResolver,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN, -1);

        assertThat(mode).isEqualTo(StayAwakePreferenceController.SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChanged_turnOffStayAwake() {
        mController.onPreferenceChange(null, false);

        final int mode = Settings.Global.getInt(mContentResolver,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN, -1);

        assertThat(mode).isEqualTo(StayAwakePreferenceController.SETTING_VALUE_OFF);
    }

    @Test
    public void updateState_preferenceShouldBeChecked() {
        Settings.Global.putInt(mContentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                StayAwakePreferenceController.SETTING_VALUE_ON);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(mContentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                StayAwakePreferenceController.SETTING_VALUE_OFF);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void displayPreference_expectSetDisabledByAdminToBeCalled() {
        mController = spy(mController);
        RestrictedLockUtils.EnforcedAdmin admin = Mockito.mock(
                RestrictedLockUtils.EnforcedAdmin.class);
        doReturn(admin).when(mController).checkIfMaximumTimeToLockSetByAdmin();
        mController.updateState(mPreference);

        verify(mPreference).setDisabledByAdmin(admin);
    }

    @Test
    public void observerOnChangeCalledWithSameUri_preferenceShouldBeUpdated() {
        Settings.Global.putInt(mContentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                StayAwakePreferenceController.SETTING_VALUE_ON);
        mController.onResume();
        mController.mSettingsObserver.onChange(false,
                Settings.Global.getUriFor(Settings.Global.STAY_ON_WHILE_PLUGGED_IN));

        verify(mPreference).setChecked(true);
    }

    @Test
    public void observerOnChangeCalledWithDifferentUri_preferenceShouldNotBeUpdated() {
        Settings.Global.putInt(mContentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                StayAwakePreferenceController.SETTING_VALUE_ON);
        mController.onResume();
        mController.mSettingsObserver.onChange(false, null);

        verify(mPreference, never()).setChecked(true);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();

        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
