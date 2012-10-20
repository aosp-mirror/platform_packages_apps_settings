/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.AccountPreference;
import com.android.settings.DialogCreatable;
import com.android.settings.R;

import java.util.ArrayList;

public class SyncSettings extends AccountPreferenceBase
        implements OnAccountsUpdateListener, DialogCreatable {

    private static final String KEY_SYNC_SWITCH = "sync_switch";

    private String[] mAuthorities;

    private SettingsDialogFragment mDialogFragment;
    private CheckBoxPreference mAutoSyncPreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.sync_settings);
        mAutoSyncPreference =
                (CheckBoxPreference) getPreferenceScreen().findPreference(KEY_SYNC_SWITCH);
        mAutoSyncPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ActivityManager.isUserAMonkey()) {
                    Log.d("SyncSettings", "ignoring monkey's attempt to flip sync state");
                } else {
                    ContentResolver.setMasterSyncAutomatically((Boolean) newValue);
                }
                return true;
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        Activity activity = getActivity();
        AccountManager.get(activity).addOnAccountsUpdatedListener(this, null, true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mAutoSyncPreference.setChecked(ContentResolver.getMasterSyncAutomatically());
        mAuthorities = activity.getIntent().getStringArrayExtra(AUTHORITIES_FILTER_KEY);

        updateAuthDescriptions();
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        AccountManager.get(activity).removeOnAccountsUpdatedListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferences, Preference preference) {
        if (preference instanceof AccountPreference) {
            startAccountSettings((AccountPreference) preference);
        } else {
            return false;
        }
        return true;
    }

    private void startAccountSettings(AccountPreference acctPref) {
        Intent intent = new Intent("android.settings.ACCOUNT_SYNC_SETTINGS");
        intent.putExtra(AccountSyncSettings.ACCOUNT_KEY, acctPref.getAccount());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = new SettingsDialogFragment(this, dialogId);
        mDialogFragment.show(getActivity().getFragmentManager(), Integer.toString(dialogId));
    }

    private void removeAccountPreferences() {
        PreferenceScreen parent = getPreferenceScreen();
        for (int i = 0; i < parent.getPreferenceCount(); ) {
            if (parent.getPreference(i) instanceof AccountPreference) {
                parent.removePreference(parent.getPreference(i));
            } else {
                i++;
            }
        }
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        if (getActivity() == null) return;

        removeAccountPreferences();
        for (int i = 0, n = accounts.length; i < n; i++) {
            final Account account = accounts[i];
            final ArrayList<String> auths = getAuthoritiesForAccountType(account.type);

            boolean showAccount = true;
            if (mAuthorities != null && auths != null) {
                showAccount = false;
                for (String requestedAuthority : mAuthorities) {
                    if (auths.contains(requestedAuthority)) {
                        showAccount = true;
                        break;
                    }
                }
            }

            if (showAccount) {
                final Drawable icon = getDrawableForType(account.type);
                final AccountPreference preference =
                        new AccountPreference(getActivity(), account, icon, auths, true);
                getPreferenceScreen().addPreference(preference);
                preference.setSummary(getLabelForType(account.type));
            }
        }
        onSyncStateUpdated();
    }

    @Override
    protected void onAuthDescriptionsUpdated() {
        // Update account icons for all account preference items
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (pref instanceof AccountPreference) {
                AccountPreference accPref = (AccountPreference)
                        getPreferenceScreen().getPreference(i);
                accPref.setIcon(getDrawableForType(accPref.getAccount().type));
                accPref.setSummary(getLabelForType(accPref.getAccount().type));
            }
        }
    }
}
