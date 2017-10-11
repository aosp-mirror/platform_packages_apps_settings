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
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
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
 * Tests for {@link ManageDeviceAdminPreferenceController}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class ManageDeviceAdminPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    private ManageDeviceAdminPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mController = new ManageDeviceAdminPreferenceController(mContext);
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);

        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile()).thenReturn(0);
        when(mContext.getResources().getString(R.string.number_of_device_admins_none))
                .thenReturn("no apps");
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("no apps");

        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile()).thenReturn(5);
        when(mContext.getResources().getQuantityString(R.plurals.number_of_device_admins, 5, 5))
                .thenReturn("5 active apps");
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("5 active apps");
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
        assertThat(mController.getPreferenceKey()).isEqualTo("manage_device_admin");
    }
}
