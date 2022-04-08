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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class EnterprisePrivacySettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private EnterprisePrivacySettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSettings = new EnterprisePrivacySettings();
    }

    @Test
    public void verifyConstants() {
        assertThat(mSettings.getMetricsCategory())
                .isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_SETTINGS);
        assertThat(mSettings.getLogTag()).isEqualTo("EnterprisePrivacySettings");
        assertThat(mSettings.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_ENTERPRISE_PRIVACY);
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
        final List<AbstractPreferenceController> controllers =
            mSettings.createPreferenceControllers(RuntimeEnvironment.application);
        verifyPreferenceControllers(controllers);
    }

    @Test
    public void getSearchIndexProviderPreferenceControllers() {
        final List<AbstractPreferenceController> controllers =
            EnterprisePrivacySettings.SEARCH_INDEX_DATA_PROVIDER
                .getPreferenceControllers(RuntimeEnvironment.application);
        verifyPreferenceControllers(controllers);
    }

    private void verifyPreferenceControllers(List<AbstractPreferenceController> controllers) {
        assertThat(controllers).isNotNull();
        assertThat(controllers.size()).isEqualTo(17);
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
        assertThat(controllers.get(position++)).isInstanceOf(ImePreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                GlobalHttpProxyPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                CaCertsCurrentUserPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                CaCertsManagedProfilePreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                PreferenceCategoryController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                FailedPasswordWipeCurrentUserPreferenceController.class);
        assertThat(controllers.get(position)).isInstanceOf(
                FailedPasswordWipeManagedProfilePreferenceController.class);
    }
}
