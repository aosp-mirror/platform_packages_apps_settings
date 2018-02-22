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

package com.android.settings.connecteddevice.usb;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.wrapper.UserManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UsbBackendTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private UsbManager mUsbManager;
    @Mock
    private UserManagerWrapper mUserManagerWrapper;
    @Mock
    private ConnectivityManager mConnectivityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI))
            .thenReturn(true);
        when((Object)mContext.getSystemService(UsbManager.class)).thenReturn(mUsbManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn((Object) mConnectivityManager);
    }

    @Test
    public void constructor_noUsbPort_shouldNotCrash() {
        UsbBackend usbBackend = new UsbBackend(mContext, mUserManagerWrapper, null);
        // Should not crash
    }
}
