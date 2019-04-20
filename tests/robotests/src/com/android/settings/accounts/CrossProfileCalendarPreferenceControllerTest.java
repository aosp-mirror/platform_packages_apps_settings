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

import static android.provider.Settings.Secure.CROSS_PROFILE_CALENDAR_ENABLED;

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
import android.provider.Settings;
import android.util.ArraySet;

import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDevicePolicyManager;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class CrossProfileCalendarPreferenceControllerTest {

    private static final String PREF_KEY = "cross_profile_calendar";
    private static final int MANAGED_USER_ID = 10;
    private static final String TEST_PACKAGE_NAME = "com.test";
    private static final ComponentName TEST_COMPONENT_NAME = new ComponentName("test", "test");

    @Mock
    private UserHandle mManagedUser;

    private RestrictedSwitchPreference mPreference;
    private Context mContext;
    private CrossProfileCalendarPreferenceController mController;
    private ShadowDevicePolicyManager dpm;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = new CrossProfileCalendarPreferenceController(mContext, PREF_KEY);
        mController.setManagedUser(mManagedUser);
        mPreference = spy(new RestrictedSwitchPreference(mContext));
        dpm = Shadows.shadowOf(application.getSystemService(DevicePolicyManager.class));

        when(mManagedUser.getIdentifier()).thenReturn(MANAGED_USER_ID);
        doReturn(mContext).when(mContext).createPackageContextAsUser(
                any(String.class), anyInt(), any(UserHandle.class));
    }

    @Test
    public void getAvailabilityStatus_noManagedUser_shouldBeDisabled() {
        mController.setManagedUser(null);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(CrossProfileCalendarPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noPackageAllowed_shouldBeDisabledForUser() throws Exception {
        dpm.setProfileOwner(TEST_COMPONENT_NAME);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_somePackagesAllowed_shouldBeAvailable() throws Exception {
        dpm.setProfileOwner(TEST_COMPONENT_NAME);
        dpm.setCrossProfileCalendarPackages(TEST_COMPONENT_NAME,
                Collections.singleton(TEST_PACKAGE_NAME));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_allPackagesAllowed_shouldBeAvailable() throws Exception {
        dpm.setProfileOwner(TEST_COMPONENT_NAME);
        dpm.setCrossProfileCalendarPackages(TEST_COMPONENT_NAME, null);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updateStateToDisabled_isNotChecked() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                CROSS_PROFILE_CALENDAR_ENABLED, 0, mManagedUser.getIdentifier());

        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateStateToEnabled_isChecked() throws Exception {
        // Put 0 first so we know the value is not originally 1.
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                CROSS_PROFILE_CALENDAR_ENABLED, 0, mManagedUser.getIdentifier());
        mController.updateState(mPreference);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                CROSS_PROFILE_CALENDAR_ENABLED, 1, mManagedUser.getIdentifier());

        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChangeToFalse_shouldUpdateProviderValue() {
        mController.onPreferenceChange(mPreference, false);
        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                CROSS_PROFILE_CALENDAR_ENABLED, 1, mManagedUser.getIdentifier()))
                .isEqualTo(0);
    }

    @Test
    public void onPreferenceChangeToTrue_shouldUpdateProviderValue() {
        // Change to false first so we know the value is not originally 1.
        mController.onPreferenceChange(mPreference, false);

        mController.onPreferenceChange(mPreference, true);
        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                CROSS_PROFILE_CALENDAR_ENABLED, 0, mManagedUser.getIdentifier()))
                .isEqualTo(1);
    }
}