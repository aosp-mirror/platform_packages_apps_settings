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

import android.content.Context;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.PreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnterprisePrivacySettings}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class EnterprisePrivacySettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private EnterprisePrivacySettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

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
    public void isPageEnabled_hasDeviceOwner_shouldReturnTrue() {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner())
                .thenReturn(true);

        assertThat(EnterprisePrivacySettings.isPageEnabled(mContext))
                .isTrue();
    }

    @Test
    public void isPageEnabled_noDeviceOwner_shouldReturnFalse() {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner())
                .thenReturn(false);

        assertThat(EnterprisePrivacySettings.isPageEnabled(mContext))
                .isFalse();
    }

    @Test
    public void getPreferenceControllers() {
        final List<PreferenceController> controllers = mSettings.getPreferenceControllers(
                ShadowApplication.getInstance().getApplicationContext());
        assertThat(controllers).isNotNull();
        assertThat(controllers.size()).isEqualTo(17);
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
                EnterpriseSetDefaultAppsPreferenceController.class);
        assertThat(controllers.get(9)).isInstanceOf(
                AlwaysOnVpnPrimaryUserPreferenceController.class);
        assertThat(controllers.get(10)).isInstanceOf(
                AlwaysOnVpnManagedProfilePreferenceController.class);
        assertThat(controllers.get(11)).isInstanceOf(GlobalHttpProxyPreferenceController.class);
        assertThat(controllers.get(12)).isInstanceOf(CaCertsCurrentUserPreferenceController.class);
        assertThat(controllers.get(13)).isInstanceOf(
                CaCertsManagedProfilePreferenceController.class);
        assertThat(controllers.get(14)).isInstanceOf(
                FailedPasswordWipePrimaryUserPreferenceController.class);
        assertThat(controllers.get(15)).isInstanceOf(
                FailedPasswordWipeManagedProfilePreferenceController.class);
        assertThat(controllers.get(16)).isInstanceOf(ImePreferenceController.class);
    }
}
