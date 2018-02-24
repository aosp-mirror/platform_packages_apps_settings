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
 * limitations under the License
 */
package com.android.settings.system;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.accounts.AccountManager;
import android.content.Context;
import android.os.UserManager;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = {ShadowSecureSettings.class, ShadowUtils.class}
)
public class FactoryResetPreferenceControllerTest {

    private static final String FACTORY_RESET_KEY = "factory_reset";

    @Mock
    private UserManager mUserManager;
    @Mock
    private AccountManager mAccountManager;

    private Context mContext;
    private FactoryResetPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        ShadowApplication.getInstance().setSystemService(Context.USER_SERVICE, mUserManager);
        ShadowApplication.getInstance().setSystemService(Context.ACCOUNT_SERVICE, mAccountManager);
        mController = new FactoryResetPreferenceController(mContext);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void isAvailable_systemUser() {
        when(mUserManager.isAdminUser()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_nonSystemUser() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        ShadowUtils.setIsDemoUser(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_demoUser() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        ShadowUtils.setIsDemoUser(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(FACTORY_RESET_KEY);
    }
}
