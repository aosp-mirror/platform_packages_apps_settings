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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ContactSearchPreferenceControllerTest {

    private static final String PREF_KEY = "contacts_search";

    @Mock
    private UserHandle mManagedUser;

    private Context mContext;
    private ContactSearchPreferenceController mController;
    private RestrictedSwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ContactSearchPreferenceController(mContext, PREF_KEY);
        mController.setManagedUser(mManagedUser);
        mPreference = spy(new RestrictedSwitchPreference(mContext));
    }

    @Test
    public void getAvailabilityStatus_noManagedUser_DISABLED() {
        mController.setManagedUser(null);
        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(ContactSearchPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasManagedUser_AVAILABLE() {
        mController.setManagedUser(mManagedUser);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(ContactSearchPreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_shouldRefreshContent() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 0, mManagedUser.getIdentifier());
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();

        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 1, mManagedUser.getIdentifier());
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
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 1, mManagedUser.getIdentifier()))
                .isEqualTo(0);

        mController.onPreferenceChange(mPreference, true);
        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 0, mManagedUser.getIdentifier()))
                .isEqualTo(1);
    }
}