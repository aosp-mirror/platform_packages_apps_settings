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

package com.android.settings.development;

import static android.provider.Settings.Global.DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT;
import static android.provider.Settings.Global.DEVELOPMENT_FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS;

import static com.android.settings.development.DesktopModeSecondaryDisplayPreferenceController.SETTING_VALUE_OFF;
import static com.android.settings.development.DesktopModeSecondaryDisplayPreferenceController.SETTING_VALUE_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class DesktopModeSecondaryDisplayPreferenceControllerTest {

    private static final String ENG_BUILD_TYPE = "eng";
    private static final String USER_BUILD_TYPE = "user";
    private static final int SETTING_VALUE_INVALID = -1;

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;
    @Mock
    private FragmentActivity mActivity;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mTransaction;

    private Context mContext;
    private DesktopModeSecondaryDisplayPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        doReturn(mTransaction).when(mFragmentManager).beginTransaction();
        doReturn(mFragmentManager).when(mActivity).getSupportFragmentManager();
        doReturn(mActivity).when(mFragment).getActivity();
        mController = new DesktopModeSecondaryDisplayPreferenceController(mContext, mFragment);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable_engBuild_shouldBeTrue() {
        mController = spy(mController);
        doReturn(ENG_BUILD_TYPE).when(mController).getBuildType();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_userBuild_shouldBeTrue() {
        mController = spy(mController);
        doReturn(USER_BUILD_TYPE).when(mController).getBuildType();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onPreferenceChange_switchEnabled_enablesDesktopModeOnSecondaryDisplay() {
        mController.onPreferenceChange(mPreference, /* newValue= */ true);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                DEVELOPMENT_FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS,
                /* def= */ SETTING_VALUE_INVALID);
        assertThat(mode).isEqualTo(SETTING_VALUE_ON);

        verify(mTransaction).add(any(RebootConfirmationDialogFragment.class), any());
    }

    @Test
    public void onPreferenceChange_switchEnabled_enablesFreeformSupport() {
        mController.onPreferenceChange(mPreference, /* newValue= */ true);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT, /* def= */ SETTING_VALUE_INVALID);
        assertThat(mode).isEqualTo(SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChange_switchDisabled_disablesDesktopModeOnSecondaryDisplay() {
        mController.onPreferenceChange(mPreference, /* newValue= */ false);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                DEVELOPMENT_FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS,
                /* def= */ SETTING_VALUE_INVALID);
        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
    }

    @Test
    public void onPreferenceChange_switchDisabled_disablesFreeformSupport() {
        mController.onPreferenceChange(mPreference, /* newValue= */ false);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT, /* def= */ SETTING_VALUE_INVALID);
        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
    }

    @Test
    public void updateState_settingEnabled_checksPreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                DEVELOPMENT_FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS, SETTING_VALUE_ON);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_unchecksPreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                DEVELOPMENT_FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS, SETTING_VALUE_OFF);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_disablesPreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                DEVELOPMENT_FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS,
                /* def= */ SETTING_VALUE_INVALID);
        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
        verify(mPreference).setEnabled(false);
    }
}
