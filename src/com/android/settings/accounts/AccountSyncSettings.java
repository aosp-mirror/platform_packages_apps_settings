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
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.content.pm.ProviderInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AccountSyncSettings extends AccountPreferenceBase {

    public static final String ACCOUNT_KEY = "account";
    private static final int MENU_SYNC_NOW_ID       = Menu.FIRST;
    private static final int MENU_SYNC_CANCEL_ID    = Menu.FIRST + 1;
    private static final int MENU_REMOVE_ACCOUNT_ID = Menu.FIRST + 2;
    private static final int REALLY_REMOVE_DIALOG = 100;
    private static final int FAILED_REMOVAL_DIALOG = 101;
    private static final int CANT_DO_ONETIME_SYNC_DIALOG = 102;
    private TextView mUserId;
    private TextView mProviderId;
    private ImageView mProviderIcon;
    private TextView mErrorInfoView;
    private Account mAccount;
    // List of all accounts, updated when accounts are added/removed
    // We need to re-scan the accounts on sync events, in case sync state changes.
    private Account[] mAccounts;
    private ArrayList<SyncStateCheckBoxPreference> mCheckBoxes =
                new ArrayList<SyncStateCheckBoxPreference>();
    private ArrayList<SyncAdapterType> mInvisibleAdapters = Lists.newArrayList();

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog dialog = null;
        if (id == REALLY_REMOVE_DIALOG) {
            dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.really_remove_account_title)
                .setMessage(R.string.really_remove_account_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.remove_account_label,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AccountManager.get(AccountSyncSettings.this.getActivity())
                                .removeAccount(mAccount,
                                new AccountManagerCallback<Boolean>() {
                            @Override
                            public void run(AccountManagerFuture<Boolean> future) {
                                // If already out of this screen, don't proceed.
                                if (!AccountSyncSettings.this.isResumed()) {
                                    return;
                                }
                                boolean failed = true;
                                try {
                                    if (future.getResult() == true) {
                                        failed = false;
                                    }
                                } catch (OperationCanceledException e) {
                                    // handled below
                                } catch (IOException e) {
                                    // handled below
                                } catch (AuthenticatorException e) {
                                    // handled below
                                }
                                if (failed && getActivity() != null &&
                                        !getActivity().isFinishing()) {
                                    showDialog(FAILED_REMOVAL_DIALOG);
                                } else {
                                    finish();
                                }
                            }
                        }, null);
                    }
                })
                .create();
        } else if (id == FAILED_REMOVAL_DIALOG) {
            dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.really_remove_account_title)
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(R.string.remove_account_failed)
                .create();
        } else if (id == CANT_DO_ONETIME_SYNC_DIALOG) {
            dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.cant_sync_dialog_title)
                .setMessage(R.string.cant_sync_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        }
        return dialog;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.account_sync_screen, container, false);

        final ListView list = (ListView) view.findViewById(android.R.id.list);
        Utils.prepareCustomPreferencesList(container, view, list, false);

        initializeUi(view);

        return view;
    }

    protected void initializeUi(final View rootView) {
        addPreferencesFromResource(R.xml.account_sync_settings);

        mErrorInfoView = (TextView) rootView.findViewById(R.id.sync_settings_error_info);
        mErrorInfoView.setVisibility(View.GONE);

        mUserId = (TextView) rootView.findViewById(R.id.user_id);
        mProviderId = (TextView) rootView.findViewById(R.id.provider_id);
        mProviderIcon = (ImageView) rootView.findViewById(R.id.provider_icon);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments == null) {
            Log.e(TAG, "No arguments provided when starting intent. ACCOUNT_KEY needed.");
            return;
        }

        mAccount = (Account) arguments.getParcelable(ACCOUNT_KEY);
        if (mAccount != null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "Got account: " + mAccount);
            mUserId.setText(mAccount.name);
            mProviderId.setText(mAccount.type);
        }
    }

    @Override
    public void onResume() {
        final Activity activity = getActivity();
        AccountManager.get(activity).addOnAccountsUpdatedListener(this, null, false);
        updateAuthDescriptions();
        onAccountsUpdated(AccountManager.get(activity).getAccounts());

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        AccountManager.get(getActivity()).removeOnAccountsUpdatedListener(this);
    }

    private void addSyncStateCheckBox(Account account, String authority) {
        SyncStateCheckBoxPreference item =
                new SyncStateCheckBoxPreference(getActivity(), account, authority);
        item.setPersistent(false);
        final ProviderInfo providerInfo = getPackageManager().resolveContentProvider(authority, 0);
        if (providerInfo == null) {
            return;
        }
        CharSequence providerLabel = providerInfo.loadLabel(getPackageManager());
        if (TextUtils.isEmpty(providerLabel)) {
            Log.e(TAG, "Provider needs a label for authority '" + authority + "'");
            return;
        }
        String title = getString(R.string.sync_item_title, providerLabel);
        item.setTitle(title);
        item.setKey(authority);
        mCheckBoxes.add(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        MenuItem syncNow = menu.add(0, MENU_SYNC_NOW_ID, 0,
                getString(R.string.sync_menu_sync_now))
                .setIcon(R.drawable.ic_menu_refresh_holo_dark);
        MenuItem syncCancel = menu.add(0, MENU_SYNC_CANCEL_ID, 0,
                getString(R.string.sync_menu_sync_cancel))
                .setIcon(com.android.internal.R.drawable.ic_menu_close_clear_cancel);

        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        if (!um.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS)) {
            MenuItem removeAccount = menu.add(0, MENU_REMOVE_ACCOUNT_ID, 0,
                    getString(R.string.remove_account_label))
                    .setIcon(R.drawable.ic_menu_delete_holo_dark);
            removeAccount.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER |
                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        syncNow.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        syncCancel.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean syncActive = ContentResolver.getCurrentSync() != null;
        menu.findItem(MENU_SYNC_NOW_ID).setVisible(!syncActive);
        menu.findItem(MENU_SYNC_CANCEL_ID).setVisible(syncActive);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SYNC_NOW_ID:
                startSyncForEnabledProviders();
                return true;
            case MENU_SYNC_CANCEL_ID:
                cancelSyncForEnabledProviders();
                return true;
            case MENU_REMOVE_ACCOUNT_ID:
                showDialog(REALLY_REMOVE_DIALOG);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferences, Preference preference) {
        if (preference instanceof SyncStateCheckBoxPreference) {
            SyncStateCheckBoxPreference syncPref = (SyncStateCheckBoxPreference) preference;
            String authority = syncPref.getAuthority();
            Account account = syncPref.getAccount();
            boolean syncAutomatically = ContentResolver.getSyncAutomatically(account, authority);
            if (syncPref.isOneTimeSyncMode()) {
                requestOrCancelSync(account, authority, true);
            } else {
                boolean syncOn = syncPref.isChecked();
                boolean oldSyncState = syncAutomatically;
                if (syncOn != oldSyncState) {
                    // if we're enabling sync, this will request a sync as well
                    ContentResolver.setSyncAutomatically(account, authority, syncOn);
                    // if the master sync switch is off, the request above will
                    // get dropped.  when the user clicks on this toggle,
                    // we want to force the sync, however.
                    if (!ContentResolver.getMasterSyncAutomatically() || !syncOn) {
                        requestOrCancelSync(account, authority, syncOn);
                    }
                }
            }
            return true;
        } else {
            return super.onPreferenceTreeClick(preferences, preference);
        }
    }

    private void startSyncForEnabledProviders() {
        requestOrCancelSyncForEnabledProviders(true /* start them */);
        getActivity().invalidateOptionsMenu();
    }

    private void cancelSyncForEnabledProviders() {
        requestOrCancelSyncForEnabledProviders(false /* cancel them */);
        getActivity().invalidateOptionsMenu();
    }

    private void requestOrCancelSyncForEnabledProviders(boolean startSync) {
        // sync everything that the user has enabled
        int count = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (! (pref instanceof SyncStateCheckBoxPreference)) {
                continue;
            }
            SyncStateCheckBoxPreference syncPref = (SyncStateCheckBoxPreference) pref;
            if (!syncPref.isChecked()) {
                continue;
            }
            requestOrCancelSync(syncPref.getAccount(), syncPref.getAuthority(), startSync);
        }
        // plus whatever the system needs to sync, e.g., invisible sync adapters
        if (mAccount != null) {
            for (SyncAdapterType syncAdapter : mInvisibleAdapters) {
                // invisible sync adapters' account type should be same as current account type
                if (syncAdapter.accountType.equals(mAccount.type)) {
                    requestOrCancelSync(mAccount, syncAdapter.authority, startSync);
                }
            }
        }
    }

    private void requestOrCancelSync(Account account, String authority, boolean flag) {
        if (flag) {
            Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSync(account, authority, extras);
        } else {
            ContentResolver.cancelSync(account, authority);
        }
    }

    private boolean isSyncing(List<SyncInfo> currentSyncs, Account account, String authority) {
        for (SyncInfo syncInfo : currentSyncs) {
            if (syncInfo.account.equals(account) && syncInfo.authority.equals(authority)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSyncStateUpdated() {
        if (!isResumed()) return;
        setFeedsState();
    }

    private void setFeedsState() {
        // iterate over all the preferences, setting the state properly for each
        Date date = new Date();
        List<SyncInfo> currentSyncs = ContentResolver.getCurrentSyncs();
        boolean syncIsFailing = false;

        // Refresh the sync status checkboxes - some syncs may have become active.
        updateAccountCheckboxes(mAccounts);

        for (int i = 0, count = getPreferenceScreen().getPreferenceCount(); i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (! (pref instanceof SyncStateCheckBoxPreference)) {
                continue;
            }
            SyncStateCheckBoxPreference syncPref = (SyncStateCheckBoxPreference) pref;

            String authority = syncPref.getAuthority();
            Account account = syncPref.getAccount();

            SyncStatusInfo status = ContentResolver.getSyncStatus(account, authority);
            boolean syncEnabled = ContentResolver.getSyncAutomatically(account, authority);
            boolean authorityIsPending = status == null ? false : status.pending;
            boolean initialSync = status == null ? false : status.initialize;

            boolean activelySyncing = isSyncing(currentSyncs, account, authority);
            boolean lastSyncFailed = status != null
                    && status.lastFailureTime != 0
                    && status.getLastFailureMesgAsInt(0)
                       != ContentResolver.SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS;
            if (!syncEnabled) lastSyncFailed = false;
            if (lastSyncFailed && !activelySyncing && !authorityIsPending) {
                syncIsFailing = true;
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "Update sync status: " + account + " " + authority +
                        " active = " + activelySyncing + " pend =" +  authorityIsPending);
            }

            final long successEndTime = (status == null) ? 0 : status.lastSuccessTime;
            if (!syncEnabled) {
                syncPref.setSummary(R.string.sync_disabled);
            } else if (activelySyncing) {
                syncPref.setSummary(R.string.sync_in_progress);
            } else if (successEndTime != 0) {
                date.setTime(successEndTime);
                final String timeString = formatSyncDate(date);
                syncPref.setSummary(getResources().getString(R.string.last_synced, timeString));
            } else {
                syncPref.setSummary("");
            }
            int syncState = ContentResolver.getIsSyncable(account, authority);

            syncPref.setActive(activelySyncing && (syncState >= 0) &&
                    !initialSync);
            syncPref.setPending(authorityIsPending && (syncState >= 0) &&
                    !initialSync);

            syncPref.setFailed(lastSyncFailed);
            ConnectivityManager connManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final boolean masterSyncAutomatically = ContentResolver.getMasterSyncAutomatically();
            final boolean backgroundDataEnabled = connManager.getBackgroundDataSetting();
            final boolean oneTimeSyncMode = !masterSyncAutomatically || !backgroundDataEnabled;
            syncPref.setOneTimeSyncMode(oneTimeSyncMode);
            syncPref.setChecked(oneTimeSyncMode || syncEnabled);
        }
        mErrorInfoView.setVisibility(syncIsFailing ? View.VISIBLE : View.GONE);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        super.onAccountsUpdated(accounts);
        mAccounts = accounts;
        updateAccountCheckboxes(accounts);
        onSyncStateUpdated();
    }

    private void updateAccountCheckboxes(Account[] accounts) {
        mInvisibleAdapters.clear();

        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypes();
        HashMap<String, ArrayList<String>> accountTypeToAuthorities =
            Maps.newHashMap();
        for (int i = 0, n = syncAdapters.length; i < n; i++) {
            final SyncAdapterType sa = syncAdapters[i];
            if (sa.isUserVisible()) {
                ArrayList<String> authorities = accountTypeToAuthorities.get(sa.accountType);
                if (authorities == null) {
                    authorities = new ArrayList<String>();
                    accountTypeToAuthorities.put(sa.accountType, authorities);
                }
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.d(TAG, "onAccountUpdated: added authority " + sa.authority
                            + " to accountType " + sa.accountType);
                }
                authorities.add(sa.authority);
            } else {
                // keep track of invisible sync adapters, so sync now forces
                // them to sync as well.
                mInvisibleAdapters.add(sa);
            }
        }

        for (int i = 0, n = mCheckBoxes.size(); i < n; i++) {
            getPreferenceScreen().removePreference(mCheckBoxes.get(i));
        }
        mCheckBoxes.clear();

        for (int i = 0, n = accounts.length; i < n; i++) {
            final Account account = accounts[i];
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "looking for sync adapters that match account " + account);
            }
            final ArrayList<String> authorities = accountTypeToAuthorities.get(account.type);
            if (authorities != null && (mAccount == null || mAccount.equals(account))) {
                for (int j = 0, m = authorities.size(); j < m; j++) {
                    final String authority = authorities.get(j);
                    // We could check services here....
                    int syncState = ContentResolver.getIsSyncable(account, authority);
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.d(TAG, "  found authority " + authority + " " + syncState);
                    }
                    if (syncState > 0) {
                        addSyncStateCheckBox(account, authority);
                    }
                }
            }
        }

        Collections.sort(mCheckBoxes);
        for (int i = 0, n = mCheckBoxes.size(); i < n; i++) {
            getPreferenceScreen().addPreference(mCheckBoxes.get(i));
        }
    }

    /**
     * Updates the titlebar with an icon for the provider type.
     */
    @Override
    protected void onAuthDescriptionsUpdated() {
        super.onAuthDescriptionsUpdated();
        getPreferenceScreen().removeAll();
        if (mAccount != null) {
            mProviderIcon.setImageDrawable(getDrawableForType(mAccount.type));
            mProviderId.setText(getLabelForType(mAccount.type));
        }
        addPreferencesFromResource(R.xml.account_sync_settings);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_accounts;
    }
}
