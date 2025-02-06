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
import static android.provider.ContactsContract.RawContacts.DefaultAccount;
import static android.provider.Settings.ACTION_ADD_ACCOUNT;
import static android.provider.Settings.EXTRA_ACCOUNT_TYPES;

import android.accounts.Account;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.ContactsContract.RawContacts.DefaultAccount.DefaultAccountAndState;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceGroup;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.accounts.AddAccountSettings;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings page for contacts default account
 */
@SearchIndexable
public class ContactsStorageSettings extends DashboardFragment
        implements SelectorWithWidgetPreference.OnClickListener, OnPreferenceClickListener,
        AuthenticatorHelper.OnAccountsUpdateListener {
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.contacts_storage_settings);
    private static final String TAG = "ContactsStorageSettings";
    private static final String PREF_KEY_ADD_ACCOUNT = "add_account";
    private static final String PREF_KEY_DEVICE_ONLY = "device_only_account_preference";
    private static final String PREF_KEY_ACCOUNT_CATEGORY = "account_category";
    private final Map<String, DefaultAccountAndState> mAccountMap = new HashMap<>();
    private AuthenticatorHelper mAuthenticatorHelper;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mAuthenticatorHelper = new AuthenticatorHelper(context,
                new UserHandle(UserHandle.myUserId()), this);
        mAuthenticatorHelper.listenToAccountUpdates();
        preloadEligibleAccountIcon();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAuthenticatorHelper.stopListeningToAccountUpdates();
    }

    @UiThread
    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference selectedPref) {
        final String selectedPreferenceKey = selectedPref.getKey();
        // Check if current account is different from the selected account.
        for (String preferenceKey : mAccountMap.keySet()) {
            if (selectedPreferenceKey.equals(preferenceKey)) {
                try {
                    DefaultAccountAndState currentDefaultAccount = mAccountMap.get(preferenceKey);
                    DefaultAccount.setDefaultAccountForNewContacts(getContentResolver(),
                            currentDefaultAccount);
                    selectedPref.setChecked(true);
                    if (currentDefaultAccount.getState()
                            == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD) {
                        startMoveLocalAndSimContactsActivity();
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "Error setting the default account " + e);
                    Toast.makeText(getContext(),
                            R.string.contacts_storage_set_default_account_error_message,
                            Toast.LENGTH_SHORT).show();
                }
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
            String[] accountTypesArray = getEligibleAccountTypes();
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
    public void onAccountsUpdate(UserHandle userHandle) {
        preloadEligibleAccountIcon();
        refreshUI();
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
        final PreferenceGroup preferenceGroup = findPreference(PREF_KEY_ACCOUNT_CATEGORY);
        preferenceGroup.removeAll();
        // If the default account is SIM, we should show in the page, otherwise don't show.
        SelectorWithWidgetPreference simAccountPreference = buildSimAccountPreference();
        if (simAccountPreference != null) {
            preferenceGroup.addPreference(simAccountPreference);
        }
        List<Account> accounts = DefaultAccount.getEligibleCloudAccounts(getContentResolver());
        for (int i = 0; i < accounts.size(); i++) {
            preferenceGroup.addPreference(
                    buildCloudAccountPreference(accounts.get(i), /*order=*/i));
        }
        // If there's no eligible account types, the "Add Account" preference should
        // not be shown to the users.
        if (getEligibleAccountTypes().length > 0) {
            preferenceGroup.addPreference(buildAddAccountPreference(accounts.isEmpty()));
        }
        setupDeviceOnlyPreference();
        setDefaultAccountPreference(preferenceGroup);
    }

    private void preloadEligibleAccountIcon() {
        String[] accountTypes = getEligibleAccountTypes();
        for (String accountType : accountTypes) {
            // Preload the drawable for the account type to avoid the latency when rendering the
            // account preference.
            mAuthenticatorHelper.preloadDrawableForType(getContext(), accountType);
        }
    }

    private void setupDeviceOnlyPreference() {
        SelectorWithWidgetPreference preference = findPreference(PREF_KEY_DEVICE_ONLY);
        if (preference != null) {
            preference.setOnClickListener(this);
            mAccountMap.put(PREF_KEY_DEVICE_ONLY, DefaultAccountAndState.ofLocal());
        }
    }

    private void setDefaultAccountPreference(PreferenceGroup preferenceGroup) {
        DefaultAccountAndState currentDefaultAccountAndState =
                DefaultAccount.getDefaultAccountForNewContacts(getContentResolver());
        String preferenceKey = getAccountHashCode(currentDefaultAccountAndState);
        Account currentDefaultAccount = currentDefaultAccountAndState.getAccount();

        // Set the current default account preference to be checked if found among existing
        // preferences. If not, then create a new preference for default account.
        SelectorWithWidgetPreference preference = null;
        if (mAccountMap.containsKey(preferenceKey)) {
            preference = getPreferenceScreen().findPreference(preferenceKey);
        } else if (preferenceKey != null && currentDefaultAccount != null) {
            preference = buildCloudAccountPreference(currentDefaultAccount, mAccountMap.size());
            preferenceGroup.addPreference(preference);
        }
        if (preference != null) {
            preference.setChecked(true);
        }
    }

    private SelectorWithWidgetPreference buildCloudAccountPreference(Account account, int order) {
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(
                getPrefContext());
        DefaultAccountAndState accountAndState = DefaultAccountAndState.ofCloud(account);
        String preferenceKey = getAccountHashCode(accountAndState);
        String accountPreferenceTitle = getString(R.string.contacts_storage_account_title,
                mAuthenticatorHelper.getLabelForType(getPrefContext(), account.type));
        preference.setTitle(accountPreferenceTitle);
        preference.setIcon(mAuthenticatorHelper.getDrawableForType(getPrefContext(), account.type));
        preference.setSummary(account.name);
        preference.setKey(preferenceKey);
        preference.setOnClickListener(this);
        preference.setOrder(order);
        mAccountMap.put(preferenceKey, accountAndState);
        return preference;
    }

    @Nullable
    private SelectorWithWidgetPreference buildSimAccountPreference() {
        DefaultAccountAndState currentDefaultAccountAndState =
                DefaultAccount.getDefaultAccountForNewContacts(getContentResolver());
        if (currentDefaultAccountAndState.getState()
                == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_SIM) {
            String preferenceKey = getAccountHashCode(currentDefaultAccountAndState);
            SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(
                    getPrefContext());
            preference.setTitle(R.string.sim_card_label);
            preference.setIcon(R.drawable.ic_sim_card);
            preference.setSummary(R.string.contacts_storage_device_only_preference_summary);
            preference.setKey(preferenceKey);
            preference.setOnClickListener(this);
            mAccountMap.put(preferenceKey, currentDefaultAccountAndState);
            return preference;
        }
        return null;
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

    private void startMoveLocalAndSimContactsActivity() {
        Intent intent = new Intent()
                .setAction(DefaultAccount.ACTION_MOVE_CONTACTS_TO_DEFAULT_ACCOUNT)
                .setPackage("com.android.providers.contacts")
                .addFlags(FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
    }

    @Nullable
    private String getAccountHashCode(
            DefaultAccountAndState currentDefaultAccountAndState) {
        Account currentDefaultAccount = currentDefaultAccountAndState.getAccount();
        if (currentDefaultAccount != null && (currentDefaultAccountAndState.getState()
                == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD
                || currentDefaultAccountAndState.getState()
                == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_SIM)) {
            return String.valueOf(currentDefaultAccount.hashCode());
        } else if (currentDefaultAccountAndState.getState()
                == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_LOCAL) {
            return PREF_KEY_DEVICE_ONLY;
        } else {
            // If the account is not set or in error state, it should just return null and won't
            // set the checked status in radio button.
            return null;
        }
    }

    @VisibleForTesting
    String[] getEligibleAccountTypes() {
        return Resources.getSystem().getStringArray(
                com.android.internal.R.array.config_rawContactsEligibleDefaultAccountTypes);
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
