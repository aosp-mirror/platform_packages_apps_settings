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

package com.android.settings.privacy;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;

import androidx.preference.Preference;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class AccessibilityUsagePreferenceControllerTest {

    private Context mContext;
    private ShadowAccessibilityManager mAccessibilityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mAccessibilityManager = Shadow.extract(
                RuntimeEnvironment.application.getSystemService(AccessibilityManager.class));
    }

    @After
    public void tearDown() {
        ShadowAccessibilityManager.reset();
        ShadowDeviceConfig.reset();
    }

    @Test
    public void getAvailabilityStatus_noEnabledServices_shouldReturnUnsupported() {
        mAccessibilityManager.setEnabledAccessibilityServiceList(new ArrayList<>());
        final AccessibilityUsagePreferenceController controller =
                new AccessibilityUsagePreferenceController(mContext, "test_key");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_enabledServices_shouldReturnAvailable() {
        mAccessibilityManager.setEnabledAccessibilityServiceList(
                new ArrayList<>(Arrays.asList(new AccessibilityServiceInfo())));
        final AccessibilityUsagePreferenceController controller =
                new AccessibilityUsagePreferenceController(mContext, "test_key");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updateState_noEnabledServices_shouldHidePreference() {
        mAccessibilityManager.setEnabledAccessibilityServiceList(new ArrayList<>());
        final AccessibilityUsagePreferenceController controller =
                new AccessibilityUsagePreferenceController(mContext, "test_key");
        final Preference preference = new Preference(mContext);
        preference.setVisible(true);

        controller.updateState(preference);

        assertThat(preference.isVisible()).isFalse();
    }
}
