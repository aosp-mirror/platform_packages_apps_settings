/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

public abstract class AbsBasePrivacySettingsPreference {

    protected void verifyEnterpriseSearchIndexableResources(
            List<SearchIndexableResource> searchIndexableResources) {
        assertThat(searchIndexableResources).isNotEmpty();
        assertThat(searchIndexableResources.size()).isEqualTo(1);
        assertThat(searchIndexableResources.get(0).xmlResId)
                .isEqualTo(R.xml.enterprise_privacy_settings);
    }

    protected void verifyEnterprisePreferenceControllers(
            List<AbstractPreferenceController> controllers) {
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

    protected void verifyFinancedSearchIndexableResources(
            List<SearchIndexableResource> searchIndexableResources) {
        assertThat(searchIndexableResources).isNotEmpty();
        assertThat(searchIndexableResources.size()).isEqualTo(1);
        assertThat(searchIndexableResources.get(0).xmlResId)
                .isEqualTo(R.xml.financed_privacy_settings);
    }

    protected void verifyFinancedPreferenceControllers(
            List<AbstractPreferenceController> controllers) {
        assertThat(controllers).isEmpty();
    }
}
