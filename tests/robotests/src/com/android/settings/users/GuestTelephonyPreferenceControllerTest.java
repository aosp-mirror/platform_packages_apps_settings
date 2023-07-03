/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;
import android.os.UserManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowDevicePolicyManager.class
})
public class GuestTelephonyPreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;
    private ShadowUserManager mUserManager;
    private ShadowDevicePolicyManager mDpm;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mUserManager = ShadowUserManager.getShadow();
        mUserManager.setSupportsMultipleUsers(true);
        mDpm = ShadowDevicePolicyManager.getShadow();
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
    }

    @Test
    public void displayPref_NotAdmin_shouldNotDisplay() {
        mUserManager.setIsAdminUser(false);

        final GuestTelephonyPreferenceController controller =
                new GuestTelephonyPreferenceController(mContext, "fake_key");
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);

        when(preference.getKey()).thenReturn(controller.getPreferenceKey());
        when(mScreen.findPreference(preference.getKey())).thenReturn(preference);

        controller.displayPreference(mScreen);

        verify(preference).setVisible(false);
    }

    @Test
    public void updateState_NotAdmin_shouldNotDisplayPreference() {
        mUserManager.setIsAdminUser(false);

        final GuestTelephonyPreferenceController controller =
                new GuestTelephonyPreferenceController(mContext, "fake_key");
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);

        controller.updateState(preference);

        verify(preference).setVisible(false);
    }

    @Test
    public void updateState_Admin_shouldDisplayPreference() {
        SystemProperties.set("fw.max_users", Long.toBinaryString(4));
        mDpm.setDeviceOwner(null);
        mUserManager.setIsAdminUser(true);
        mUserManager.setUserSwitcherEnabled(true);
        mUserManager.setSupportsMultipleUsers(true);
        mUserManager.setUserTypeEnabled(UserManager.USER_TYPE_FULL_RESTRICTED, true);
        mUserManager.setUserTypeEnabled(UserManager.USER_TYPE_FULL_SYSTEM, true);
        mUserManager.setUserTypeEnabled(UserManager.USER_TYPE_FULL_GUEST, true);

        final GuestTelephonyPreferenceController controller =
                new GuestTelephonyPreferenceController(mContext, "fake_key");
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);

        controller.updateState(preference);

        verify(preference).setVisible(true);
    }

    @Test
    public void setChecked_Guest_hasNoCallRestriction() {
        mUserManager.setIsAdminUser(true);

        final GuestTelephonyPreferenceController controller =
                new GuestTelephonyPreferenceController(mContext, "fake_key");

        controller.setChecked(true);

        assertThat(mUserManager.hasGuestUserRestriction("no_outgoing_calls", false)).isTrue();
        assertThat(mUserManager.hasGuestUserRestriction("no_sms", true)).isTrue();
    }

    @Test
    public void setUnchecked_Guest_hasCallRestriction() {
        mUserManager.setIsAdminUser(true);

        final GuestTelephonyPreferenceController controller =
                new GuestTelephonyPreferenceController(mContext, "fake_key");

        controller.setChecked(false);

        assertThat(mUserManager.hasGuestUserRestriction("no_outgoing_calls", true)).isTrue();
        assertThat(mUserManager.hasGuestUserRestriction("no_sms", true)).isTrue();
    }

}
