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
import static android.provider.ContactsContract.RawContacts.DefaultAccount.KEY_ELIGIBLE_DEFAULT_ACCOUNTS;
import static android.provider.ContactsContract.RawContacts.DefaultAccount.QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD;
import static android.provider.ContactsContract.RawContacts.DefaultAccount.QUERY_ELIGIBLE_DEFAULT_ACCOUNTS_METHOD;
import static android.provider.ContactsContract.RawContacts.DefaultAccount.SET_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD;
import static android.provider.Settings.ACTION_ADD_ACCOUNT;
import static android.provider.Settings.EXTRA_ACCOUNT_TYPES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.app.settings.SettingsEnums;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts.DefaultAccount.DefaultAccountAndState;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accounts.AddAccountSettings;
import com.android.settingslib.accounts.AuthenticatorHelper;
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
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ContactsStorageSettingsTest.ShadowAuthenticatorHelper.class)
public class ContactsStorageSettingsTest {
    private static final String PREF_KEY_DEVICE_ONLY = "device_only_account_preference";
    private static final String PREF_KEY_ACCOUNT_CATEGORY = "account_category";
    private static final String PREF_KEY_ADD_ACCOUNT = "add_account";

    private static final Account TEST_ACCOUNT1 = new Account("test@gmail.com", "com.google");

    private static final Account TEST_ACCOUNT2 = new Account("test@samsung.com", "com.samsung");

    private static final Account TEST_ACCOUNT3 = new Account("test@outlook.com", "com.outlook");

    private static final Account SIM_ACCOUNT = new Account("SIM", "SIM");

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    @Spy
    public final Context mContext = spy(ApplicationProvider.getApplicationContext());
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private ContentProviderClient mContentProviderClient;
    private PreferenceManager mPreferenceManager;
    private TestContactsStorageSettings mContactsStorageSettings;
    private PreferenceScreen mScreen;
    private PreferenceGroup accountCategory;

