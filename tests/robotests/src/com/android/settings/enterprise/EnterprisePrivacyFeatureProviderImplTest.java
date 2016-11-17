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

package com.android.settings.enterprise;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.PackageManagerWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnterprisePrivacyFeatureProviderImpl}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class EnterprisePrivacyFeatureProviderImplTest {

    private final ComponentName DEVICE_OWNER = new ComponentName("dummy", "component");

    private @Mock DevicePolicyManagerWrapper mDevicePolicyManager;
    private @Mock PackageManagerWrapper mPackageManager;

    private EnterprisePrivacyFeatureProvider mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN))
                .thenReturn(true);

        mProvider = new EnterprisePrivacyFeatureProviderImpl(mDevicePolicyManager, mPackageManager);
    }

    @Test
    public void testHasDeviceOwner() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        assertThat(mProvider.hasDeviceOwner()).isFalse();

        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(DEVICE_OWNER);
        assertThat(mProvider.hasDeviceOwner()).isTrue();
    }
}
