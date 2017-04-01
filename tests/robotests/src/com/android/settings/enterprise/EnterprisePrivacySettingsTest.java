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
        verifyPreferenceControllers(controllers);
    }

    @Test
    public void getSearchIndexProviderPreferenceControllers() {
        final List<PreferenceController> controllers
                = EnterprisePrivacySettings.SEARCH_INDEX_DATA_PROVIDER.getPreferenceControllers(
                        ShadowApplication.getInstance().getApplicationContext());
        verifyPreferenceControllers(controllers);
    }

    private void verifyPreferenceControllers(List<PreferenceController> controllers) {
        assertThat(controllers).isNotNull();
        assertThat(controllers.size()).isEqualTo(15);
        int position = 0;
        assertThat(controllers.get(position++)).isInstanceOf(NetworkLogsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(BugReportsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                SecurityLogsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                EnterpriseInstalledPackagesPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AdminGrantedLocationPermissionsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AdminGrantedMicrophonePermissionPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AdminGrantedCameraPermissionPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                EnterpriseSetDefaultAppsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AlwaysOnVpnCurrentUserPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AlwaysOnVpnManagedProfilePreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                GlobalHttpProxyPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                CaCertsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                FailedPasswordWipeCurrentUserPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                FailedPasswordWipeManagedProfilePreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(ImePreferenceController.class);
    }
}
