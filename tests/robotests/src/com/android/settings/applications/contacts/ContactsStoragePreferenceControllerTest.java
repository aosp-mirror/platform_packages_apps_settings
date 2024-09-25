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

package com.android.settings.applications.contacts;

import static android.provider.ContactsContract.Settings.KEY_DEFAULT_ACCOUNT;
import static android.provider.ContactsContract.Settings.QUERY_DEFAULT_ACCOUNT_METHOD;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.ContactsContract;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowAuthenticationHelper;

import org.junit.Before;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAuthenticationHelper.class)
public class ContactsStoragePreferenceControllerTest {

    private static final String CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY =
            "contacts_default_account";
    private static final Account TEST_ACCOUNT1 = new Account("test@gmail.com", "type1");

    private static final Account TEST_ACCOUNT2 = new Account("test@samsung.com", "type2");

    private static final Account TEST_ACCOUNT3 = new Account("LABEL3", "type3");

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private Context mContext;

    @Mock
    private ContentResolver mContentResolver;

    @Mock
    private Resources mResources;

    @Mock
    private AccountManager mAccountManager;

    private ContactsStoragePreferenceController mPreferenceController;

    @Before
    public void setUp() throws Exception {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mContext.getSystemService(eq(Context.ACCOUNT_SERVICE))).thenReturn(mAccountManager);
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(new Account[]{});

        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CONTACTS_DEFAULT_ACCOUNT_IN_SETTINGS)
    public void getAvailabilityStatus_flagIsOn_shouldReturnAvailable() {

        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_CONTACTS_DEFAULT_ACCOUNT_IN_SETTINGS)
    public void getAvailabilityStatus_flagIsOff_shouldReturnConditionallyUnavailable() {

        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getSummary_noAccountIsSetAsDefault_shouldReturnNoAccountSetSummary() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_DEFAULT_ACCOUNT, null);
        when(mContentResolver.call(eq(ContactsContract.AUTHORITY_URI),
                eq(QUERY_DEFAULT_ACCOUNT_METHOD), any(), any())).thenReturn(bundle);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getString(eq(R.string.contacts_storage_no_account_set))).thenReturn(
                "No default set");

        assertThat(mPreferenceController.getSummary()).isEqualTo("No default set");
    }

    @Test
    public void getSummary_googleAccountIsSetAsDefault_shouldReturnGoogleAccountTypeAndAccountName() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_DEFAULT_ACCOUNT, TEST_ACCOUNT1);
        when(mContentResolver.call(eq(ContactsContract.AUTHORITY_URI),
                eq(QUERY_DEFAULT_ACCOUNT_METHOD), any(), any())).thenReturn(bundle);

        assertThat(mPreferenceController.getSummary()).isEqualTo("LABEL1 | test@gmail.com");
    }

    @Test
    public void getSummary_samsungAccountIsSetAsDefault_shouldReturnSamsungAccountTypeAndAccountName() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_DEFAULT_ACCOUNT, TEST_ACCOUNT2);
        when(mContentResolver.call(eq(ContactsContract.AUTHORITY_URI),
                eq(QUERY_DEFAULT_ACCOUNT_METHOD), any(), any())).thenReturn(bundle);

        assertThat(mPreferenceController.getSummary()).isEqualTo("LABEL2 | test@samsung.com");
    }

    @Test
    public void getSummary_accountLabelSameAsAccountName_onlyReturnAccountName() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_DEFAULT_ACCOUNT, TEST_ACCOUNT3);
        when(mContentResolver.call(eq(ContactsContract.AUTHORITY_URI),
                eq(QUERY_DEFAULT_ACCOUNT_METHOD), any(), any())).thenReturn(bundle);

        // Since package name and account name is the same, we only return account name.
        assertThat(mPreferenceController.getSummary()).isEqualTo("LABEL3");
    }
}
