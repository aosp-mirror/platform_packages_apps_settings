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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PrivacyPreferenceControllerHelperTest {

    private static final String MANAGED_GENERIC = "managed by organization";
    private static final String MANAGED_WITH_NAME = "managed by Foo, Inc.";
    private static final String MANAGING_ORGANIZATION = "Foo, Inc.";
    private static final ComponentName DEVICE_OWNER_COMPONENT =
            new ComponentName("com.android.foo", "bar");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    private FakeFeatureFactory mFeatureFactory;
    private PrivacyPreferenceControllerHelper mPrivacyPreferenceControllerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        when((Object) mContext.getSystemService(DevicePolicyManager.class))
                .thenReturn(mDevicePolicyManager);
        mPrivacyPreferenceControllerHelper = new PrivacyPreferenceControllerHelper(mContext);

        when(mDevicePolicyManager.isDeviceManaged()).thenReturn(true);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser())
                .thenReturn(DEVICE_OWNER_COMPONENT);
        when(mDevicePolicyManager.getDeviceOwnerType(DEVICE_OWNER_COMPONENT))
                .thenReturn(DEVICE_OWNER_TYPE_DEFAULT);
    }

    @Test
    @Ignore
    public void testUpdateState_noDeviceOwnerName_useGenericPreferenceSummary() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        when(mContext.getString(R.string.enterprise_privacy_settings_summary_generic))
                .thenReturn(MANAGED_GENERIC);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.getDeviceOwnerOrganizationName())
                .thenReturn(null);

        mPrivacyPreferenceControllerHelper.updateState(preference);

        assertThat(preference.getSummary().toString()).isEqualTo(MANAGED_GENERIC);
    }

    @Test
    @Ignore
    public void testUpdateState_deviceOwnerName_usePreferenceSummaryWithDeviceOwnerName() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        when(mContext.getResources().getString(
                R.string.enterprise_privacy_settings_summary_with_name, MANAGING_ORGANIZATION))
                .thenReturn(MANAGED_WITH_NAME);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.getDeviceOwnerOrganizationName())
                .thenReturn(MANAGING_ORGANIZATION);

        mPrivacyPreferenceControllerHelper.updateState(preference);

        assertThat(preference.getSummary().toString()).isEqualTo(MANAGED_WITH_NAME);
    }

    @Test
    public void hasDeviceOwner_deviceOwner_returnsTrue() {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner()).thenReturn(true);

        assertThat(mPrivacyPreferenceControllerHelper.hasDeviceOwner()).isTrue();
    }

    @Test
    public void hasDeviceOwner_noDeviceOwner_returnsFalse() {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner()).thenReturn(false);

        assertThat(mPrivacyPreferenceControllerHelper.hasDeviceOwner()).isFalse();
    }

    @Test
    public void isFinancedDevice_deviceNotManaged_returnsFalse() {
        when(mDevicePolicyManager.isDeviceManaged()).thenReturn(false);

        assertThat(mPrivacyPreferenceControllerHelper.isFinancedDevice()).isFalse();
    }

    @Test
    public void isFinancedDevice_deviceManaged_defaultDeviceOwnerType_returnsFalse() {
        assertThat(mPrivacyPreferenceControllerHelper.isFinancedDevice()).isFalse();
    }

    @Test
    public void isFinancedDevice_deviceManaged_financedDeviceOwnerType_returnsTrue() {
        when(mDevicePolicyManager.getDeviceOwnerType(DEVICE_OWNER_COMPONENT))
                .thenReturn(DEVICE_OWNER_TYPE_FINANCED);

        assertThat(mPrivacyPreferenceControllerHelper.isFinancedDevice()).isTrue();
    }
}
