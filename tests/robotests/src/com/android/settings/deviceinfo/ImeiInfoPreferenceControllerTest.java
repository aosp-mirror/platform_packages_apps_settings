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

package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.UserManager;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ImeiInfoPreferenceControllerTest {
    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock
    private ConnectivityManager mConnectivityManager;

    private ImeiInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(
                mConnectivityManager);
        mController = new ImeiInfoPreferenceController(mContext);
    }

    @Test
    public void testIsAvailable_isAdminAndHasMobile_shouldReturnTrue() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_isAdminButNoMobile_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE))
                .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_isNotAdmin_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }
}
