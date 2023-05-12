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

import static android.provider.Settings.Secure.MANAGED_PROFILE_CONTACT_REMOTE_SEARCH;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class ContactSearchPreferenceControllerTest {

    private static final String PREF_KEY = "contacts_search";
    private static final int MANAGED_USER_ID = 10;

    private Context mContext;
    private ContactSearchPreferenceController mController;
    private RestrictedSwitchPreference mPreference;

    @Mock
    private UserHandle mManagedUser;
    @Mock
    private UserManager mUserManager;
    @Mock
    private UserInfo mUserInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        mPreference = spy(new RestrictedSwitchPreference(mContext));
        when(mUserInfo.isManagedProfile()).thenReturn(true);
        when(mUserManager.getUserInfo(anyInt())).thenReturn(mUserInfo);
        when(mUserManager.getProcessUserId()).thenReturn(0);
        when(mUserManager.getUserProfiles()).thenReturn(Collections.singletonList(mManagedUser));
        when(mManagedUser.getIdentifier()).thenReturn(MANAGED_USER_ID);
        mController = new ContactSearchPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void getAvailabilityStatus_noManagedUser_DISABLED() {
        when(mUserManager.getProcessUserId()).thenReturn(MANAGED_USER_ID);
        mController = new ContactSearchPreferenceController(mContext, PREF_KEY);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(ContactSearchPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasManagedUser_AVAILABLE() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(ContactSearchPreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_shouldRefreshContent() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 0, MANAGED_USER_ID);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();

        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 1, MANAGED_USER_ID);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_preferenceShouldBeDisabled() {
        mController.updateState(mPreference);

        verify(mPreference).setDisabledByAdmin(any());
    }

    @Test
    public void onPreferenceChange_shouldUpdateProviderValue() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 1, MANAGED_USER_ID))
                .isEqualTo(0);

        mController.onPreferenceChange(mPreference, true);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 0, MANAGED_USER_ID))
                .isEqualTo(1);
    }

    @Test
    public void onQuietModeDisabled_preferenceEnabled() {
        when(mUserManager.isQuietModeEnabled(any(UserHandle.class))).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void onQuietModeEnabled_preferenceDisabledAndUnchecked() {
        when(mUserManager.isQuietModeEnabled(any(UserHandle.class))).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void afterQuietModeTurnedOnAndOffWhenPreferenceChecked_toggleCheckedAndEnabled() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 1, MANAGED_USER_ID);
        when(mUserManager.isQuietModeEnabled(any(UserHandle.class))).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();

        when(mUserManager.isQuietModeEnabled(any(UserHandle.class))).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void afterQuietModeTurnedOnAndOffWhenPreferenceUnchecked_toggleUncheckedAndEnabled() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 0, MANAGED_USER_ID);
        when(mUserManager.isQuietModeEnabled(any(UserHandle.class))).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();

        when(mUserManager.isQuietModeEnabled(any(UserHandle.class))).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isFalse();
    }
}
