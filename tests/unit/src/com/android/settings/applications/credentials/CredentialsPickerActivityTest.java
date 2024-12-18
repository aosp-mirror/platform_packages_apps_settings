/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.applications.credentials;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Flags;
import android.os.UserManager;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CredentialsPickerActivityTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private UserManager mUserManager;

    private Context mMockContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mMockContext = spy(ApplicationProvider.getApplicationContext());
        when(mMockContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
    }

    @Test
    public void testInjectFragmentIntoIntent_normalProfile() {
        Intent intent = new Intent();
        CredentialsPickerActivity.injectFragmentIntoIntent(mMockContext, intent);
        assertThat(intent.getStringExtra(CredentialsPickerActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(DefaultCombinedPicker.class.getName());
    }

    @Test
    public void testInjectFragmentIntoIntent_workProfile() {
        Intent intent = new Intent();

        // Simulate managed / work profile.
        when(mUserManager.isManagedProfile(anyInt())).thenReturn(true);
        assertThat(DefaultCombinedPickerWork.isUserHandledByFragment(mUserManager, 10)).isTrue();

        CredentialsPickerActivity.injectFragmentIntoIntent(mMockContext, intent);
        assertThat(intent.getStringExtra(CredentialsPickerActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(DefaultCombinedPickerWork.class.getName());
    }

    @Test
    public void testInjectFragmentIntoIntent_privateProfile() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        Intent intent = new Intent();

        // Simulate private profile.
        doReturn(true).when(mUserManager).isPrivateProfile();
        assertThat(DefaultCombinedPickerPrivate.isUserHandledByFragment(mUserManager)).isTrue();

        CredentialsPickerActivity.injectFragmentIntoIntent(mMockContext, intent);
        assertThat(intent.getStringExtra(CredentialsPickerActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(DefaultCombinedPickerPrivate.class.getName());
    }
}
