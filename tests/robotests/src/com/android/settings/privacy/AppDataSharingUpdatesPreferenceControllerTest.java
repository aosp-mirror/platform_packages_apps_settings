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

package com.android.settings.privacy;

import static android.content.pm.PackageManager.FEATURE_AUTOMOTIVE;
import static android.content.pm.PackageManager.FEATURE_LEANBACK;
import static android.content.pm.PackageManager.FEATURE_WATCH;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.safetylabel.SafetyLabelConstants;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class AppDataSharingUpdatesPreferenceControllerTest {

    public static final String PREFERENCE_KEY = "PREFERENCE_KEY";
    private static final List<String> sUnsupportedFormFactors =
            Arrays.asList(FEATURE_AUTOMOTIVE, FEATURE_LEANBACK, FEATURE_WATCH);
    private MockitoSession mMockitoSession;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Context mContext;
    private AppDataSharingUpdatesPreferenceController mController;

    @Before
    public void setUp() {
        mMockitoSession = Mockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.WARN)
                .startMocking();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mController = new AppDataSharingUpdatesPreferenceController(mContext, PREFERENCE_KEY);
        for (String formFactor : sUnsupportedFormFactors) {
            doReturn(false).when(mPackageManager).hasSystemFeature(eq(formFactor));
        }
    }

    @After
    public void tearDown() {
        mMockitoSession.finishMocking();
    }

    @Test
    public void whenSafetyLabelsDisabled_thenPreferenceUnavailable()
            throws Exception {
        setSafetyLabelsDeviceConfigEnabled(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void whenSafetyLabelsEnabled_andSupportedFormFactor_thenPreferenceAvailable()
            throws Exception {
        setSafetyLabelsDeviceConfigEnabled(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void whenSafetyLabelsEnabled_andUnsupportedFormFactor_thenPreferenceUnavailable()
            throws Exception {
        setSafetyLabelsDeviceConfigEnabled(true);
        for (String formFactor : sUnsupportedFormFactors) {
            doReturn(true).when(mPackageManager).hasSystemFeature(eq(formFactor));
            assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
            doReturn(false).when(mPackageManager).hasSystemFeature(eq(formFactor));
        }
    }

    private void setSafetyLabelsDeviceConfigEnabled(boolean newValue)
            throws Settings.SettingNotFoundException {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                SafetyLabelConstants.SAFETY_LABEL_CHANGE_NOTIFICATIONS_ENABLED,
                ((Boolean) newValue).toString(), /* makeDefault */ false);
    }
}
