/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.mockito.Mockito.when;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link CaCertsManagedProfilePreferenceController}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class CaCertsManagedProfilePreferenceControllerTest extends
        CaCertsPreferenceControllerTestBase {

    @Override
    void mockGetNumberOfCaCerts(int numOfCaCerts) {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfOwnerInstalledCaCertsForManagedProfile()).thenReturn(numOfCaCerts);
    }

    @Override
    String getPreferenceKey() {
        return CaCertsManagedProfilePreferenceController.CA_CERTS_MANAGED_PROFILE;
    }

    @Override
    CaCertsPreferenceControllerBase createController() {
        return new CaCertsManagedProfilePreferenceController(mContext, null /* lifecycle */);
    }
}
