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

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.PreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

/**
 * Tests for {@link EnterprisePrivacySettings}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class EnterprisePrivacySettingsTest {

    private EnterprisePrivacySettings mSettings;

    @Before
    public void setUp() {
        mSettings = new EnterprisePrivacySettings();
    }

    @Test
    public void testGetMetricsCategory() {
        assertThat(mSettings.getMetricsCategory())
                .isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_SETTINGS);
    }

    @Test
    public void testGetCategoryKey() {
        assertThat(mSettings.getCategoryKey()).isNull();
    }

    @Test
    public void testGetLogTag() {
        assertThat(mSettings.getLogTag()).isEqualTo("EnterprisePrivacySettings");
    }

    @Test
    public void testGetPreferenceScreenResId() {
        assertThat(mSettings.getPreferenceScreenResId())
                .isEqualTo(R.xml.enterprise_privacy_settings);
    }

    @Test
    public void getPreferenceControllers() {
        final List<PreferenceController> controllers = mSettings.getPreferenceControllers(
                ShadowApplication.getInstance().getApplicationContext());
        assertThat(controllers).isNotNull();
        assertThat(controllers.size()).isEqualTo(11);
        assertThat(controllers.get(0)).isInstanceOf(InstalledPackagesPreferenceController.class);
        assertThat(controllers.get(1)).isInstanceOf(NetworkLogsPreferenceController.class);
        assertThat(controllers.get(2)).isInstanceOf(BugReportsPreferenceController.class);
        assertThat(controllers.get(3)).isInstanceOf(SecurityLogsPreferenceController.class);
        assertThat(controllers.get(4)).isInstanceOf(
                EnterpriseInstalledPackagesPreferenceController.class);
        assertThat(controllers.get(5)).isInstanceOf(
                AdminGrantedLocationPermissionsPreferenceController.class);
        assertThat(controllers.get(6)).isInstanceOf(
                AdminGrantedMicrophonePermissionPreferenceController.class);
        assertThat(controllers.get(7)).isInstanceOf(
                AdminGrantedCameraPermissionPreferenceController.class);
        assertThat(controllers.get(8)).isInstanceOf(
                AlwaysOnVpnPrimaryUserPreferenceController.class);
        assertThat(controllers.get(9)).isInstanceOf(
                AlwaysOnVpnManagedProfilePreferenceController.class);
        assertThat(controllers.get(10)).isInstanceOf(GlobalHttpProxyPreferenceController.class);
    }
}
