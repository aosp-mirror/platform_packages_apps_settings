/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.security;

import static android.view.contentprotection.flags.Flags.FLAG_MANAGE_DEVICE_POLICY_ENABLED;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ContentProtectionWorkSwitchControllerTest {

    private static final UserHandle TEST_USER_HANDLE = UserHandle.of(10);

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock private PreferenceScreen mMockPreferenceScreen;

    @Mock private RestrictedSwitchPreference mMockSwitchPreference;

    @Nullable private UserHandle mManagedProfileUserHandle;

    @Nullable private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    @DevicePolicyManager.ContentProtectionPolicy
    private int mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;

    private TestContentProtectionWorkSwitchController mController;

    @Before
    public void setUp() {
        mController = new TestContentProtectionWorkSwitchController();
    }

    @Test
    public void constructor_flagDisabled_doesNotFetchData() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.mCounterGetManagedProfile).isEqualTo(0);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(0);
        assertThat(mController.mCounterGetContentProtectionPolicy).isEqualTo(0);
    }

    @Test
    public void constructor_flagEnabled_fetchesManagedProfile() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(0);
        assertThat(mController.mCounterGetContentProtectionPolicy).isEqualTo(0);
    }

    @Test
    public void constructor_flagEnabled_withManagedProfile_fetchesPolicy() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(0);
        assertThat(mController.mCounterGetContentProtectionPolicy).isEqualTo(1);
    }

    @Test
    public void getAvailabilityStatus_flagDisabled_managedProfile_available() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_flagDisabled_noManagedProfile_unavailable() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_flagEnabled_managedProfile_policyDisabled_available() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_flagEnabled_managedProfile_policyEnabled_available() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_flagEnabled_managedProfile_policyNotControlled_unavailable() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_flagEnabled_noManagedProfile_unavailable() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isChecked_flagDisabled_false() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_flagEnabled_policyEnabled_true() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_flagEnabled_policyDisabled_false() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_flagEnabled_policyNotControlled_false() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_alwaysFalse() {
        assertThat(mController.setChecked(true)).isFalse();
        assertThat(mController.setChecked(false)).isFalse();
    }

    @Test
    public void displayPreference_flagDisabled_managedProfile_disabledByAdmin() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        setupForDisplayPreference();

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcedAdmin);
        assertThat(mController.mCounterGetManagedProfile).isEqualTo(3);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
    }

    @Test
    public void displayPreference_flagDisabled_noManagedProfile_notDisabledByAdmin() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        setupForDisplayPreference();

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference, never()).setDisabledByAdmin(any());
        assertThat(mController.mCounterGetManagedProfile).isEqualTo(3);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(0);
    }

    @Test
    public void displayPreference_flagEnabled_managedProfile_disabledByAdmin() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        setupForDisplayPreference();

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcedAdmin);
        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
    }

    @Test
    public void displayPreference_flagEnabled_noManagedProfile_notDisabledByAdmin() {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        setupForDisplayPreference();

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference, never()).setDisabledByAdmin(any());
        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(0);
    }

    private void setupForDisplayPreference() {
        when(mMockPreferenceScreen.findPreference(any())).thenReturn(mMockSwitchPreference);
        when(mMockSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());
        mController = new TestContentProtectionWorkSwitchController();
    }

    private class TestContentProtectionWorkSwitchController
            extends ContentProtectionWorkSwitchController {

        public int mCounterGetManagedProfile;

        public int mCounterGetEnforcedAdmin;

        public int mCounterGetContentProtectionPolicy;

        TestContentProtectionWorkSwitchController() {
            super(ContentProtectionWorkSwitchControllerTest.this.mContext, "key");
        }

        @Override
        @Nullable
        protected UserHandle getManagedProfile() {
            mCounterGetManagedProfile++;
            return mManagedProfileUserHandle;
        }

        @Override
        @Nullable
        protected RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin(
                @NonNull UserHandle userHandle) {
            mCounterGetEnforcedAdmin++;
            return mEnforcedAdmin;
        }

        @Override
        @DevicePolicyManager.ContentProtectionPolicy
        protected int getContentProtectionPolicy(@Nullable UserHandle userHandle) {
            mCounterGetContentProtectionPolicy++;
            return mContentProtectionPolicy;
        }
    }
}
