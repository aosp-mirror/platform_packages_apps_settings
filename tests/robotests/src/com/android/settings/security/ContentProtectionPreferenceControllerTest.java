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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ComponentName;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.DeviceConfig;
import android.view.contentcapture.ContentCaptureManager;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowDeviceConfig.class,
        })
public class ContentProtectionPreferenceControllerTest {

    private static final String PACKAGE_NAME = "com.test.package";

    private static final ComponentName COMPONENT_NAME =
            new ComponentName(PACKAGE_NAME, "TestClass");

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;

    private String mConfigDefaultContentProtectionService = COMPONENT_NAME.flattenToString();

    private ContentProtectionPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mController = new ContentProtectionPreferenceController(mContext, "key");
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    @Test
    public void isAvailable_isFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_isTrue() {
        doReturn(COMPONENT_NAME.flattenToString())
                .when(mContext)
                .getString(config_defaultContentProtectionService);

        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);

        assertThat(mController.isAvailable()).isTrue();
    }
}
