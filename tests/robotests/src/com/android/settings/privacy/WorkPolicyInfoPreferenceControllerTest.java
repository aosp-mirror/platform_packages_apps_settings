/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.privacy;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WorkPolicyInfoPreferenceControllerTest {

    private Context mContext;
    private FakeFeatureFactory mFakeFeatureFactory;
    private EnterprisePrivacyFeatureProvider mEnterpriseProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mEnterpriseProvider = mFakeFeatureFactory.getEnterprisePrivacyFeatureProvider();
        SafetyCenterManagerWrapper.sInstance = mock(SafetyCenterManagerWrapper.class);
    }

    @Test
    public void getAvailabilityStatus_noWorkPolicyInfo_shouldReturnUnsupported() {
        when(mEnterpriseProvider.hasWorkPolicyInfo()).thenReturn(false);
        WorkPolicyInfoPreferenceController controller =
                new WorkPolicyInfoPreferenceController(mContext, "test_key");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_haveWorkPolicyInfo_shouldReturnAvailable() {
        when(mEnterpriseProvider.hasWorkPolicyInfo()).thenReturn(true);
        WorkPolicyInfoPreferenceController controller =
                new WorkPolicyInfoPreferenceController(mContext, "test_key");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_safetyCenterEnabled_shouldReturnUnsupported() {
        when(SafetyCenterManagerWrapper.get().isEnabled(mContext)).thenReturn(true);
        WorkPolicyInfoPreferenceController controller =
                new WorkPolicyInfoPreferenceController(mContext, "test_key");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void handlePreferenceTreeClick_nonMatchingKey_shouldDoNothing() {
        when(mEnterpriseProvider.hasWorkPolicyInfo()).thenReturn(true);
        WorkPolicyInfoPreferenceController controller =
                new WorkPolicyInfoPreferenceController(mContext, "test_key");

        final Preference pref = new Preference(mContext);
        assertThat(controller.handlePreferenceTreeClick(pref)).isFalse();
        verify(mEnterpriseProvider, never()).showWorkPolicyInfo(mContext);
    }

    @Test
    public void handlePreferenceTreeClick_matchingKey_shouldShowWorkPolicyInfo() {
        when(mEnterpriseProvider.hasWorkPolicyInfo()).thenReturn(true);
        WorkPolicyInfoPreferenceController controller =
                new WorkPolicyInfoPreferenceController(mContext, "test_key");

        final Preference pref = new Preference(mContext);
        pref.setKey(controller.getPreferenceKey());
        assertThat(controller.handlePreferenceTreeClick(pref)).isTrue();
        verify(mEnterpriseProvider).showWorkPolicyInfo(mContext);
    }
}
