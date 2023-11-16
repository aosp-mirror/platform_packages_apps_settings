/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.settings.development.graphicsdriver;

import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController.ANGLE_DRIVER_SUFFIX;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController.PROPERTY_DEBUG_ANGLE_DEVELOPER_OPTION;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController.PROPERTY_PERSISTENT_GRAPHICS_EGL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.development.DevelopmentSettingsDashboardFragment;
import com.android.settings.development.RebootConfirmationDialogFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemProperties;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class GraphicsDriverEnableAngleAsSystemDriverControllerTest {
    private static final String TAG = "GraphicsDriverEnableAngleAsSystemDriverControllerTest";
    @Mock private PreferenceScreen mScreen;
    @Mock private SwitchPreference mPreference;
    @Mock private DevelopmentSettingsDashboardFragment mFragment;
    @Mock private FragmentActivity mActivity;
    @Mock private FragmentManager mFragmentManager;
    @Mock private FragmentTransaction mTransaction;

    private GraphicsDriverEnableAngleAsSystemDriverController mController;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        ShadowSystemProperties.override(PROPERTY_DEBUG_ANGLE_DEVELOPER_OPTION, "true");
        doReturn(mTransaction).when(mFragmentManager).beginTransaction();
        doReturn(mFragmentManager).when(mActivity).getSupportFragmentManager();
        doReturn(mActivity).when(mFragment).getActivity();
        mController = new GraphicsDriverEnableAngleAsSystemDriverController(mContext, mFragment);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_switchOn_shouldEnableAngleAsSystemDriver() {
        // since GraphicsEnvironment is mocked in Robolectric test environment,
        // we will override the system property persist.graphics.egl as if it is changed by
        // mGraphicsEnvironment.toggleAngleAsSystemDriver(true).

        // for test that actually verifies mGraphicsEnvironment.toggleAngleAsSystemDriver(true)
        // on a device/emulator, please refer to
        // GraphicsDriverEnableAngleAsSystemDriverControllerJUnitTest
        ShadowSystemProperties.override(PROPERTY_PERSISTENT_GRAPHICS_EGL, ANGLE_DRIVER_SUFFIX);
        mController.onPreferenceChange(mPreference, true);
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo(ANGLE_DRIVER_SUFFIX); // empty
        verify(mTransaction).add(any(RebootConfirmationDialogFragment.class), any());
    }

    @Test
    public void onPreferenceChange_switchOff_shouldDisableAngleAsSystemDriver() {
        // since GraphicsEnvironment is mocked in Robolectric test environment,
        // we will override the system property persist.graphics.egl as if it is changed by
        // mGraphicsEnvironment.toggleAngleAsSystemDriver(false).

        // for test that actually verifies mGraphicsEnvironment.toggleAngleAsSystemDriver(true)
        // on a device/emulator, please refer to
        // GraphicsDriverEnableAngleAsSystemDriverControllerJUnitTest
        ShadowSystemProperties.override(PROPERTY_PERSISTENT_GRAPHICS_EGL, "");
        mController.onPreferenceChange(mPreference, false);
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");
        verify(mTransaction).add(any(RebootConfirmationDialogFragment.class), any());
    }

    @Test
    public void updateState_angleUsed_preferenceShouldBeChecked() {
        ShadowSystemProperties.override(PROPERTY_PERSISTENT_GRAPHICS_EGL, ANGLE_DRIVER_SUFFIX);
        mController.updateState(mPreference);
        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_angleNotUsed_preferenceShouldNotBeChecked() {
        ShadowSystemProperties.override(PROPERTY_PERSISTENT_GRAPHICS_EGL, "");
        mController.updateState(mPreference);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionSwitchDisabled_shouldDisableAngleAsSystemDriver() {
        mController.onDeveloperOptionsSwitchDisabled();
        final String systemEGLDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        assertThat(systemEGLDriver).isEqualTo("");
    }

    @Test
    public void onDeveloperOptionSwitchDisabled_preferenceShouldNotBeChecked() {
        mController.onDeveloperOptionsSwitchDisabled();
        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldNotBeEnabled() {
        mController.onDeveloperOptionsSwitchDisabled();
        verify(mPreference).setEnabled(false);
    }
}
