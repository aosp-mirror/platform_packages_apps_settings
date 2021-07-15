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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserManager;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUserManager.class)
public class RestrictedEncryptionPreferenceControllerTest {

    private Context mContext;
    private ShadowUserManager mUserManager;
    private InstallCertificatePreferenceController mInstallCertificatePreferenceController;
    private ResetCredentialsPreferenceController mResetCredentialsPreferenceController;
    private UserCredentialsPreferenceController mUserCredentialsPreferenceController;
    private InstallCaCertificatePreferenceController mInstallCaCertificatePreferenceController;
    private InstallUserCertificatePreferenceController mInstallUserCertificatePreferenceController;
    private InstallWifiCertificatePreferenceController mInstallWifiCertificatePreferenceController;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mInstallCertificatePreferenceController =
                new InstallCertificatePreferenceController(mContext);
        mResetCredentialsPreferenceController =
                new ResetCredentialsPreferenceController(mContext, mLifecycle);
        mUserCredentialsPreferenceController =
                new UserCredentialsPreferenceController(mContext);
        mInstallCaCertificatePreferenceController =
                new InstallCaCertificatePreferenceController(mContext);
        mInstallUserCertificatePreferenceController =
                new InstallUserCertificatePreferenceController(mContext);
        mInstallWifiCertificatePreferenceController =
                new InstallWifiCertificatePreferenceController(mContext);
        mUserManager = ShadowUserManager.getShadow();
    }

    @Test
    public void isAvailable_noRestriction_shouldReturnTrue() {
        assertThat(mInstallCertificatePreferenceController.isAvailable()).isTrue();
        assertThat(mResetCredentialsPreferenceController.isAvailable()).isTrue();
        assertThat(mUserCredentialsPreferenceController.isAvailable()).isTrue();
        assertThat(mInstallCaCertificatePreferenceController.isAvailable()).isTrue();
        assertThat(mInstallUserCertificatePreferenceController.isAvailable()).isTrue();
        assertThat(mInstallWifiCertificatePreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_hasRestriction_shouldReturnFalse() {
        mUserManager.addBaseUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS);

        assertThat(mInstallCertificatePreferenceController.isAvailable()).isFalse();
        assertThat(mResetCredentialsPreferenceController.isAvailable()).isFalse();
        assertThat(mUserCredentialsPreferenceController.isAvailable()).isFalse();
    }
}