
/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.provider.Settings.ACTION_ADD_ACCOUNT;
import static android.provider.Settings.EXTRA_ACCOUNT_TYPES;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.ContactsContract.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accounts.AddAccountSettings;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings page for contacts default account
 */
@SearchIndexable
public class ContactsStorageSettings extends DashboardFragment
        implements SelectorWithWidgetPreference.OnClickListener, OnPreferenceClickListener {
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.contacts_storage_settings);
    private static final String TAG = "ContactsStorageSettings";
    private static final String PREF_KEY_ADD_ACCOUNT = "add_account";
    private static final String PREF_KEY_DEVICE_ONLY = "device_only_account_preference";
    private final Map<String, Account> mAccountMap = new HashMap<>();
    private AuthenticatorHelper mAuthenticatorHelper;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mAuthenticatorHelper = new AuthenticatorHelper(context,
                new UserHandle(UserHandle.myUserId()), null);
    }

    @UiThread
    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference selectedPref) {
        final String selectedPreferenceKey = selectedPref.getKey();
        // Check if current provider is different from the selected provider.
        for (String preferenceKey : mAccountMap.keySet()) {
            if (selectedPreferenceKey.equals(preferenceKey)) {
                selectedPref.setChecked(true);
                //TODO: Call DefaultAccount.setDefaultAccountForNewContacts once
                // the implementation is ready.
                Settings.setDefaultAccount(getContentResolver(), mAccountMap.get(preferenceKey));
            } else {
                SelectorWithWidgetPreference unSelectedPreference =
                        getPreferenceScreen().findPreference(preferenceKey);
                if (unSelectedPreference != null) {
                    unSelectedPreference.setChecked(false);
                }
            }
        }
    }

    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (PREF_KEY_ADD_ACCOUNT.equals(preference.getKey())) {
            Resources resources = Resources.getSystem();
            String[] accountTypesArray =
                    resources.getStringArray(
                            com.android.internal.R.array.config_rawContactsEligibleDefaultAccountTypes);
            Intent intent = new Intent(ACTION_ADD_ACCOUNT);
            intent.setClass(getContext(), AddAccountSettings.class);
            intent.putExtra(EXTRA_ACCOUNT_TYPES, accountTypesArray);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public void onCreatePreferences(@NonNull Bundle savedInstanceState,
        @NonNull String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        refreshUI();
    }

    @UiThread
    void refreshUI() {
        // Clear all the accounts stored in the map and later on re-fetch the eligible accounts
        // when creating eligible account preferences.
        mAccountMap.clear();
        final PreferenceScreen screen = getPreferenceScreen();
        AccountManager accountManager = AccountManager.get(getPrefContext());
        //TODO: Call DefaultAccount.getDefaultAccountForNewContacts once
        // implementation is ready.
        Account[] accounts = accountManager.getAccounts();

        for (int i = 0; i < accounts.length; i++) {
            screen.addPreference(buildAccountPreference(accounts[i], i));
        }
        screen.addPreference(buildAddAccountPreference(accounts.length == 0));
        setupDeviceOnlyPreference();

        //TODO: Call DefaultAccount.ListEligibleCloudAccounts once the
        // implementation is ready. And differentiate device only account vs account not set case.
        Account currentDefaultAccount = Settings.getDefaultAccount(getContentResolver());
        String preferenceKey = currentDefaultAccount != null ?
                String.valueOf(currentDefaultAccount.hashCode()) : PREF_KEY_DEVICE_ONLY;
        SelectorWithWidgetPreference preference = getPreferenceScreen().findPreference(
                preferenceKey);
        if (preference != null) {
            preference.setChecked(true);
        }
    }

    private void setupDeviceOnlyPreference() {
        SelectorWithWidgetPreference preference = findPreference(PREF_KEY_DEVICE_ONLY);
        if (preference != null) {
            preference.setOnClickListener(this);
            mAccountMap.put(PREF_KEY_DEVICE_ONLY, null);
        }
    }

    //TODO: Add preference category on account preferences.
    private Preference buildAccountPreference(Account account, int order) {
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(
                getPrefContext());
        preference.setTitle(mAuthenticatorHelper.getLabelForType(getPrefContext(), account.type));
        preference.setIcon(mAuthenticatorHelper.getDrawableForType(getPrefContext(), account.type));
        preference.setSummary(account.name);
        preference.setKey(String.valueOf(account.hashCode()));
        preference.setOnClickListener(this);
        preference.setOrder(order);
        mAccountMap.put(String.valueOf(account.hashCode()), account);
        return preference;
    }

    private RestrictedPreference buildAddAccountPreference(boolean noAccountBeenAdded) {
        RestrictedPreference preference = new RestrictedPreference(getPrefContext());
        preference.setKey(PREF_KEY_ADD_ACCOUNT);
        if (noAccountBeenAdded) {
            preference.setTitle(R.string.contacts_storage_first_time_add_account_message);
        } else {
            preference.setTitle(R.string.add_account_label);
        }
        preference.setIcon(R.drawable.ic_add_24dp);
        preference.setOnPreferenceClickListener(this);
        preference.setOrder(998);
        return preference;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.contacts_storage_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONTACTS_STORAGE;
    }
}
