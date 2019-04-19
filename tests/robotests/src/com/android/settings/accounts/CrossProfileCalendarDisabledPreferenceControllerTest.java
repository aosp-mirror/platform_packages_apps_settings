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
package com.android.settings.accounts;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDevicePolicyManager;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class CrossProfileCalendarDisabledPreferenceControllerTest {

    private static final String PREF_KEY = "cross_profile_calendar_disabled";
    private static final int MANAGED_USER_ID = 10;
    private static final String TEST_PACKAGE_NAME = "com.test";
    private static final ComponentName TEST_COMPONENT_NAME = new ComponentName("test", "test");

    @Mock
    private UserHandle mManagedUser;

    private Context mContext;
    private CrossProfileCalendarDisabledPreferenceController mController;
    private ShadowDevicePolicyManager mDpm;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = new CrossProfileCalendarDisabledPreferenceController(mContext, PREF_KEY);
        mController.setManagedUser(mManagedUser);
        mDpm = Shadows.shadowOf(application.getSystemService(DevicePolicyManager.class));

        when(mManagedUser.getIdentifier()).thenReturn(MANAGED_USER_ID);
        doReturn(mContext).when(mContext).createPackageContextAsUser(
                any(String.class), anyInt(), any(UserHandle.class));
    }

    @Test
    public void getAvailabilityStatus_noPackageAllowed_shouldBeAvailable() {
        mDpm.setProfileOwner(TEST_COMPONENT_NAME);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_somePackagesAllowed_shouldBeDisabledForUser() {
        mDpm.setProfileOwner(TEST_COMPONENT_NAME);
        mDpm.setCrossProfileCalendarPackages(TEST_COMPONENT_NAME,
                Collections.singleton(TEST_PACKAGE_NAME));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_allPackagesAllowed_shouldBeDisabledForUser() {
        mDpm.setProfileOwner(TEST_COMPONENT_NAME);
        mDpm.setCrossProfileCalendarPackages(TEST_COMPONENT_NAME, null);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }
}