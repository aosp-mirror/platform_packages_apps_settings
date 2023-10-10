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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class TestingSettingsTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private TestingSettings mTestingSettings;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void isRadioInfoVisible_returnFalse_whenUserRestricted() {
        mockService(mContext, Context.USER_SERVICE, UserManager.class, mUserManager);

        doReturn(true).when(mUserManager).isAdminUser();
        doReturn(false).when(mUserManager).isGuestUser();
        doReturn(true).when(mUserManager)
                .hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);

        assertThat(mTestingSettings.isRadioInfoVisible(mContext)).isFalse();
    }

    private <T> void mockService(Context context, String serviceName,
            Class<T> serviceClass, T service) {
         when(context.getSystemServiceName(serviceClass)).thenReturn(serviceName);
         when(context.getSystemService(serviceName)).thenReturn(service);
     }
}
