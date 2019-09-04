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

import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.wifi.WifiMasterSwitchPreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowUserManager;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRestrictedLockUtilsInternal.class, ShadowUtils.class})
public class TopLevelNetworkEntryPreferenceControllerTest {

    @Mock
    private WifiMasterSwitchPreferenceController mWifiPreferenceController;
    @Mock
    private MobileNetworkPreferenceController mMobileNetworkPreferenceController;
    @Mock
    private TetherPreferenceController mTetherPreferenceController;

    private Context mContext;
    private TopLevelNetworkEntryPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final ShadowUserManager um = Shadows.shadowOf(
                RuntimeEnvironment.application.getSystemService(UserManager.class));
        um.setIsAdminUser(true);

        mController = new TopLevelNetworkEntryPreferenceController(mContext, "test_key");

        ReflectionHelpers.setField(mController, "mWifiPreferenceController",
                mWifiPreferenceController);
        ReflectionHelpers.setField(mController, "mMobileNetworkPreferenceController",
                mMobileNetworkPreferenceController);
        ReflectionHelpers.setField(mController, "mTetherPreferenceController",
                mTetherPreferenceController);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void getAvailabilityStatus_demoUser_unsupported() {
        ShadowUtils.setIsDemoUser(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_hasMobileAndHotspot_shouldReturnMobileSummary() {
        when(mWifiPreferenceController.isAvailable()).thenReturn(true);
        when(mMobileNetworkPreferenceController.isAvailable()).thenReturn(true);
        when(mTetherPreferenceController.isAvailable()).thenReturn(true);

        assertThat(mController.getSummary())
                .isEqualTo("Wi\u2011Fi, mobile, data usage, and hotspot");
    }

    @Test
    public void getSummary_noMobileOrHotspot_shouldReturnSimpleSummary() {
        when(mWifiPreferenceController.isAvailable()).thenReturn(true);
        when(mMobileNetworkPreferenceController.isAvailable()).thenReturn(false);
        when(mTetherPreferenceController.isAvailable()).thenReturn(false);

        assertThat(mController.getSummary()).isEqualTo("Wi\u2011Fi and data usage");
    }
}
