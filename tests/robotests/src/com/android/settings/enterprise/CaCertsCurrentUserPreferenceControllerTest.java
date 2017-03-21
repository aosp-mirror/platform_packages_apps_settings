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

import android.content.Context;
import android.content.res.Resources;
import com.android.settings.R;
import android.support.v7.preference.Preference;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CaCertsCurrentUserPreferenceController}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class CaCertsCurrentUserPreferenceControllerTest {

    private final String INSTALLED_CERTS_USER = "trusted credentials";
    private final String INSTALLED_CERTS_PERSONAL = "trusted credentials in personal profile";
    private final String NUMBER_INSTALLED_CERTS_1 = "1 cert";
    private final String NUMBER_INSTALLED_CERTS_10 = "10 certs";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    private CaCertsCurrentUserPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mController = new CaCertsCurrentUserPreferenceController(mContext);

        when(mContext.getString(R.string.enterprise_privacy_ca_certs_user))
                .thenReturn(INSTALLED_CERTS_USER);
        when(mContext.getString(R.string.enterprise_privacy_ca_certs_personal))
                .thenReturn(INSTALLED_CERTS_PERSONAL);
        when(mContext.getResources().getQuantityString(R.plurals.enterprise_privacy_number_ca_certs,
                1, 1)).thenReturn(NUMBER_INSTALLED_CERTS_1);
        when(mContext.getResources().getQuantityString(R.plurals.enterprise_privacy_number_ca_certs,
                10, 10)).thenReturn(NUMBER_INSTALLED_CERTS_10);
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(true);

        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isInCompMode()).thenReturn(false);

        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfOwnerInstalledCaCertsInCurrentUser()).thenReturn(0);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isFalse();

        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfOwnerInstalledCaCertsInCurrentUser()).thenReturn(1);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isTrue();
        assertThat(preference.getTitle()).isEqualTo(INSTALLED_CERTS_USER);
        assertThat(preference.getSummary()).isEqualTo(NUMBER_INSTALLED_CERTS_1);

        preference.setVisible(false);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfOwnerInstalledCaCertsInCurrentUser()).thenReturn(10);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isTrue();
        assertThat(preference.getTitle()).isEqualTo(INSTALLED_CERTS_USER);
        assertThat(preference.getSummary()).isEqualTo(NUMBER_INSTALLED_CERTS_10);

        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isInCompMode()).thenReturn(true);

        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfOwnerInstalledCaCertsInCurrentUser()).thenReturn(0);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isFalse();

        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfOwnerInstalledCaCertsInCurrentUser()).thenReturn(1);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isTrue();
        assertThat(preference.getTitle()).isEqualTo(INSTALLED_CERTS_PERSONAL);
        assertThat(preference.getSummary()).isEqualTo(NUMBER_INSTALLED_CERTS_1);

        preference.setVisible(false);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfOwnerInstalledCaCertsInCurrentUser()).thenReturn(10);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isTrue();
        assertThat(preference.getTitle()).isEqualTo(INSTALLED_CERTS_PERSONAL);
        assertThat(preference.getSummary()).isEqualTo(NUMBER_INSTALLED_CERTS_10);
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo("ca_certs_current_user");
    }
}