    @Before
    public void setUp() throws Exception {
        mContactsStorageSettings = spy(
                new TestContactsStorageSettings(mContext, mContentResolver));
        when(mContentResolver.acquireContentProviderClient(
                eq(ContactsContract.AUTHORITY_URI))).thenReturn(mContentProviderClient);
        mPreferenceManager = new PreferenceManager(mContext);
        when(mContactsStorageSettings.getPreferenceManager()).thenReturn(mPreferenceManager);
        mScreen = spy(mPreferenceManager.inflateFromResource(mContext,
                R.xml.contacts_storage_settings, mScreen));
        when(mScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        accountCategory = mScreen.findPreference(PREF_KEY_ACCOUNT_CATEGORY);
        SelectorWithWidgetPreference deviceOnlyPreference = mScreen.findPreference(
                PREF_KEY_DEVICE_ONLY);
        when(mContactsStorageSettings.findPreference(eq(PREF_KEY_DEVICE_ONLY))).thenReturn(
                deviceOnlyPreference);
        when(mContactsStorageSettings.findPreference(eq(PREF_KEY_ACCOUNT_CATEGORY))).thenReturn(
                accountCategory);
        when(mContactsStorageSettings.getPreferenceScreen()).thenReturn(mScreen);
        mContactsStorageSettings.setEligibleAccountTypes(new String[]{"com.google"});
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
    public void verifyDeviceOnlyPreference_onClick_setDefaultAccountToNull() throws Exception {
        Bundle currentDefaultAccount = new Bundle();
        currentDefaultAccount.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_NOT_SET);
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(currentDefaultAccount);
        Bundle eligibleAccountBundle = new Bundle();
        eligibleAccountBundle.putParcelableArrayList(KEY_ELIGIBLE_DEFAULT_ACCOUNTS,
                new ArrayList<>());
        when(mContentProviderClient.call(eq(QUERY_ELIGIBLE_DEFAULT_ACCOUNTS_METHOD), any(),
                any())).thenReturn(eligibleAccountBundle);

        SelectorWithWidgetPreference deviceOnlyPreference = mContactsStorageSettings.findPreference(
                PREF_KEY_DEVICE_ONLY);

        assertThat(deviceOnlyPreference.getTitle()).isEqualTo("Device only");
        assertThat(deviceOnlyPreference.getSummary()).isEqualTo(
                "New contacts won't be synced with an account");
        assertThat(deviceOnlyPreference.getOrder()).isEqualTo(999);
        assertThat(mContactsStorageSettings.findPreference(
                PREF_KEY_ACCOUNT_CATEGORY).getTitle()).isEqualTo("Where to save contacts");

        mContactsStorageSettings.refreshUI();
        mContactsStorageSettings.onRadioButtonClicked(deviceOnlyPreference);

        assertThat(deviceOnlyPreference.isChecked()).isTrue();
        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(mContentProviderClient).call(eq(SET_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                captor.capture());
        Bundle accountBundle = captor.getValue();
        assertThat(accountBundle.getString(ContactsContract.Settings.ACCOUNT_NAME)).isNull();
        assertThat(accountBundle.getString(ContactsContract.Settings.ACCOUNT_TYPE)).isNull();
    }

    @Test
    public void verifyAddAccountPreference_eligibleAccountsAvailable_startAddAccountActivityOnClick()
            throws Exception {
        Bundle currentDefaultAccount = new Bundle();
        currentDefaultAccount.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_NOT_SET);
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(currentDefaultAccount);
        Bundle eligibleAccountBundle = new Bundle();
        eligibleAccountBundle.putParcelableArrayList(KEY_ELIGIBLE_DEFAULT_ACCOUNTS,
                new ArrayList<>());
        when(mContentProviderClient.call(eq(QUERY_ELIGIBLE_DEFAULT_ACCOUNTS_METHOD), any(),
                any())).thenReturn(eligibleAccountBundle);

        mContactsStorageSettings.refreshUI();

        assertThat(mContactsStorageSettings.findPreference(
                PREF_KEY_ACCOUNT_CATEGORY).getTitle()).isEqualTo("Where to save contacts");
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
        assertThat(List.of(eligibleAccounts)).containsExactly("com.google");
    }

    @Test
    public void verifyAddAccountPreference_noEligibleAccountsAvailable_dontShowPreference()
            throws Exception {
        Bundle currentDefaultAccount = new Bundle();
        currentDefaultAccount.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_NOT_SET);
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(currentDefaultAccount);
        Bundle eligibleAccountBundle = new Bundle();
        eligibleAccountBundle.putParcelableArrayList(KEY_ELIGIBLE_DEFAULT_ACCOUNTS,
                new ArrayList<>());
        when(mContentProviderClient.call(eq(QUERY_ELIGIBLE_DEFAULT_ACCOUNTS_METHOD), any(),
                any())).thenReturn(eligibleAccountBundle);
        mContactsStorageSettings.setEligibleAccountTypes(new String[]{});

        mContactsStorageSettings.refreshUI();

        Preference addAccountPreference = mScreen.findPreference(PREF_KEY_ADD_ACCOUNT);
        assertThat(addAccountPreference).isNull();
    }

    @Test
    public void verifyEligibleAccountPreference_onClick_setSelectedDefaultAccount()
            throws Exception {
        Bundle currentDefaultAccount = new Bundle();
        currentDefaultAccount.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD);
        currentDefaultAccount.putString(ContactsContract.Settings.ACCOUNT_NAME, TEST_ACCOUNT2.name);
        currentDefaultAccount.putString(ContactsContract.Settings.ACCOUNT_TYPE, TEST_ACCOUNT2.type);
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(currentDefaultAccount);
        Bundle eligibleAccountBundle = new Bundle();
        ArrayList<Account> eligibleAccounts = new ArrayList<>(
                List.of(TEST_ACCOUNT1, TEST_ACCOUNT2));
        eligibleAccountBundle.putParcelableArrayList(KEY_ELIGIBLE_DEFAULT_ACCOUNTS,
                eligibleAccounts);
        when(mContentProviderClient.call(eq(QUERY_ELIGIBLE_DEFAULT_ACCOUNTS_METHOD), any(),
                any())).thenReturn(eligibleAccountBundle);

        mContactsStorageSettings.refreshUI();

        SelectorWithWidgetPreference account1Preference = accountCategory.findPreference(
                String.valueOf(TEST_ACCOUNT1.hashCode()));
        assertThat(account1Preference.getTitle()).isEqualTo("Device & Google");
        assertThat(account1Preference.getSummary()).isEqualTo("test@gmail.com");
        assertThat(account1Preference.getIcon()).isNotNull();

        SelectorWithWidgetPreference account2Preference = accountCategory.findPreference(
                String.valueOf(TEST_ACCOUNT2.hashCode()));
        assertThat(account2Preference.getTitle()).isEqualTo("Device & Samsung");
        assertThat(account2Preference.getSummary()).isEqualTo("test@samsung.com");
        assertThat(account2Preference.getIcon()).isNotNull();

