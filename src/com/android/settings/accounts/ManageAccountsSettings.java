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
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.AccountPreference;
import com.android.settings.DialogCreatable;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.HashSet;

public class ManageAccountsSettings extends AccountPreferenceBase
        implements OnAccountsUpdateListener, DialogCreatable {

    private static final int MENU_ADD_ACCOUNT = Menu.FIRST;

    private static final int REQUEST_SHOW_SYNC_SETTINGS = 1;

    private String[] mAuthorities;
    private TextView mErrorInfoView;

    private SettingsDialogFragment mDialogFragment;
    private Switch mAutoSyncSwitch;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.manage_accounts_settings);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        Activity activity = getActivity();
        AccountManager.get(activity).addOnAccountsUpdatedListener(this, null, true);
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(mAutoSyncSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.RIGHT));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.manage_accounts_screen, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        final View view = getView();

        mErrorInfoView = (TextView)view.findViewById(R.id.sync_settings_error_info);
        mErrorInfoView.setVisibility(View.GONE);

        mAutoSyncSwitch = new Switch(activity);

        // TODO Where to put the switch in tablet multipane layout?
        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mAutoSyncSwitch.setPadding(0, 0, padding, 0);
        mAutoSyncSwitch.setChecked(ContentResolver.getMasterSyncAutomatically());
        mAutoSyncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ContentResolver.setMasterSyncAutomatically(isChecked);
                onSyncStateUpdated();
            }
        });

        mAuthorities = activity.getIntent().getStringArrayExtra(AUTHORITIES_FILTER_KEY);

        updateAuthDescriptions();
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        AccountManager.get(activity).removeOnAccountsUpdatedListener(this);
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);
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
        Bundle args = new Bundle();
        args.putParcelable(AccountSyncSettings.ACCOUNT_KEY, acctPref.getAccount());
        ((PreferenceActivity) getActivity()).startPreferencePanel(
                AccountSyncSettings.class.getCanonicalName(), args,
                R.string.account_sync_settings_title, acctPref.getAccount().name,
                this, REQUEST_SHOW_SYNC_SETTINGS);
    }

    @Override
    public void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = new SettingsDialogFragment(this, dialogId);
        mDialogFragment.show(getActivity().getFragmentManager(), Integer.toString(dialogId));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem addAccountItem = menu.add(0, MENU_ADD_ACCOUNT, 0, R.string.add_account_label);
        addAccountItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_ADD_ACCOUNT) {
            onAddAccountClicked();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSyncStateUpdated() {
        // Catch any delayed delivery of update messages
        if (getActivity() == null) return;
        // Set background connection state
        if (mAutoSyncSwitch != null) {
            mAutoSyncSwitch.setChecked(ContentResolver.getMasterSyncAutomatically());
        }

        // iterate over all the preferences, setting the state properly for each
        SyncInfo currentSync = ContentResolver.getCurrentSync();

        boolean anySyncFailed = false; // true if sync on any account failed

        // only track userfacing sync adapters when deciding if account is synced or not
        final SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypes();
        HashSet<String> userFacing = new HashSet<String>();
        for (int k = 0, n = syncAdapters.length; k < n; k++) {
            final SyncAdapterType sa = syncAdapters[k];
            if (sa.isUserVisible()) {
                userFacing.add(sa.authority);
            }
        }
        for (int i = 0, count = getPreferenceScreen().getPreferenceCount(); i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (! (pref instanceof AccountPreference)) {
                continue;
            }

            AccountPreference accountPref = (AccountPreference) pref;
            Account account = accountPref.getAccount();
            int syncCount = 0;
            boolean syncIsFailing = false;
            final ArrayList<String> authorities = accountPref.getAuthorities();
            if (authorities != null) {
                for (String authority : authorities) {
                    SyncStatusInfo status = ContentResolver.getSyncStatus(account, authority);
                    boolean syncEnabled = ContentResolver.getSyncAutomatically(account, authority)
                            && ContentResolver.getMasterSyncAutomatically()
                            && (ContentResolver.getIsSyncable(account, authority) > 0);
                    boolean authorityIsPending = ContentResolver.isSyncPending(account, authority);
                    boolean activelySyncing = currentSync != null
                            && currentSync.authority.equals(authority)
                            && new Account(currentSync.account.name, currentSync.account.type)
                                    .equals(account);
                    boolean lastSyncFailed = status != null
                            && syncEnabled
                            && status.lastFailureTime != 0
                            && status.getLastFailureMesgAsInt(0)
                               != ContentResolver.SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS;
                    if (lastSyncFailed && !activelySyncing && !authorityIsPending) {
                        syncIsFailing = true;
                        anySyncFailed = true;
                    }
                    syncCount += syncEnabled && userFacing.contains(authority) ? 1 : 0;
                }
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "no syncadapters found for " + account);
                }
            }
            int syncStatus = AccountPreference.SYNC_DISABLED;
            if (syncIsFailing) {
                syncStatus = AccountPreference.SYNC_ERROR;
            } else if (syncCount == 0) {
                syncStatus = AccountPreference.SYNC_DISABLED;
            } else if (syncCount > 0) {
                syncStatus = AccountPreference.SYNC_ENABLED;
            }
            accountPref.setSyncStatus(syncStatus);
        }

        mErrorInfoView.setVisibility(anySyncFailed ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        if (getActivity() == null) return;
        getPreferenceScreen().removeAll();
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
                        new AccountPreference(getActivity(), account, icon, auths);
                getPreferenceScreen().addPreference(preference);
            }
        }
        onSyncStateUpdated();
    }

    @Override
    protected void onAuthDescriptionsUpdated() {
        // Update account icons for all account preference items
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            AccountPreference pref = (AccountPreference) getPreferenceScreen().getPreference(i);
            pref.setProviderIcon(getDrawableForType(pref.getAccount().type));
            pref.setSummary(getLabelForType(pref.getAccount().type));
        }
    }

    public void onAddAccountClicked() {
        Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
        intent.putExtra(AUTHORITIES_FILTER_KEY, mAuthorities);
        startActivity(intent);
    }
}
