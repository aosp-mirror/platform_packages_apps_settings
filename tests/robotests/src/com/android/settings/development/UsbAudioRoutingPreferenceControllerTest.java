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
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class UsbAudioRoutingPreferenceControllerTest {

    private static final ComponentName TEST_COMPONENT_NAME = new ComponentName("test", "test");

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private RestrictedSwitchPreference mPreference;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private Context mContext;

    private UsbAudioRoutingPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = new UsbAudioRoutingPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        when(mContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDevicePolicyManager);
        doReturn(mContext).when(mContext).createPackageContextAsUser(
                any(String.class), anyInt(), any(UserHandle.class));
    }

    @Test
    public void updateState_usbAudioRoutingEnabled_shouldCheckedPreference() {
        when(mDevicePolicyManager.isUsbDataSignalingEnabled()).thenReturn(true);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(TEST_COMPONENT_NAME);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED,
                UsbAudioRoutingPreferenceController.SETTING_VALUE_ON);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_usbAudioRoutingDisabled_shouldUncheckedPreference() {
        when(mDevicePolicyManager.isUsbDataSignalingEnabled()).thenReturn(true);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(TEST_COMPONENT_NAME);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED,
                UsbAudioRoutingPreferenceController.SETTING_VALUE_OFF);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
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
    public void onPreferenceChange_preferenceChecked_shouldEnableUsbAudioRouting() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final int usbAudioRoutingMode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED, -1 /* default */);

        assertThat(usbAudioRoutingMode).isEqualTo(
                UsbAudioRoutingPreferenceController.SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChange__preferenceUnchecked_shouldDisableUsbAudioRouting() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final int usbAudioRoutingMode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED, -1 /* default */);

        assertThat(usbAudioRoutingMode).isEqualTo(
                UsbAudioRoutingPreferenceController.SETTING_VALUE_OFF);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldBeEnabled() {
        mController.onDeveloperOptionsSwitchDisabled();

        final int usbAudioRoutingMode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED, -1 /* default */);

        assertThat(usbAudioRoutingMode).isEqualTo(
                UsbAudioRoutingPreferenceController.SETTING_VALUE_OFF);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
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