        mContactsStorageSettings.onRadioButtonClicked(account2Preference);
        assertThat(account1Preference.isChecked()).isFalse();
        assertThat(account2Preference.isChecked()).isTrue();

        ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
        verify(mContentProviderClient).call(eq(SET_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                captor.capture());
        Bundle setAccountBundle = captor.getValue();
        assertThat(setAccountBundle.getString(ContactsContract.Settings.ACCOUNT_NAME)).isEqualTo(
                "test@samsung.com");
        assertThat(setAccountBundle.getString(ContactsContract.Settings.ACCOUNT_TYPE)).isEqualTo(
                "com.samsung");

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        Intent moveContactsIntent = intentCaptor.getValue();
        assertThat(moveContactsIntent.getAction()).isEqualTo(
                ContactsContract.RawContacts.DefaultAccount.ACTION_MOVE_CONTACTS_TO_DEFAULT_ACCOUNT);
        assertThat(moveContactsIntent.getPackage()).isEqualTo(
                "com.android.providers.contacts");
    }

    @Test
    public void verifyAccountPreference_defaultAccountIsNotEligibleCloudAccount_createNewDefaultAccountPreference()
            throws Exception {
        Bundle currentDefaultAccount = new Bundle();
        currentDefaultAccount.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD);
        currentDefaultAccount.putString(ContactsContract.Settings.ACCOUNT_NAME, TEST_ACCOUNT3.name);
        currentDefaultAccount.putString(ContactsContract.Settings.ACCOUNT_TYPE, TEST_ACCOUNT3.type);
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(currentDefaultAccount);
        Bundle eligibleAccountBundle = new Bundle();
        ArrayList<Account> eligibleAccounts = new ArrayList<>(
                List.of(TEST_ACCOUNT1, TEST_ACCOUNT2));
        eligibleAccountBundle.putParcelableArrayList(KEY_ELIGIBLE_DEFAULT_ACCOUNTS,
                eligibleAccounts);
        when(mContentProviderClient.call(eq(QUERY_ELIGIBLE_DEFAULT_ACCOUNTS_METHOD), any(),
                any())).thenReturn(eligibleAccountBundle);

        mContactsStorageSettings.refreshUI();

        SelectorWithWidgetPreference account1Preference = accountCategory.findPreference(
                String.valueOf(TEST_ACCOUNT1.hashCode()));
        assertThat(account1Preference.getTitle()).isEqualTo("Device & Google");
        assertThat(account1Preference.getSummary()).isEqualTo("test@gmail.com");
        assertThat(account1Preference.getIcon()).isNotNull();

        SelectorWithWidgetPreference account2Preference = accountCategory.findPreference(
                String.valueOf(TEST_ACCOUNT2.hashCode()));
        assertThat(account2Preference.getTitle()).isEqualTo("Device & Samsung");
        assertThat(account2Preference.getSummary()).isEqualTo("test@samsung.com");
        assertThat(account2Preference.getIcon()).isNotNull();

        SelectorWithWidgetPreference account3Preference = accountCategory.findPreference(
                String.valueOf(TEST_ACCOUNT3.hashCode()));
        assertThat(account3Preference.getTitle()).isEqualTo("Device & Outlook");
        assertThat(account3Preference.getSummary()).isEqualTo("test@outlook.com");
        assertThat(account3Preference.getIcon()).isNotNull();

