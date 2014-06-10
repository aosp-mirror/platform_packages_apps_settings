/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Settings screen for the account types on the device.
 * This shows all account types available for personal and work profiles.
 */
public class AccountSettings extends SettingsPreferenceFragment
        implements OnAccountsUpdateListener, OnPreferenceClickListener {
    public static final String TAG = "AccountSettings";

    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_ADD_ACCOUNT = "add_account";

    private static final String KEY_CATEGORY_PERSONAL = "account_personal";
    private static final String KEY_ADD_ACCOUNT_PERSONAL = "add_account_personal";
    private static final String KEY_CATEGORY_WORK = "account_work";

    private AuthenticatorHelper mAuthenticatorHelper;
    private boolean mListeningToAccountUpdates;

    private PreferenceGroup mAccountTypesForUser;
    private Preference mAddAccountForUser;

    private UserManager mUm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);

        mAuthenticatorHelper = new AuthenticatorHelper();
        mAuthenticatorHelper.updateAuthDescriptions(getActivity());
        mAuthenticatorHelper.onAccountsUpdated(getActivity(), null);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_settings);

        if(mUm.isLinkedUser()) {
            // Restricted user or similar
            // TODO: Do we disallow modifying accounts for restricted profiles?
            mAccountTypesForUser = (PreferenceGroup) findPreference(KEY_ACCOUNT);
            if (mUm.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS)) {
                removePreference(KEY_ADD_ACCOUNT);
            } else {
                mAddAccountForUser = findPreference(KEY_ADD_ACCOUNT);
                mAddAccountForUser.setOnPreferenceClickListener(this);
            }
            removePreference(KEY_CATEGORY_PERSONAL);
            removePreference(KEY_CATEGORY_WORK);
        } else {
            mAccountTypesForUser = (PreferenceGroup) findPreference(KEY_CATEGORY_PERSONAL);
            mAddAccountForUser = findPreference(KEY_ADD_ACCOUNT_PERSONAL);
            mAddAccountForUser.setOnPreferenceClickListener(this);

            // TODO: Show the work accounts also
            // TODO: Handle the case where there is only one account
            removePreference(KEY_CATEGORY_WORK);
            removePreference(KEY_ADD_ACCOUNT);
        }
        updateAccountTypes(mAccountTypesForUser);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopListeningToAccountUpdates();
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        // TODO: watch for package upgrades to invalidate cache; see 7206643
        mAuthenticatorHelper.updateAuthDescriptions(getActivity());
        mAuthenticatorHelper.onAccountsUpdated(getActivity(), accounts);
        listenToAccountUpdates();
        updateAccountTypes(mAccountTypesForUser);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // Check the preference
        if (preference == mAddAccountForUser) {
            Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void updateAccountTypes(PreferenceGroup preferenceGroup) {
        preferenceGroup.removeAll();
        preferenceGroup.setOrderingAsAdded(true);
        for (AccountPreference preference : getAccountTypePreferences()) {
            preferenceGroup.addPreference(preference);
        }
        if (mAddAccountForUser != null) {
            preferenceGroup.addPreference(mAddAccountForUser);
        }
    }

    private List<AccountPreference> getAccountTypePreferences() {
        String[] accountTypes = mAuthenticatorHelper.getEnabledAccountTypes();
        List<AccountPreference> accountTypePreferences =
                new ArrayList<AccountPreference>(accountTypes.length);
        for (String accountType : accountTypes) {
            CharSequence label = mAuthenticatorHelper.getLabelForType(getActivity(), accountType);
            if (label == null) {
                continue;
            }

            Account[] accounts = AccountManager.get(getActivity()).getAccountsByType(accountType);
            boolean skipToAccount = accounts.length == 1
                    && !mAuthenticatorHelper.hasAccountPreferences(accountType);

            if (skipToAccount) {
                Bundle fragmentArguments = new Bundle();
                fragmentArguments.putParcelable(AccountSyncSettings.ACCOUNT_KEY,
                        accounts[0]);

                accountTypePreferences.add(new AccountPreference(
                        getActivity(),
                        label,
                        accountType,
                        AccountSyncSettings.class.getName(),
                        fragmentArguments));
            } else {
                Bundle fragmentArguments = new Bundle();
                fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_LABEL,
                        label.toString());

                accountTypePreferences.add(new AccountPreference(
                        getActivity(),
                        label,
                        accountType,
                        ManageAccountsSettings.class.getName(),
                        fragmentArguments));
            }
            mAuthenticatorHelper.preloadDrawableForType(getActivity(), accountType);
        }
        // Sort by label
        Collections.sort(accountTypePreferences, new Comparator<AccountPreference>() {
            @Override
            public int compare(AccountPreference t1, AccountPreference t2) {
                return t1.mTitle.toString().compareTo(t2.mTitle.toString());
            }
        });
        return accountTypePreferences;
    }

    private void listenToAccountUpdates() {
        if (!mListeningToAccountUpdates) {
            AccountManager.get(getActivity()).addOnAccountsUpdatedListener(this, null, true);
            mListeningToAccountUpdates = true;
        }
    }

    private void stopListeningToAccountUpdates() {
        if (mListeningToAccountUpdates) {
            AccountManager.get(getActivity()).removeOnAccountsUpdatedListener(this);
            mListeningToAccountUpdates = false;
        }
    }

    private class AccountPreference extends Preference implements OnPreferenceClickListener {
        /**
         * Title of the tile that is shown to the user.
         * @attr ref android.R.styleable#PreferenceHeader_title
         */
        private final CharSequence mTitle;

        /**
         * Full class name of the fragment to display when this tile is
         * selected.
         * @attr ref android.R.styleable#PreferenceHeader_fragment
         */
        private final String mFragment;

        /**
         * Optional arguments to supply to the fragment when it is
         * instantiated.
         */
        private final Bundle mFragmentArguments;


        public AccountPreference(Context context, CharSequence title,
                String accountType, String fragment, Bundle fragmentArguments) {
            super(context);
            mTitle = title;
            mFragment = fragment;
            mFragmentArguments = fragmentArguments;
            setWidgetLayoutResource(R.layout.account_type_preference);

            Drawable drawable = mAuthenticatorHelper.getDrawableForType(context, accountType);
            setTitle(title);
            setIcon(drawable);

            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (mFragment != null) {
                Utils.startWithFragment(
                        getContext(), mFragment, mFragmentArguments, null, 0, 0, mTitle);
                return true;
            }
            return false;
        }
    }
    // TODO Implement a {@link SearchIndexProvider} to allow Indexing and Search of account types
    // See http://b/15403806
}
