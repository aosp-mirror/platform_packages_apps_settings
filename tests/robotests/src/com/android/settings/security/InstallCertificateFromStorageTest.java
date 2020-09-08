/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.security.InstallCertificateFromStorage.SEARCH_INDEX_DATA_PROVIDER;

import static com.google.common.truth.Truth.assertThat;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class InstallCertificateFromStorageTest {

    @Mock
    private UserManager mUserManager;

    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private Context mContext;

    private List<String> mTestKeys;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication application = ShadowApplication.getInstance();
        application.setSystemService(Context.DEVICE_POLICY_SERVICE, mDevicePolicyManager);
        application.setSystemService(Context.USER_SERVICE, mUserManager);
        mContext = RuntimeEnvironment.application;
        setUpTestKeys();
    }

    private void setUpTestKeys() {
        mTestKeys = new ArrayList<>();
        mTestKeys.add("certificate_types");
        mTestKeys.add("install_ca_certificate");
        mTestKeys.add("install_user_certificate");
        mTestKeys.add("install_wifi_certificate");
    }

    @Test
    public void getMetricsCategory_shouldReturnInstallCertificateFromStorage() {
        InstallCertificateFromStorage fragment = new InstallCertificateFromStorage();
        assertThat(fragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.INSTALL_CERTIFICATE_FROM_STORAGE);
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> nonIndexableKeys =
                SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(nonIndexableKeys).containsAllIn(mTestKeys);
    }

}
