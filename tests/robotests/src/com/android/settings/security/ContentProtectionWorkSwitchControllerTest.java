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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ContentProtectionWorkSwitchControllerTest {
    private static final UserHandle TEST_USER_HANDLE = UserHandle.of(10);

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock private PreferenceScreen mMockPreferenceScreen;
    private ContentProtectionWorkSwitchController mController;
    private UserHandle mManagedProfileUserHandle;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new TestContentProtectionWorkSwitchController();
    }

    @Test
    public void isAvailable_managedProfile_available() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noManagedProfile_notAvailable() {
        mManagedProfileUserHandle = null;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isChecked_noManagedProfile_alwaysOff() {
        mManagedProfileUserHandle = null;

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_managedProfile_alwaysOff() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_alwaysFalse() {
        assertThat(mController.setChecked(true)).isFalse();
        assertThat(mController.setChecked(false)).isFalse();
    }

    @Test
    public void displayPreference_managedProfile_disabled() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        RestrictedSwitchPreference mockSwitchPreference = mock(RestrictedSwitchPreference.class);
        when(mMockPreferenceScreen.findPreference(any())).thenReturn(mockSwitchPreference);
        when(mockSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());

        mController.displayPreference(mMockPreferenceScreen);

        assertThat(mController.isAvailable()).isTrue();
        verify(mockSwitchPreference).setDisabledByAdmin(mEnforcedAdmin);
    }

    @Test
    public void displayPreference_noManagedProfile_notDisabled() {
        mManagedProfileUserHandle = null;
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        RestrictedSwitchPreference mockSwitchPreference = mock(RestrictedSwitchPreference.class);
        when(mMockPreferenceScreen.findPreference(any())).thenReturn(mockSwitchPreference);
        when(mockSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());

        mController.displayPreference(mMockPreferenceScreen);

        assertThat(mController.isAvailable()).isFalse();
        verify(mockSwitchPreference, never()).setDisabledByAdmin(any());
    }

    @Test
    public void displayPreference_noEnforcedAdmin_notDisabled() {
        mManagedProfileUserHandle = null;
        mEnforcedAdmin = null;
        RestrictedSwitchPreference mockSwitchPreference = mock(RestrictedSwitchPreference.class);
        when(mMockPreferenceScreen.findPreference(any())).thenReturn(mockSwitchPreference);
        when(mockSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());

        mController.displayPreference(mMockPreferenceScreen);

        assertThat(mController.isAvailable()).isFalse();
        verify(mockSwitchPreference, never()).setDisabledByAdmin(any());
    }

    private class TestContentProtectionWorkSwitchController
            extends ContentProtectionWorkSwitchController {

        TestContentProtectionWorkSwitchController() {
            super(ContentProtectionWorkSwitchControllerTest.this.mContext, "key");
        }

        @Override
        @Nullable
        protected UserHandle getManagedProfile() {
            return mManagedProfileUserHandle;
        }

        @Override
        @Nullable
        protected RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin(
                @NonNull UserHandle managedProfile) {
            return mEnforcedAdmin;
        }
    }
}
