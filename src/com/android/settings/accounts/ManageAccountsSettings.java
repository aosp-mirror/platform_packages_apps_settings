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
import android.accounts.AuthenticatorDescription;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.AccountPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.location.LocationSettings;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static android.content.Intent.EXTRA_USER;

/** Manages settings for Google Account. */
public class ManageAccountsSettings extends AccountPreferenceBase
        implements AuthenticatorHelper.OnAccountsUpdateListener {
    private static final String ACCOUNT_KEY = "account"; // to pass to auth settings
    public static final String KEY_ACCOUNT_TYPE = "account_type";
    public static final String KEY_ACCOUNT_LABEL = "account_label";

    // Action name for the broadcast intent when the Google account preferences page is launching
    // the location settings.
    private static final String LAUNCHING_LOCATION_SETTINGS =
            "com.android.settings.accounts.LAUNCHING_LOCATION_SETTINGS";

    private static final int MENU_SYNC_NOW_ID = Menu.FIRST;
    private static final int MENU_SYNC_CANCEL_ID    = Menu.FIRST + 1;

    private static final int REQUEST_SHOW_SYNC_SETTINGS = 1;

    private String[] mAuthorities;
    private TextView mErrorInfoView;

    // If an account type is set, then show only accounts of that type
    private String mAccountType;
    // Temporary hack, to deal with backward compatibility 
    // mFirstAccount is used for the injected preferences
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
    public void onResume() {
        super.onResume();
        mAuthenticatorHelper.listenToAccountUpdates();
        updateAuthDescriptions();
        showAccountsIfNeeded();
        showSyncState();
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
    }

    @Override
    public void onPause() {
        super.onPause();
        mAuthenticatorHelper.stopListeningToAccountUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
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
        args.putParcelable(EXTRA_USER, mUserHandle);
        ((SettingsActivity) getActivity()).startPreferencePanel(
                AccountSyncSettings.class.getCanonicalName(), args,
                R.string.account_sync_settings_title, acctPref.getAccount().name,
                this, REQUEST_SHOW_SYNC_SETTINGS);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_SYNC_NOW_ID, 0, getString(R.string.sync_menu_sync_now))
                .setIcon(R.drawable.ic_menu_refresh_holo_dark);
        menu.add(0, MENU_SYNC_CANCEL_ID, 0, getString(R.string.sync_menu_sync_cancel))
                .setIcon(com.android.internal.R.drawable.ic_menu_close_clear_cancel);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean syncActive = !ContentResolver.getCurrentSyncsAsUser(
                mUserHandle.getIdentifier()).isEmpty();
        menu.findItem(MENU_SYNC_NOW_ID).setVisible(!syncActive);
        menu.findItem(MENU_SYNC_CANCEL_ID).setVisible(syncActive);
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
        final int userId = mUserHandle.getIdentifier();
        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(userId);
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
                            && ContentResolver.getSyncAutomaticallyAsUser(account, sa.authority,
                                    userId)) {
                        if (sync) {
                            ContentResolver.requestSyncAsUser(account, sa.authority, userId,
                                    extras);
                        } else {
                            ContentResolver.cancelSyncAsUser(account, sa.authority, userId);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onSyncStateUpdated() {
        showSyncState();
        // Catch any delayed delivery of update messages
        final Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    /**
     * Shows the sync state of the accounts. Note: it must be called after the accounts have been
     * loaded, @see #showAccountsIfNeeded().
     */
    private void showSyncState() {
        // Catch any delayed delivery of update messages
        if (getActivity() == null || getActivity().isFinishing()) return;

        final int userId = mUserHandle.getIdentifier();

        // iterate over all the preferences, setting the state properly for each
        List<SyncInfo> currentSyncs = ContentResolver.getCurrentSyncsAsUser(userId);

        boolean anySyncFailed = false; // true if sync on any account failed
        Date date = new Date();

        // only track userfacing sync adapters when deciding if account is synced or not
        final SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(userId);
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
                    SyncStatusInfo status = ContentResolver.getSyncStatusAsUser(account, authority,
                            userId);
                    boolean syncEnabled = isSyncEnabled(userId, account, authority);
                    boolean authorityIsPending = ContentResolver.isSyncPending(account, authority);
                    boolean activelySyncing = isSyncing(currentSyncs, account, authority);
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


    private boolean isSyncing(List<SyncInfo> currentSyncs, Account account, String authority) {
        final int count = currentSyncs.size();
        for (int i = 0; i < count;  i++) {
            SyncInfo syncInfo = currentSyncs.get(i);
            if (syncInfo.account.equals(account) && syncInfo.authority.equals(authority)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSyncEnabled(int userId, Account account, String authority) {
        return ContentResolver.getSyncAutomaticallyAsUser(account, authority, userId)
                && ContentResolver.getMasterSyncAutomaticallyAsUser(userId)
                && (ContentResolver.getIsSyncableAsUser(account, authority, userId) > 0);
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        showAccountsIfNeeded();
        onSyncStateUpdated();
    }

    private void showAccountsIfNeeded() {
        if (getActivity() == null) return;
        Account[] accounts = AccountManager.get(getActivity()).getAccountsAsUser(
                mUserHandle.getIdentifier());
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
                }
            }
        }
        if (mAccountType != null && mFirstAccount != null) {
            addAuthenticatorSettings();
        } else {
            // There's no account, close activity
            finish();
        }
    }

    private void addAuthenticatorSettings() {
        PreferenceScreen prefs = addPreferencesForType(mAccountType, getPreferenceScreen());
        if (prefs != null) {
            updatePreferenceIntents(prefs);
        }
    }

    /** Listens to a preference click event and starts a fragment */
    private class FragmentStarter
            implements Preference.OnPreferenceClickListener {
        private final String mClass;
        private final int mTitleRes;

        /**
         * @param className the class name of the fragment to be started.
         * @param title the title resource id of the started preference panel.
         */
        public FragmentStarter(String className, int title) {
            mClass = className;
            mTitleRes = title;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            ((SettingsActivity) getActivity()).startPreferencePanel(
                    mClass, null, mTitleRes, null, null, 0);
            // Hack: announce that the Google account preferences page is launching the location
            // settings
            if (mClass.equals(LocationSettings.class.getName())) {
                Intent intent = new Intent(LAUNCHING_LOCATION_SETTINGS);
                getActivity().sendBroadcast(
                        intent, android.Manifest.permission.WRITE_SECURE_SETTINGS);
            }
            return true;
        }
    }

    /**
     * Filters through the preference list provided by GoogleLoginService.
     *
     * This method removes all the invalid intent from the list, adds account name as extra into the
     * intent, and hack the location settings to start it as a fragment.
     */
    private void updatePreferenceIntents(PreferenceScreen prefs) {
        final PackageManager pm = getActivity().getPackageManager();
        for (int i = 0; i < prefs.getPreferenceCount();) {
            Preference pref = prefs.getPreference(i);
            Intent intent = pref.getIntent();
            if (intent != null) {
                // Hack. Launch "Location" as fragment instead of as activity.
                //
                // When "Location" is launched as activity via Intent, there's no "Up" button at the
                // top left, and if there's another running instance of "Location" activity, the
                // back stack would usually point to some other place so the user won't be able to
                // go back to the previous page by "back" key. Using fragment is a much easier
                // solution to those problems.
                //
                // If we set Intent to null and assign a fragment to the PreferenceScreen item here,
                // in order to make it work as expected, we still need to modify the container
                // PreferenceActivity, override onPreferenceStartFragment() and call
                // startPreferencePanel() there. In order to inject the title string there, more
                // dirty further hack is still needed. It's much easier and cleaner to listen to
                // preference click event here directly.
                if (intent.getAction().equals(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) {
                    // The OnPreferenceClickListener overrides the click event completely. No intent
                    // will get fired.
                    pref.setOnPreferenceClickListener(new FragmentStarter(
                            LocationSettings.class.getName(),
                            R.string.location_settings_title));
                } else {
                    ResolveInfo ri = pm.resolveActivityAsUser(intent,
                            PackageManager.MATCH_DEFAULT_ONLY, mUserHandle.getIdentifier());
                    if (ri == null) {
                        prefs.removePreference(pref);
                        continue;
                    } else {
                        intent.putExtra(ACCOUNT_KEY, mFirstAccount);
                        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                Intent prefIntent = preference.getIntent();
                                /*
                                 * Check the intent to see if it resolves to a exported=false
                                 * activity that doesn't share a uid with the authenticator.
                                 *
                                 * Otherwise the intent is considered unsafe in that it will be
                                 * exploiting the fact that settings has system privileges.
                                 */
                                if (isSafeIntent(pm, prefIntent)) {
                                    getActivity().startActivityAsUser(prefIntent, mUserHandle);
                                } else {
                                    Log.e(TAG,
                                            "Refusing to launch authenticator intent because"
                                            + "it exploits Settings permissions: "
                                            + prefIntent);
                                }
                                return true;
                            }
                        });
                    }
                }
            }
            i++;
        }
    }

    /**
     * Determines if the supplied Intent is safe. A safe intent is one that is
     * will launch a exported=true activity or owned by the same uid as the
     * authenticator supplying the intent.
     */
    private boolean isSafeIntent(PackageManager pm, Intent intent) {
        AuthenticatorDescription authDesc =
                mAuthenticatorHelper.getAccountTypeDescription(mAccountType);
        ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return false;
        }
        ActivityInfo resolvedActivityInfo = resolveInfo.activityInfo;
        ApplicationInfo resolvedAppInfo = resolvedActivityInfo.applicationInfo;
        try {
            ApplicationInfo authenticatorAppInf = pm.getApplicationInfo(authDesc.packageName, 0);
            return resolvedActivityInfo.exported
                    || resolvedAppInfo.uid == authenticatorAppInf.uid;
        } catch (NameNotFoundException e) {
            Log.e(TAG,
                    "Intent considered unsafe due to exception.",
                    e);
            return false;
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