        assertThat(account1Preference.isChecked()).isFalse();
        assertThat(account2Preference.isChecked()).isFalse();
        assertThat(account3Preference.isChecked()).isTrue();
    }

    @Test
    public void verifyAccountPreference_defaultAccountIsSimAccount_createSimAccountPreference()
            throws Exception {
        Bundle currentDefaultAccount = new Bundle();
        currentDefaultAccount.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_SIM);
        currentDefaultAccount.putString(ContactsContract.Settings.ACCOUNT_NAME, SIM_ACCOUNT.name);
        currentDefaultAccount.putString(ContactsContract.Settings.ACCOUNT_TYPE, SIM_ACCOUNT.type);
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(currentDefaultAccount);
        Bundle eligibleAccountBundle = new Bundle();
        eligibleAccountBundle.putParcelableArrayList(KEY_ELIGIBLE_DEFAULT_ACCOUNTS,
                new ArrayList<>());
        when(mContentProviderClient.call(eq(QUERY_ELIGIBLE_DEFAULT_ACCOUNTS_METHOD), any(),
                any())).thenReturn(eligibleAccountBundle);

        mContactsStorageSettings.refreshUI();

        SelectorWithWidgetPreference simPreference = accountCategory.findPreference(
                String.valueOf(SIM_ACCOUNT.hashCode()));
        assertThat(simPreference.getTitle()).isEqualTo("SIM");
        assertThat(simPreference.getSummary()).isEqualTo("SIM");
        assertThat(simPreference.getIcon()).isNotNull();
        assertThat(simPreference.isChecked()).isTrue();
    }

    @Test
    public void verifyAccountPreference_newAccountAdded_accountAddedToAccountPreference()
            throws Exception {
        Bundle currentDefaultAccount = new Bundle();
        currentDefaultAccount.putInt(KEY_DEFAULT_ACCOUNT_STATE,
                DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD);
        currentDefaultAccount.putString(ContactsContract.Settings.ACCOUNT_NAME, TEST_ACCOUNT1.name);
        currentDefaultAccount.putString(ContactsContract.Settings.ACCOUNT_TYPE, TEST_ACCOUNT1.type);
        when(mContentProviderClient.call(eq(QUERY_DEFAULT_ACCOUNT_FOR_NEW_CONTACTS_METHOD), any(),
                any())).thenReturn(currentDefaultAccount);
        Bundle eligibleAccountBundle = new Bundle();
        ArrayList<Account> eligibleAccounts = new ArrayList<>(
                List.of(TEST_ACCOUNT1, TEST_ACCOUNT2));
        eligibleAccountBundle.putParcelableArrayList(KEY_ELIGIBLE_DEFAULT_ACCOUNTS,
                eligibleAccounts);
        when(mContentProviderClient.call(eq(QUERY_ELIGIBLE_DEFAULT_ACCOUNTS_METHOD), any(),
                any())).thenReturn(eligibleAccountBundle);

        mContactsStorageSettings.onAccountsUpdate(null);

        // onAccountsUpdate should refresh the icon and layouts.
        SelectorWithWidgetPreference account1Preference = accountCategory.findPreference(
                String.valueOf(TEST_ACCOUNT1.hashCode()));
        assertThat(account1Preference.getTitle()).isEqualTo("Device & Google");
        assertThat(account1Preference.getSummary()).isEqualTo("test@gmail.com");
        assertThat(account1Preference.getIcon()).isNotNull();

        SelectorWithWidgetPreference account2Preference = accountCategory.findPreference(
                String.valueOf(TEST_ACCOUNT2.hashCode()));
        assertThat(account2Preference.getTitle()).isEqualTo("Device & Samsung");
        assertThat(account2Preference.getSummary()).isEqualTo("test@samsung.com");
        assertThat(account2Preference.getIcon()).isNotNull();
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
        private String[] mEligibleAccountTypes;

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

        @Override
        String[] getEligibleAccountTypes() {
            return mEligibleAccountTypes == null ? Resources.getSystem().getStringArray(
                    com.android.internal.R.array.config_rawContactsEligibleDefaultAccountTypes)
                    : mEligibleAccountTypes;
        }

        public void setEligibleAccountTypes(String[] eligibleAccountTypes) {
            mEligibleAccountTypes = eligibleAccountTypes;
        }
    }

    @Implements(AuthenticatorHelper.class)
    public static class ShadowAuthenticatorHelper {

        boolean preloadDrawableForType = false;

        @Implementation
        public void listenToAccountUpdates() {
        }

        @Implementation
        public void onAccountsUpdated(Account[] accounts) {

        }
        @Implementation
        public void preloadDrawableForType(final Context context, final String accountType) {
            preloadDrawableForType = true;
        }

        @Implementation
        protected Drawable getDrawableForType(Context context, final String accountType) {
            if (preloadDrawableForType) {
                return context.getPackageManager().getDefaultActivityIcon();
            }
            return null;
        }

        @Implementation
        protected CharSequence getLabelForType(Context context, final String accountType) {
            if (TextUtils.equals(accountType, "com.google")) {
                return "Google";
            } else if (TextUtils.equals(accountType, "com.samsung")) {
                return "Samsung";
            } else if (TextUtils.equals(accountType, "com.outlook")) {
                return "Outlook";
            }
            return null;
        }
    }
}
