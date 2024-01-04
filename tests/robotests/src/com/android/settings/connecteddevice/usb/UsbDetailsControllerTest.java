/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.usb;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.usb.UsbPortStatus;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowKeyguardManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowKeyguardManager.class
})
public class UsbDetailsControllerTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private UsbBackend mUsbBackend;

    private Context mContext;
    private UsbDetailsController mUsbDetailsController;
    private UsbDetailsFragment mUsbDetailsFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = getApplicationContext();
        mUsbDetailsFragment = new UsbDetailsFragment();
        mUsbDetailsController = new UsbDetailsController(
                mContext, mUsbDetailsFragment, mUsbBackend) {
            @Override
            protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
            }

            @Override
            public String getPreferenceKey() {
                return null;
            }
        };
    }

    @Test
    public void isAvailable_returnsTrue() {
        assertThat(mUsbDetailsController.isAvailable()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_AUTH_CHALLENGE_FOR_USB_PREFERENCES)
    public void requireAuthAndExecute_whenAlreadyAuthenticated_executes() {
        mUsbDetailsFragment.setUserAuthenticated(true);
        Runnable action = () -> mUsbBackend.setDataRole(UsbPortStatus.DATA_ROLE_HOST);

        mUsbDetailsController.requireAuthAndExecute(action);

        verify(mUsbBackend).setDataRole(anyInt());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_AUTH_CHALLENGE_FOR_USB_PREFERENCES)
    public void requireAuthAndExecute_authenticatesAndExecutes() {
        mUsbDetailsFragment.setUserAuthenticated(false);
        setAuthPassesAutomatically();
        Runnable action = () -> mUsbBackend.setDataRole(UsbPortStatus.DATA_ROLE_HOST);

        mUsbDetailsController.requireAuthAndExecute(action);

        assertThat(mUsbDetailsFragment.isUserAuthenticated()).isTrue();
        verify(mUsbBackend).setDataRole(anyInt());
    }

    private void setAuthPassesAutomatically() {
        Shadows.shadowOf(mContext.getSystemService(KeyguardManager.class))
                .setIsKeyguardSecure(false);
    }
}
