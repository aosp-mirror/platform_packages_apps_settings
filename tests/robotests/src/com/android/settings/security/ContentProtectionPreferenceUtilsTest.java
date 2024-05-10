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

package com.android.settings.security;

import static com.android.internal.R.string.config_defaultContentProtectionService;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.provider.DeviceConfig;
import android.view.contentcapture.ContentCaptureManager;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowDeviceConfig.class,
        })
public class ContentProtectionPreferenceUtilsTest {
    private static final String PACKAGE_NAME = "com.test.package";

    private static final ComponentName COMPONENT_NAME =
            new ComponentName(PACKAGE_NAME, "TestClass");

    private String mConfigDefaultContentProtectionService = COMPONENT_NAME.flattenToString();

    @Mock private Context mMockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    @Test
    public void isAvailable_bothEnabled_true() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);
        when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn(mConfigDefaultContentProtectionService);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isTrue();
    }

    @Test
    public void isAvailable_onlyUiEnabled_false() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void isAvailable_onlyServiceEnabled_false() {
        when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn(mConfigDefaultContentProtectionService);
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "false",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void isAvailable_emptyComponentName_false() {
        when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn("");
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void isAvailable_blankComponentName_false() {
	when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn("   ");
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void isAvailable_invalidComponentName_false() {
        when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn("invalid");

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }


    @Test
    public void isAvailable_bothDisabled_false() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "false",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }
}
