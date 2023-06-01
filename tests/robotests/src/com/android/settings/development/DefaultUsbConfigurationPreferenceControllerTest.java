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

package com.android.settings.development;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DefaultUsbConfigurationPreferenceControllerTest {

    private static final ComponentName TEST_COMPONENT_NAME = new ComponentName("test", "test");

    @Mock
    private RestrictedPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private DefaultUsbConfigurationPreferenceController mController;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        Context context = spy(RuntimeEnvironment.application);
        mController = new DefaultUsbConfigurationPreferenceController(context);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        when(context.getSystemService(DevicePolicyManager.class)).thenReturn(mDevicePolicyManager);
        doReturn(context).when(context).createPackageContextAsUser(
                any(String.class), anyInt(), any(UserHandle.class));
    }

    @Test
    public void updateState_usbDataSignalingEnabled_shouldNotDisablePreference() {
        when(mDevicePolicyManager.isUsbDataSignalingEnabled()).thenReturn(true);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(TEST_COMPONENT_NAME);

        mController.updateState(mPreference);

        verify(mPreference).setDisabledByAdmin(null);
    }

    @Test
    public void updateState_usbDataSignalingDisabled_shouldDisablePreference() {
        when(mDevicePolicyManager.isUsbDataSignalingEnabled()).thenReturn(false);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(TEST_COMPONENT_NAME);

        mController.updateState(mPreference);

        verify(mPreference).setDisabledByAdmin(eq(new RestrictedLockUtils.EnforcedAdmin(
                TEST_COMPONENT_NAME, null, UserHandle.SYSTEM)));
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_usbEnabled_shouldNotDisablePreference() {
        when(mDevicePolicyManager.isUsbDataSignalingEnabled()).thenReturn(true);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(TEST_COMPONENT_NAME);

        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference).setDisabledByAdmin(null);
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_usbDisabled_shouldDisablePreference() {
        when(mDevicePolicyManager.isUsbDataSignalingEnabled()).thenReturn(false);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(TEST_COMPONENT_NAME);

        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference).setDisabledByAdmin(eq(new RestrictedLockUtils.EnforcedAdmin(
                TEST_COMPONENT_NAME, null, UserHandle.SYSTEM)));
    }
}
