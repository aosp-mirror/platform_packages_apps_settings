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

package com.android.settings.notification;

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;
import static android.provider.Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS;
import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.testutils.shadow.ShadowUtils;
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
        ShadowUtils.class,
        ShadowRestrictedLockUtilsInternal.class,
})
public class RedactNotificationPreferenceControllerTest {

    @Mock
    private DevicePolicyManager mDpm;
    @Mock
    UserManager mUm;
    @Mock
    KeyguardManager mKm;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private Context mMockContext;

    private Context mContext;
    private RedactNotificationPreferenceController mController;
    private RedactNotificationPreferenceController mWorkController;
    private RestrictedSwitchPreference mPreference;
    private RestrictedSwitchPreference mWorkPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mMockContext))
                .thenReturn(mLockPatternUtils);
        when(mMockContext.getContentResolver()).thenReturn(mContext.getContentResolver());
        when(mMockContext.getSystemService(UserManager.class)).thenReturn(mUm);
        when(mMockContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDpm);
        when(mMockContext.getSystemService(KeyguardManager.class)).thenReturn(mKm);
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {0});

        mController = new RedactNotificationPreferenceController(
                mMockContext, RedactNotificationPreferenceController.KEY_LOCKSCREEN_REDACT);
        mPreference = new RestrictedSwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(
                mController.getPreferenceKey())).thenReturn(mPreference);
        assertThat(mController.mProfileUserId).isEqualTo(0);

        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {0, 10});
        mWorkController = new RedactNotificationPreferenceController(mMockContext,
                RedactNotificationPreferenceController.KEY_LOCKSCREEN_WORK_PROFILE_REDACT);
        mWorkPreference = new RestrictedSwitchPreference(mContext);
        mWorkPreference.setKey(mWorkController.getPreferenceKey());
        when(mScreen.findPreference(
                mWorkController.getPreferenceKey())).thenReturn(mWorkPreference);
        assertThat(mWorkController.mProfileUserId).isEqualTo(10);
    }

    @After
    public void tearDown() {
        ShadowRestrictedLockUtilsInternal.reset();
    }

    @Test
    public void getAvailabilityStatus_noSecureLockscreen() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1, 0);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1, 10);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mWorkController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noWorkProfile() {
        // reset controllers with no work profile
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {UserHandle.myUserId()});
        mWorkController = new RedactNotificationPreferenceController(mMockContext,
                RedactNotificationPreferenceController.KEY_LOCKSCREEN_WORK_PROFILE_REDACT);
        mController = new RedactNotificationPreferenceController(mMockContext,
                RedactNotificationPreferenceController.KEY_LOCKSCREEN_REDACT);

        // should otherwise show
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mWorkController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_adminSaysNoRedaction() {
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(
                KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);

        mController.displayPreference(mScreen);
        RestrictedSwitchPreference primaryPref =
                mScreen.findPreference(mController.getPreferenceKey());
        assertThat(primaryPref.isDisabledByAdmin()).isTrue();
        mWorkController.displayPreference(mScreen);
        RestrictedSwitchPreference workPref =
                mScreen.findPreference(mWorkController.getPreferenceKey());
        assertThat(workPref.isDisabledByAdmin()).isTrue();
    }

    @Test
    public void displayPreference_adminSaysNoSecure() {
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);

        mController.displayPreference(mScreen);
        RestrictedSwitchPreference primaryPref =
                mScreen.findPreference(mController.getPreferenceKey());
        assertThat(primaryPref.isDisabledByAdmin()).isTrue();
        mWorkController.displayPreference(mScreen);
        RestrictedSwitchPreference workPref =
                mScreen.findPreference(mWorkController.getPreferenceKey());
        assertThat(workPref.isDisabledByAdmin()).isTrue();
    }

    @Test
    public void displayPreference() {
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(0);

        mController.displayPreference(mScreen);
        RestrictedSwitchPreference primaryPref =
                mScreen.findPreference(mController.getPreferenceKey());
        assertThat(primaryPref.isDisabledByAdmin()).isFalse();
        mWorkController.displayPreference(mScreen);
        RestrictedSwitchPreference workPref =
                mScreen.findPreference(mWorkController.getPreferenceKey());
        assertThat(workPref.isDisabledByAdmin()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_adminSaysNoNotifications() {
        when(mDpm.getKeyguardDisabledFeatures(eq(null), anyInt())).thenReturn(
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);

        // should otherwise show
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1, 0);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1, 10);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mWorkController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noNotifications() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);

        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                0, 0);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                0, 10);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
        assertThat(mWorkController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_workProfileLocked() {
        // should otherwise show
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1, 0);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1, 10);

        when(mKm.isDeviceLocked(10)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mWorkController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_show() {
        // should otherwise show
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1, 0);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1, 10);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mWorkController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isChecked() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                1, 0);

        assertThat(mController.isChecked()).isTrue();

        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                0, 0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_work() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                1, 10);

        assertThat(mWorkController.isChecked()).isTrue();

        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                0, 10);

        assertThat(mWorkController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_admin() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                1, 0);

        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_false() throws Exception {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                1, 0);

        mController.setChecked(false);
        assertThat(Settings.Secure.getIntForUser(
                mContext.getContentResolver(), LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0))
                .isEqualTo(0);
    }

    @Test
    public void setChecked_workProfile_false() throws Exception {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                1, 10);

        mWorkController.setChecked(false);
        assertThat(Settings.Secure.getIntForUser(
                mContext.getContentResolver(), LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 10))
                .isEqualTo(0);
    }

    @Test
    public void setChecked_true() throws Exception {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                0, 0);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                0, 10);

        mController.setChecked(true);
        mWorkController.setChecked(true);
        assertThat(Settings.Secure.getIntForUser(
                mContext.getContentResolver(), LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 10))
                .isEqualTo(1);
        assertThat(Settings.Secure.getIntForUser(
                mContext.getContentResolver(), LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0))
                .isEqualTo(1);
    }
}

