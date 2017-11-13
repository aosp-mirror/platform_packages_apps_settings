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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link CaCertsCurrentUserPreferenceController}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class CaCertsCurrentUserPreferenceControllerTest extends
        CaCertsPreferenceControllerTestBase {

    private static final String CA_CERT_DEVICE = "CA certs";
    private static final String CA_CERT_PERSONAL = "CA certs in personal profile";

    @Before
    public void mockGetString() {
        when(mContext.getString(R.string.enterprise_privacy_ca_certs_device))
                .thenReturn(CA_CERT_DEVICE);
        when(mContext.getString(R.string.enterprise_privacy_ca_certs_personal))
                .thenReturn(CA_CERT_PERSONAL);
    }

    @Test
    public void testUpdateState_nonCompMode() {
        assertUpdateState(false /* isCompMode */, CA_CERT_DEVICE);
    }

    @Test
    public void testUpdateState_compMode() {
        assertUpdateState(true /* isCompMode */, CA_CERT_PERSONAL);
    }

    @Override
    void mockGetNumberOfCaCerts(int numOfCaCerts) {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfOwnerInstalledCaCertsForCurrentUser()).thenReturn(numOfCaCerts);
    }

    @Override
    String getPreferenceKey() {
        return CaCertsCurrentUserPreferenceController.CA_CERTS_CURRENT_USER;
    }

    @Override
    CaCertsPreferenceControllerBase createController() {
        return new CaCertsCurrentUserPreferenceController(mContext, null /* lifecycle */);
    }

    private void assertUpdateState(boolean isCompMode, String expectedTitle) {
        final Preference preference = new Preference(mContext, null, 0, 0);

        mockGetNumberOfCaCerts(2);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isInCompMode())
                .thenReturn(isCompMode);
        mController.updateState(preference);
        assertThat(preference.getTitle()).isEqualTo(expectedTitle);
    }
}
