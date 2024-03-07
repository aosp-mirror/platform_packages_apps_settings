/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.network;

import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;
import android.text.BidiFormatter;
import android.util.FeatureFlagUtils;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowRestrictedLockUtilsInternal.class,
        ShadowUtils.class,
        ShadowUserManager.class,
})
public class TopLevelNetworkEntryPreferenceControllerTest {

    @Mock
    private MobileNetworkPreferenceController mMobileNetworkPreferenceController;;

    private Context mContext;
    private TopLevelNetworkEntryPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final ShadowUserManager um = Shadow.extract(
                RuntimeEnvironment.application.getSystemService(UserManager.class));
        um.setIsAdminUser(true);

        mController = new TopLevelNetworkEntryPreferenceController(mContext, "test_key");

        ReflectionHelpers.setField(mController, "mMobileNetworkPreferenceController",
                mMobileNetworkPreferenceController);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void getAvailabilityStatus_demoUser_nonLargeScreen_unsupported() {
        ShadowUtils.setIsDemoUser(true);
        FeatureFlagUtils.setEnabled(mContext, "settings_support_large_screen", false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_hasMobile_shouldReturnMobileSummary() {
        when(mMobileNetworkPreferenceController.isAvailable()).thenReturn(true);

        assertThat(mController.getSummary()).isEqualTo(BidiFormatter.getInstance().unicodeWrap(
                mContext.getString(R.string.network_dashboard_summary_mobile)));
    }

    @Test
    public void getSummary_noMobile_shouldReturnNoMobileSummary() {
        when(mMobileNetworkPreferenceController.isAvailable()).thenReturn(false);

        assertThat(mController.getSummary()).isEqualTo(BidiFormatter.getInstance().unicodeWrap(
                mContext.getString(R.string.network_dashboard_summary_no_mobile)));
    }
}
