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
import static android.provider.ContactsContract.Settings.SET_DEFAULT_ACCOUNT_METHOD;
import static android.provider.Settings.ACTION_ADD_ACCOUNT;
import static android.provider.Settings.EXTRA_ACCOUNT_TYPES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.SearchIndexableResource;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accounts.AddAccountSettings;
import com.android.settings.testutils.shadow.ShadowAuthenticationHelper;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAuthenticationHelper.class)
public class ContactsStorageSettingsTest {
    private static final String PREF_KEY_DEVICE_ONLY = "device_only_account_preference";

    private static final String PREF_KEY_ADD_ACCOUNT = "add_account";

    private static final Account TEST_ACCOUNT1 = new Account("test@gmail.com", "type1");

    private static final Account TEST_ACCOUNT2 = new Account("test@samsung.com", "type2");

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    @Spy
    public final Context mContext = spy(ApplicationProvider.getApplicationContext());
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private AccountManager mAccountManager;

    private PreferenceManager mPreferenceManager;
    private TestContactsStorageSettings mContactsStorageSettings;
    private PreferenceScreen mScreen;

    @Before
    public void setUp() throws Exception {
        mContactsStorageSettings = spy(new TestContactsStorageSettings(mContext, mContentResolver));
        when(mContext.getSystemService(eq(Context.ACCOUNT_SERVICE))).thenReturn(mAccountManager);
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(new Account[]{});
        mPreferenceManager = new PreferenceManager(mContext);
        when(mContactsStorageSettings.getPreferenceManager()).thenReturn(mPreferenceManager);
        mScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));
        when(mScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mContactsStorageSettings.getPreferenceScreen()).thenReturn(mScreen);
        mContactsStorageSettings.onAttach(mContext);
    }

    @Test
    public void getMetricsCategory() {
        assertThat(mContactsStorageSettings.getMetricsCategory()).isEqualTo(
                SettingsEnums.CONTACTS_STORAGE);
    }

    @Test
    public void getPreferenceScreenResId() {
        assertThat(mContactsStorageSettings.getPreferenceScreenResId()).isEqualTo(
                R.xml.contacts_storage_settings);
    }

    @Test
    public void verifyDeviceOnlyPreference_onClick_setDefaultAccountToNull() {
        when(mAccountManager.getAccounts()).thenReturn(new Account[]{});
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_DEFAULT_ACCOUNT, null);
        when(mContentResolver.call(eq(ContactsContract.AUTHORITY_URI),
                eq(QUERY_DEFAULT_ACCOUNT_METHOD), any(), any())).thenReturn(bundle);

        PreferenceScreen settingScreen = mPreferenceManager.inflateFromResource(mContext,
                R.xml.contacts_storage_settings, mScreen);
        SelectorWithWidgetPreference deviceOnlyPreference = settingScreen.findPreference(
                PREF_KEY_DEVICE_ONLY);
        when(mContactsStorageSettings.findPreference(eq(PREF_KEY_DEVICE_ONLY))).thenReturn(
                deviceOnlyPreference);

        assertThat(deviceOnlyPreference.getTitle()).isEqualTo("Device only");
        assertThat(deviceOnlyPreference.getSummary()).isEqualTo(
                "New contacts won't be synced with an account");
        assertThat(deviceOnlyPreference.getOrder()).isEqualTo(999);

        mContactsStorageSettings.refreshUI();
        mContactsStorageSettings.onRadioButtonClicked(deviceOnlyPreference);

        assertThat(deviceOnlyPreference.isChecked()).isTrue();
        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(mContentResolver).call(eq(ContactsContract.AUTHORITY_URI),
                eq(SET_DEFAULT_ACCOUNT_METHOD), any(), captor.capture());
        Bundle accountBundle = captor.getValue();
        assertThat(accountBundle.getString(ContactsContract.Settings.ACCOUNT_NAME)).isNull();
        assertThat(accountBundle.getString(ContactsContract.Settings.ACCOUNT_TYPE)).isNull();
    }

    @Test
    public void verifyAddAccountPreference_onClick_startAddAccountActivity() {
        when(mAccountManager.getAccounts()).thenReturn(new Account[]{});
        when(mContentResolver.call(eq(ContactsContract.AUTHORITY_URI),
                eq(QUERY_DEFAULT_ACCOUNT_METHOD), any(), any())).thenReturn(Bundle.EMPTY);

        mContactsStorageSettings.refreshUI();

        assertThat(mScreen.findPreference(PREF_KEY_ADD_ACCOUNT).getTitle()).isEqualTo(
                "Add an account to get started");
        assertThat(mScreen.findPreference(PREF_KEY_ADD_ACCOUNT).getOrder()).isEqualTo(998);

        mScreen.findPreference(PREF_KEY_ADD_ACCOUNT).performClick();

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(captor.capture());
        Intent addAccountIntent = captor.getValue();
        assertThat(addAccountIntent.getAction()).isEqualTo(ACTION_ADD_ACCOUNT);
        assertThat(addAccountIntent.getComponent().getClassName()).isEqualTo(
                AddAccountSettings.class.getCanonicalName());
        String[] eligibleAccounts = (String[]) addAccountIntent.getExtra(EXTRA_ACCOUNT_TYPES);
        assertThat(eligibleAccounts).isEmpty();
    }

    @Test
    public void verifyEligibleAccountPreference_onClick_setSelectedDefaultAccount() {
        when(mAccountManager.getAccounts()).thenReturn(new Account[]{TEST_ACCOUNT1, TEST_ACCOUNT2});
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_DEFAULT_ACCOUNT, TEST_ACCOUNT2);
        when(mContentResolver.call(eq(ContactsContract.AUTHORITY_URI),
                eq(QUERY_DEFAULT_ACCOUNT_METHOD), any(), any())).thenReturn(bundle);

        mContactsStorageSettings.refreshUI();

        SelectorWithWidgetPreference account1Preference = mScreen.findPreference(
                String.valueOf(TEST_ACCOUNT1.hashCode()));
        assertThat(account1Preference.getTitle()).isEqualTo("LABEL1");
        assertThat(account1Preference.getSummary()).isEqualTo("test@gmail.com");
        assertThat(account1Preference.getIcon()).isNotNull();

        SelectorWithWidgetPreference account2Preference = mScreen.findPreference(
                String.valueOf(TEST_ACCOUNT2.hashCode()));
        assertThat(account2Preference.getTitle()).isEqualTo("LABEL2");
        assertThat(account2Preference.getSummary()).isEqualTo("test@samsung.com");
        assertThat(account2Preference.getIcon()).isNotNull();

        mContactsStorageSettings.onRadioButtonClicked(account2Preference);
        assertThat(account1Preference.isChecked()).isFalse();
        assertThat(account2Preference.isChecked()).isTrue();

        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(mContentResolver).call(eq(ContactsContract.AUTHORITY_URI),
                eq(SET_DEFAULT_ACCOUNT_METHOD), any(), captor.capture());
        Bundle setAccountBundle = captor.getValue();
        assertThat(setAccountBundle.getString(ContactsContract.Settings.ACCOUNT_NAME)).isEqualTo(
                "test@samsung.com");
        assertThat(setAccountBundle.getString(ContactsContract.Settings.ACCOUNT_TYPE)).isEqualTo(
                "type2");
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                ContactsStorageSettings.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(
                mContactsStorageSettings.getPreferenceScreenResId());
    }

    private static class TestContactsStorageSettings extends ContactsStorageSettings {
        private final Context mContext;
        private final ContentResolver mContentResolver;

        TestContactsStorageSettings(Context context, ContentResolver contentResolver) {
            mContext = context;
            mContentResolver = contentResolver;
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        protected ContentResolver getContentResolver() {
            // Override it so we can access this method in test
            return mContentResolver;
        }
    }
}
