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

import static android.provider.ContactsContract.RawContacts.DefaultAccount.KEY_DEFAULT_ACCOUNT_STATE;
import static android.provider.ContactsContract.RawContacts.DefaultAccount.QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD;
import static android.provider.ContactsContract.Settings;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts.DefaultAccount.DefaultAccountAndState;
import android.provider.Flags;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAuthenticationHelper;

import org.junit.Before;
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
    private ContentProviderClient mContentProviderClient;

    @Mock
    private Resources mResources;

    @Mock
    private AccountManager mAccountManager;

    private ContactsStoragePreferenceController mPreferenceController;

    @Before
    public void setUp() throws Exception {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mContentResolver.acquireContentProviderClient(
                eq(ContactsContract.AUTHORITY_URI))).thenReturn(mContentProviderClient);
        when(mContext.getSystemService(eq(Context.ACCOUNT_SERVICE))).thenReturn(mAccountManager);
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(new Account[]{});
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_NOT_SET);
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(bundle);
        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_DEFAULT_ACCOUNT_API_ENABLED)
    public void getAvailabilityStatus_flagIsOn_shouldReturnAvailable() {
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_DEFAULT_ACCOUNT_API_ENABLED)
    public void getAvailabilityStatus_flagIsOff_shouldReturnConditionallyUnavailable() {
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_DEFAULT_ACCOUNT_API_ENABLED)
    public void getAvailabilityStatus_illegalStateExceptionThrown_shouldReturnConditionallyUnavailable()
            throws Exception {
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenThrow(new IllegalStateException());

        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);

        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_DEFAULT_ACCOUNT_API_ENABLED)
    public void getAvailabilityStatus_runtimeExceptionThrown_shouldReturnConditionallyUnavailable()
            throws Exception {
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenThrow(new RuntimeException());

        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);

        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getSummary_noAccountIsSetAsDefault_shouldReturnNoAccountSetSummary() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getString(eq(R.string.contacts_storage_no_account_set_summary))).thenReturn(
                "No default set");

        // Fetch the default account from CP2.
        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);

        assertThat(mPreferenceController.getSummary()).isEqualTo("No default set");
    }

    @Test
    public void getSummary_localAccountIsSetAsDefault_shouldReturnLocalAccountSetSummary()
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_LOCAL);
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(bundle);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getString(eq(R.string.contacts_storage_local_account_summary))).thenReturn(
                "Device only");
        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);

        assertThat(mPreferenceController.getSummary()).isEqualTo("Device only");
    }

    @Test
    public void getSummary_simAccountIsSetAsDefault_shouldReturnSimAccountSummary()
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_SIM);
        bundle.putString(Settings.ACCOUNT_TYPE, "SIM");
        bundle.putString(Settings.ACCOUNT_NAME, "SIM");
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(bundle);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getString(eq(R.string.sim_card_label))).thenReturn("SIM");
        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);

        assertThat(mPreferenceController.getSummary()).isEqualTo("SIM");
    }

    @Test
    public void getSummary_googleAccountIsSetAsDefault_shouldReturnGoogleAccountTypeAndAccountName()
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD);
        bundle.putString(Settings.ACCOUNT_TYPE, "type1");
        bundle.putString(Settings.ACCOUNT_NAME, "test@gmail.com");
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(bundle);
        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);

        assertThat(mPreferenceController.getSummary()).isEqualTo("LABEL1 | test@gmail.com");
    }

    @Test
    public void getSummary_samsungAccountIsSetAsDefault_shouldReturnSamsungAccountTypeAndAccountName()
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD);
        bundle.putString(Settings.ACCOUNT_TYPE, "type2");
        bundle.putString(Settings.ACCOUNT_NAME, "test@samsung.com");
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(bundle);
        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);

        assertThat(mPreferenceController.getSummary()).isEqualTo("LABEL2 | test@samsung.com");
    }

    @Test
    public void getSummary_accountLabelSameAsAccountName_onlyReturnAccountName() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD);
        bundle.putString(Settings.ACCOUNT_TYPE, "type3");
        bundle.putString(Settings.ACCOUNT_NAME, "LABEL3");
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(bundle);
        mPreferenceController = new ContactsStoragePreferenceController(mContext,
                CONTACTS_DEFAULT_ACCOUNT_PREFERENCE_KEY);

        // Since package name and account name is the same, we only return account name.
        assertThat(mPreferenceController.getSummary()).isEqualTo("LABEL3");
    }
}
