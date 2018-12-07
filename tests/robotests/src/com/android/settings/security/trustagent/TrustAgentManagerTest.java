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
 * limitations under the License.
 */

package com.android.settings.security.trustagent;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TrustAgentManagerTest {

    private static final String CANNED_PACKAGE_NAME = "com.test.package";

    @Mock
    private PackageManager mPackageManager;

    private TrustAgentManager mTrustAgentManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mTrustAgentManager = new TrustAgentManager();
    }

    @Test
    public void shouldProvideTrust_doesProvideTrustWithPermission() {
        when(mPackageManager.checkPermission(TrustAgentManager.PERMISSION_PROVIDE_AGENT,
                CANNED_PACKAGE_NAME)).thenReturn(PackageManager.PERMISSION_GRANTED);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.packageName = CANNED_PACKAGE_NAME;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        assertThat(mTrustAgentManager.shouldProvideTrust(resolveInfo, mPackageManager)).isTrue();
    }

    @Test
    public void shouldProvideTrust_doesNotProvideTrustWithoutPermission() {
        when(mPackageManager.checkPermission(TrustAgentManager.PERMISSION_PROVIDE_AGENT,
                CANNED_PACKAGE_NAME)).thenReturn(PackageManager.PERMISSION_DENIED);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.packageName = CANNED_PACKAGE_NAME;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        assertThat(mTrustAgentManager.shouldProvideTrust(resolveInfo, mPackageManager)).isFalse();
    }
}
