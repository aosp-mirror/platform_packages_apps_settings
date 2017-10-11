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

package com.android.settings.trustagent;

import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class TrustAgentFeatureProviderImplTest {

    private static final String CANNED_PACKAGE_NAME = "com.test.package";

    @Mock
    private PackageManager mPackageManager;

    private TrustAgentManagerImpl mImpl;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mImpl = new TrustAgentManagerImpl();
    }

    @Test
    public void shouldProvideTrust_doesProvideTrustWithPermission() {
        when(mPackageManager.checkPermission(TrustAgentManager.PERMISSION_PROVIDE_AGENT,
            CANNED_PACKAGE_NAME)).thenReturn(PackageManager.PERMISSION_GRANTED);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.packageName = CANNED_PACKAGE_NAME;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        assertTrue(mImpl.shouldProvideTrust(resolveInfo, mPackageManager));
    }

    @Test
    public void shouldProvideTrust_doesNotProvideTrustWithoutPermission() {
        when(mPackageManager.checkPermission(TrustAgentManager.PERMISSION_PROVIDE_AGENT,
            CANNED_PACKAGE_NAME)).thenReturn(PackageManager.PERMISSION_DENIED);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.packageName = CANNED_PACKAGE_NAME;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        assertFalse(mImpl.shouldProvideTrust(resolveInfo, mPackageManager));
    }
}
