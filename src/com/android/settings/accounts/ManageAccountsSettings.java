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
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.AccountPreference;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class ManageAccountsSettings extends AccountPreferenceBase
        implements OnAccountsUpdateListener {

    private static final String ACCOUNT_KEY = "account"; // to pass to auth settings
    public static final String KEY_ACCOUNT_TYPE = "account_type";
    public static final String KEY_ACCOUNT_LABEL = "account_label";

    private static final int MENU_SYNC_NOW_ID = Menu.FIRST;
    private static final int MENU_SYNC_CANCEL_ID    = Menu.FIRST + 1;

    private static final int REQUEST_SHOW_SYNC_SETTINGS = 1;

    private String[] mAuthorities;
    private TextView mErrorInfoView;

    private SettingsDialogFragment mDialogFragment;
    // If an account type is set, then show only accounts of that type
    private String mAccountType;
    // Temporary hack, to deal with backward compatibility 
    private Account mFirstAccount;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Bundle args = getArguments();
        if (args != null && args.containsKey(KEY_ACCOUNT_TYPE)) {
            mAccountType = args.getString(KEY_ACCOUNT_TYPE);
        }
        addPreferencesFromResource(R.xml.manage_accounts_settings);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        Activity activity = getActivity();
        AccountManager.get(activity).addOnAccountsUpdatedListener(this, null, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.manage_accounts_screen, container, false);
        final ListView list = (ListView) view.findViewById(android.R.id.list);
        Utils.prepareCustomPreferencesList(container, view, list, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        final View view = getView();

        mErrorInfoView = (TextView)view.findViewById(R.id.sync_settings_error_info);
        mErrorInfoView.setVisibility(View.GONE);

        mAuthorities = activity.getIntent().getStringArrayExtra(AUTHORITIES_FILTER_KEY);

        Bundle args = getArguments();
        if (args != null && args.containsKey(KEY_ACCOUNT_LABEL)) {
            getActivity().setTitle(args.getString(KEY_ACCOUNT_LABEL));
        }
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem syncNow = menu.add(0, MENU_SYNC_NOW_ID, 0,
                getString(R.string.sync_menu_sync_now))
                .setIcon(R.drawable.ic_menu_refresh_holo_dark);
        MenuItem syncCancel = menu.add(0, MENU_SYNC_CANCEL_ID, 0,
                getString(R.string.sync_menu_sync_cancel))
                .setIcon(com.android.internal.R.drawable.ic_menu_close_clear_cancel);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean syncActive = ContentResolver.getCurrentSync() != null;
        menu.findItem(MENU_SYNC_NOW_ID).setVisible(!syncActive && mFirstAccount != null);
        menu.findItem(MENU_SYNC_CANCEL_ID).setVisible(syncActive && mFirstAccount != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SYNC_NOW_ID:
            requestOrCancelSyncForAccounts(true);
            return true;
        case MENU_SYNC_CANCEL_ID:
            requestOrCancelSyncForAccounts(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestOrCancelSyncForAccounts(boolean sync) {
        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypes();
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        int count = getPreferenceScreen().getPreferenceCount();
        // For each account
        for (int i = 0; i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (pref instanceof AccountPreference) {
                Account account = ((AccountPreference) pref).getAccount();
                // For all available sync authorities, sync those that are enabled for the account
                for (int j = 0; j < syncAdapters.length; j++) {
                    SyncAdapterType sa = syncAdapters[j];
                    if (syncAdapters[j].accountType.equals(mAccountType)
                            && ContentResolver.getSyncAutomatically(account, sa.authority)) {
                        if (sync) {
                            ContentResolver.requestSync(account, sa.authority, extras);
                        } else {
                            ContentResolver.cancelSync(account, sa.authority);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onSyncStateUpdated() {
        // Catch any delayed delivery of update messages
        if (getActivity() == null) return;

        // iterate over all the preferences, setting the state properly for each
        SyncInfo currentSync = ContentResolver.getCurrentSync();

        boolean anySyncFailed = false; // true if sync on any account failed
        Date date = new Date();

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
            long lastSuccessTime = 0;
            boolean syncIsFailing = false;
            final ArrayList<String> authorities = accountPref.getAuthorities();
            boolean syncingNow = false;
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
                    syncingNow |= activelySyncing;
                    if (status != null && lastSuccessTime < status.lastSuccessTime) {
                        lastSuccessTime = status.lastSuccessTime;
                    }
                    syncCount += syncEnabled && userFacing.contains(authority) ? 1 : 0;
                }
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "no syncadapters found for " + account);
                }
            }
            if (syncIsFailing) {
                accountPref.setSyncStatus(AccountPreference.SYNC_ERROR, true);
            } else if (syncCount == 0) {
                accountPref.setSyncStatus(AccountPreference.SYNC_DISABLED, true);
            } else if (syncCount > 0) {
                if (syncingNow) {
                    accountPref.setSyncStatus(AccountPreference.SYNC_IN_PROGRESS, true);
                } else {
                    accountPref.setSyncStatus(AccountPreference.SYNC_ENABLED, true);
                    if (lastSuccessTime > 0) {
                        accountPref.setSyncStatus(AccountPreference.SYNC_ENABLED, false);
                        date.setTime(lastSuccessTime);
                        final String timeString = formatSyncDate(date);
                        accountPref.setSummary(getResources().getString(
                                R.string.last_synced, timeString));
                    }
                }
            } else {
                accountPref.setSyncStatus(AccountPreference.SYNC_DISABLED, true);
            }
        }

        mErrorInfoView.setVisibility(anySyncFailed ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        if (getActivity() == null) return;
        getPreferenceScreen().removeAll();
        mFirstAccount = null;
        addPreferencesFromResource(R.xml.manage_accounts_settings);
        for (int i = 0, n = accounts.length; i < n; i++) {
            final Account account = accounts[i];
            // If an account type is specified for this screen, skip other types
            if (mAccountType != null && !account.type.equals(mAccountType)) continue;
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
                        new AccountPreference(getActivity(), account, icon, auths, false);
                getPreferenceScreen().addPreference(preference);
                if (mFirstAccount == null) {
                    mFirstAccount = account;
                    getActivity().invalidateOptionsMenu();
                }
            }
        }
        if (mAccountType != null && mFirstAccount != null) {
            addAuthenticatorSettings();
        } else {
            // There's no account, reset to top-level of settings
            Intent settingsTop = new Intent(android.provider.Settings.ACTION_SETTINGS);
            settingsTop.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getActivity().startActivity(settingsTop);
        }
        onSyncStateUpdated();
    }

    private void addAuthenticatorSettings() {
        PreferenceScreen prefs = addPreferencesForType(mAccountType, getPreferenceScreen());
        if (prefs != null) {
            updatePreferenceIntents(prefs);
        }
    }

    private void updatePreferenceIntents(PreferenceScreen prefs) {
        PackageManager pm = getActivity().getPackageManager();
        for (int i = 0; i < prefs.getPreferenceCount();) {
            Intent intent = prefs.getPreference(i).getIntent();
            if (intent != null) {
                ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (ri == null) {
                    prefs.removePreference(prefs.getPreference(i));
                    continue;
                } else {
                    intent.putExtra(ACCOUNT_KEY, mFirstAccount);
                    intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                }
            }
            i++;
        }
    }

    @Override
    protected void onAuthDescriptionsUpdated() {
        // Update account icons for all account preference items
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (pref instanceof AccountPreference) {
                AccountPreference accPref = (AccountPreference) pref;
                accPref.setSummary(getLabelForType(accPref.getAccount().type));
            }
        }
    }
}
