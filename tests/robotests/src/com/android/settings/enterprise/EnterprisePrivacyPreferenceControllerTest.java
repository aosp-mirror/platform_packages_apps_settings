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

import static android.app.admin.DevicePolicyManager.DEVICE_OWNER_TYPE_DEFAULT;
import static android.app.admin.DevicePolicyManager.DEVICE_OWNER_TYPE_FINANCED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EnterprisePrivacyPreferenceControllerTest {

    private static final String MANAGED_GENERIC = "managed by organization";
    private static final String MANAGED_WITH_NAME = "managed by Foo, Inc.";
    private static final String MANAGING_ORGANIZATION = "Foo, Inc.";
    private static final String KEY_ENTERPRISE_PRIVACY = "enterprise_privacy";
    private static final String FINANCED_PREFERENCE_TITLE = "Financed device info";
    private static final ComponentName DEVICE_OWNER_COMPONENT =
            new ComponentName("com.android.foo", "bar");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    private FakeFeatureFactory mFeatureFactory;

    private EnterprisePrivacyPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new EnterprisePrivacyPreferenceController(mContext);

        when((Object) mContext.getSystemService(DevicePolicyManager.class))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.isDeviceManaged()).thenReturn(true);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser())
                .thenReturn(DEVICE_OWNER_COMPONENT);
        when(mDevicePolicyManager.getDeviceOwnerType(DEVICE_OWNER_COMPONENT))
                .thenReturn(DEVICE_OWNER_TYPE_DEFAULT);
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);

        when(mContext.getString(R.string.enterprise_privacy_settings_summary_generic))
                .thenReturn(MANAGED_GENERIC);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.getDeviceOwnerOrganizationName())
                .thenReturn(null);
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo(MANAGED_GENERIC);

        when(mContext.getResources().getString(
                R.string.enterprise_privacy_settings_summary_with_name, MANAGING_ORGANIZATION))
                .thenReturn(MANAGED_WITH_NAME);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.getDeviceOwnerOrganizationName())
                .thenReturn(MANAGING_ORGANIZATION);
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo(MANAGED_WITH_NAME);
    }

    @Test
    public void testUpdateState_verifyPreferenceTitleIsUpdatedForFinancedDevice() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        when(mContext.getResources().getString(
                R.string.enterprise_privacy_settings_summary_with_name, MANAGING_ORGANIZATION))
                .thenReturn(MANAGED_WITH_NAME);
        when(mContext.getString(R.string.financed_privacy_settings))
                .thenReturn(FINANCED_PREFERENCE_TITLE);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.getDeviceOwnerOrganizationName())
                .thenReturn(MANAGING_ORGANIZATION);
        when(mDevicePolicyManager.getDeviceOwnerType(DEVICE_OWNER_COMPONENT))
                .thenReturn(DEVICE_OWNER_TYPE_FINANCED);

        mController.updateState(preference);

        assertThat(preference.getTitle()).isEqualTo(FINANCED_PREFERENCE_TITLE);
        assertThat(preference.getSummary()).isEqualTo(MANAGED_WITH_NAME);
    }

    @Test
    public void testIsAvailable() {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();

        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner()).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(KEY_ENTERPRISE_PRIVACY);
    }
}
