/*
 * Copyright (C) 2016 The Android Open Source Project
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings.Global;

import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.BasePreferenceController;
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
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class AddUserWhenLockedPreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock(answer = RETURNS_DEEP_STUBS)

    private Context mContext;
    private ShadowUserManager mUserManager;
    private AddUserWhenLockedPreferenceController mController;
    private LockPatternUtils mLockPatternUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLockPatternUtils = mock(LockPatternUtils.class);
        mUserManager = ShadowUserManager.getShadow();
        mController = new AddUserWhenLockedPreferenceController(mContext, "fake_key");
        ReflectionHelpers.setField(mController, "mLockPatternUtils", mLockPatternUtils);
        mUserManager.setSupportsMultipleUsers(true);
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
    }

    @Test
    public void displayPref_NotAdmin_shouldNotDisplay() {
        mUserManager.setIsAdminUser(false);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());
        when(mScreen.findPreference(preference.getKey())).thenReturn(preference);

        mController.displayPreference(mScreen);

        verify(preference).setVisible(false);
    }

    @Test
    public void updateState_NotAdmin_shouldNotDisplayPreference() {
        mUserManager.setIsAdminUser(false);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);

        mController.updateState(preference);

        verify(preference).setVisible(false);
    }

    @Test
    public void updateState_Admin_shouldDisplayPreference() {
        mUserManager.setIsAdminUser(true);
        mUserManager.setUserSwitcherEnabled(true);
        mUserManager.setSupportsMultipleUsers(true);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        final AddUserWhenLockedPreferenceController controller =
                new AddUserWhenLockedPreferenceController(mContext, "fake_key");
        ReflectionHelpers.setField(controller, "mLockPatternUtils", mLockPatternUtils);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);

        controller.updateState(preference);

        verify(preference).setVisible(true);
    }

    @Test
    public void updateState_preferenceSetCheckedWhenSettingIsOn() {
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        Global.putInt(mContext.getContentResolver(), Global.ADD_USERS_WHEN_LOCKED, 1);

        mController.updateState(preference);

        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenSettingIsOff() {
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        Global.putInt(mContext.getContentResolver(), Global.ADD_USERS_WHEN_LOCKED, 0);

        mController.updateState(preference);

        verify(preference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_SettingIsOnWhenPreferenceChecked() {
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        preference.setChecked(true);

        mController.onPreferenceChange(preference, Boolean.TRUE);

        assertThat(Global.getInt(mContext.getContentResolver(), Global.ADD_USERS_WHEN_LOCKED, 0))
                .isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_SettingIsOffWhenPreferenceNotChecked() {
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        preference.setChecked(false);

        mController.onPreferenceChange(preference, Boolean.FALSE);

        assertThat(Global.getInt(mContext.getContentResolver(), Global.ADD_USERS_WHEN_LOCKED, 0))
                .isEqualTo(0);
    }

    @Test
    public void updateState_insecureLockScreen_shouldNotDisplayPreference() {
        mUserManager.setIsAdminUser(true);
        mUserManager.setUserSwitcherEnabled(true);
        mUserManager.setSupportsMultipleUsers(true);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        final AddUserWhenLockedPreferenceController controller =
                new AddUserWhenLockedPreferenceController(mContext, "fake_key");
        ReflectionHelpers.setField(controller, "mLockPatternUtils", mLockPatternUtils);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);

        controller.updateState(preference);

        verify(preference).setVisible(false);
        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateState_secureLockScreen_shouldDisplayPreference() {
        mUserManager.setIsAdminUser(true);
        mUserManager.setUserSwitcherEnabled(true);
        mUserManager.setSupportsMultipleUsers(true);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        final AddUserWhenLockedPreferenceController controller =
                new AddUserWhenLockedPreferenceController(mContext, "fake_key");
        ReflectionHelpers.setField(controller, "mLockPatternUtils", mLockPatternUtils);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);

        controller.updateState(preference);

        verify(preference).setVisible(true);
        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }
}
