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

package com.android.settings.bluetooth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.util.Log;

import com.android.settings.accounts.AuthenticatorHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

public class MessageAccessSettings extends SettingsPreferenceFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener, Indexable {
    private static final String TAG = "MessageAccessSettings";
    private static final String GMAIL_PACKAGE_NAME = "com.google.android.gm";
    private static final String EMAIL_PACKAGE_NAME = "com.google.android.email";

    private Account[] mAccounts;
    private UserHandle mUserHandle;
    private PreferenceGroup mAvailableAccounts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserHandle = Utils.getProfileToDisplay(ActivityManagerNative.getDefault(),
                getActivity().getActivityToken(), savedInstanceState);

        addPreferencesFromResource(R.xml.bluetooth_message_access);
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
    }

    @Override
    public void onAccountsUpdate(final UserHandle userHandle) {
        mAccounts = AccountManager.get(getActivity()).getAccountsAsUser(
                mUserHandle.getIdentifier());

        final int mAccountsSize = mAccounts.length;
        for (int i = 0; i < mAccountsSize; ++i){
            Log.d(TAG, String.format("account.type = %s\n", mAccounts[i].type));
        }
    }

    /**
     * Retrieves the email icon for a given account's email preference
     *
     * @param accountPref The user's account to retrieve the icon from.
     *
     * @return The drawable representing the icon of the user's email preference
     **/
    private Drawable getIcon(AccountPreference accountPref){
        Drawable icon = null;

        // Currently only two types of icons are allowed.
        final String packageName = accountPref.account.type.equals("com.google")
                ? GMAIL_PACKAGE_NAME : EMAIL_PACKAGE_NAME;

        try{
            icon = getPackageManager().getApplicationIcon(packageName);
        }catch(NameNotFoundException nnfe){
            icon = null;
        }

        return icon;
    }

    private void initPreferences() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mAvailableAccounts = (PreferenceGroup)preferenceScreen.findPreference("accounts");
        mAccounts = AccountManager.get(getActivity()).getAccountsAsUser(
                mUserHandle.getIdentifier());

        final int mAccountsSize = mAccounts.length;
        for (int i = 0; i < mAccountsSize; ++i){
            AccountPreference accountPref = new AccountPreference(getActivity(), mAccounts[i]);
            Drawable icon = getIcon(accountPref);
            if (icon != null){
                accountPref.setIcon(icon);
            }
            mAvailableAccounts.addPreference(accountPref);
        }
    }

    private class AccountPreference extends SwitchPreference
            implements Preference.OnPreferenceChangeListener{
        private Account account;

        AccountPreference(Context context, Account account){
            super(context);
            this.account = account;
            setTitle(account.type);
            setSummary(account.name);

            setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object val) {
            if (preference instanceof AccountPreference){
                final AccountPreference accountPref = (AccountPreference) preference;

                if (((Boolean)val).booleanValue()){
                    // Enable paired deviced to connect, fill in once API is available
                    Log.w(TAG, String.format(
                                "User has turned on '%s' for Bluetooth message access.",
                                accountPref.account.name));
                } else {
                    // Disable paired deviced to connect, fill in once API is available
                    Log.w(TAG, String.format(
                                "User has turned off '%s' for Bluetooth message access.",
                                accountPref.account.name));
                }
            }
            return true;
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                List<SearchIndexableResource> indexables = new ArrayList<SearchIndexableResource>();
                SearchIndexableResource indexable = new SearchIndexableResource(context);
                indexable.xmlResId = R.xml.bluetooth_message_access;
                indexables.add(indexable);
                return indexables;
            }
        };
}
