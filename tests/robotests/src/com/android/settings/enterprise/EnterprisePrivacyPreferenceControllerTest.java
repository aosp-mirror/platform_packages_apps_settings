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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EnterprisePrivacyPreferenceControllerTest {

    private static final String KEY_ENTERPRISE_PRIVACY = "enterprise_privacy";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PrivacyPreferenceControllerHelper mPrivacyPreferenceControllerHelper;
    private EnterprisePrivacyPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mock(DevicePolicyManager.class)).when(mContext)
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        mController = new EnterprisePrivacyPreferenceController(
                mContext, mPrivacyPreferenceControllerHelper, KEY_ENTERPRISE_PRIVACY);
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);

        mController.updateState(preference);

        verify(mPrivacyPreferenceControllerHelper).updateState(preference);
    }

    @Test
    public void testIsAvailable_noDeviceOwner_returnsFalse() {
        when(mPrivacyPreferenceControllerHelper.hasDeviceOwner()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_deviceOwner_financedDevice_returnsFalse() {
        when(mPrivacyPreferenceControllerHelper.hasDeviceOwner()).thenReturn(true);
        when(mPrivacyPreferenceControllerHelper.isFinancedDevice()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_deviceOwner_enterpriseDevice_returnsTrue() {
        when(mPrivacyPreferenceControllerHelper.hasDeviceOwner()).thenReturn(true);
        when(mPrivacyPreferenceControllerHelper.isFinancedDevice()).thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    @Test
    public void getPreferenceKey_byDefault_returnsDefaultValue() {
        assertThat(mController.getPreferenceKey()).isEqualTo(KEY_ENTERPRISE_PRIVACY);
    }

    @Test
    public void getPreferenceKey_whenGivenValue_returnsGivenValue() {
        mController = new EnterprisePrivacyPreferenceController(
                mContext, mPrivacyPreferenceControllerHelper, "key");

        assertThat(mController.getPreferenceKey()).isEqualTo("key");
    }
}
