/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class MobileNetworkPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock
    private ConnectivityManager mConnectivityManager;

    private MobileNetworkPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
    }

    @Test
    public void secondaryUser_prefIsNotAvailable() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        when(mUserManager.hasUserRestriction(anyString(), any(UserHandle.class)))
                .thenReturn(false);
        when(mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE))
                .thenReturn(true);

        mController = new MobileNetworkPreferenceController(mContext);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void wifiOnly_prefIsNotAvailable() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.hasUserRestriction(anyString(), any(UserHandle.class)))
                .thenReturn(false);
        when(mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE))
                .thenReturn(false);

        mController = new MobileNetworkPreferenceController(mContext);
        assertThat(mController.isAvailable()).isFalse();
    }
}
