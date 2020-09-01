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

package com.android.settings.development;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.debug.IAdbManager;
import android.os.RemoteException;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class AdbQrCodePreferenceControllerTest {
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;
    @Mock
    private IAdbManager mAdbManager;
    @Mock
    private WirelessDebuggingFragment mFragment;

    private AdbQrCodePreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new AdbQrCodePreferenceController(mContext, "test_key"));
        mController.setParentFragment(mFragment);
        ReflectionHelpers.setField(mController, "mAdbManager", mAdbManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getContext()).thenReturn(mContext);
    }

    @Test
    public void getAvailabilityStatus_isAdbWifiQrSupported_yes_shouldBeTrue()
            throws RemoteException {
        when(mAdbManager.isAdbWifiQrSupported()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_isAdbWifiQrSupported_no_shouldBeFalse()
            throws RemoteException {
        when(mAdbManager.isAdbWifiQrSupported()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void displayPreference_isAdbWifiQrSupported_yes_prefIsVisible() throws RemoteException {
        when(mAdbManager.isAdbWifiQrSupported()).thenReturn(true);

        mController.displayPreference(mScreen);
        verify(mPreference).setVisible(true);
    }

    @Test
    public void displayPreference_isAdbWifiQrSupported_no_prefIsNotVisible()
            throws RemoteException {
        when(mAdbManager.isAdbWifiQrSupported()).thenReturn(false);

        mController.displayPreference(mScreen);
        verify(mPreference).setVisible(false);
    }

    @Test
    public void handlePreferenceTreeClick_launchActivity() {
        final String preferenceKey = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(preferenceKey);
        mController.handlePreferenceTreeClick(mPreference);

        verify(mFragment).startActivityForResult(any(Intent.class),
                eq(WirelessDebuggingFragment.PAIRING_DEVICE_REQUEST));
    }
}
