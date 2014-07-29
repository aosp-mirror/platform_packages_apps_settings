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
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static android.content.Intent.EXTRA_USER;

/**
 * Settings screen for the account types on the device.
 * This shows all account types available for personal and work profiles.
 *
 * An extra {@link UserHandle} can be specified in the intent as {@link EXTRA_USER}, if the user for
 * which the action needs to be performed is different to the one the Settings App will run in.
 */
public class AccountSettings extends SettingsPreferenceFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener,
        OnPreferenceClickListener {
    public static final String TAG = "AccountSettings";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_ADD_ACCOUNT = "add_account";

    private static final String KEY_CATEGORY_PERSONAL = "account_personal";
    private static final String KEY_ADD_ACCOUNT_PERSONAL = "add_account_personal";
    private static final String KEY_CATEGORY_WORK = "account_work";
    private static final String KEY_ADD_ACCOUNT_WORK = "add_account_work";

    private static final String ADD_ACCOUNT_ACTION = "android.settings.ADD_ACCOUNT_SETTINGS";

    private static final ArrayList<String> EMPTY_LIST = new ArrayList<String>();

    private UserManager mUm;
    private SparseArray<ProfileData> mProfiles;
    private ManagedProfileBroadcastReceiver mManagedProfileBroadcastReceiver
                = new ManagedProfileBroadcastReceiver();
    private boolean mIsSingleProfileUi = true;

    /**
     * Holds data related to the accounts belonging to one profile.
     */
    private static class ProfileData {
        /**
         * The preference that displays the accounts.
         */
        public PreferenceGroup preferenceGroup;
        /**
         * The preference that displays the add account button.
         */
        public Preference addAccountPreference;
        /**
         * The user handle of the user that these accounts belong to.
         */
        public UserHandle userHandle;
        /**
         * The {@link AuthenticatorHelper} that holds accounts data for this profile.
         */
        public AuthenticatorHelper authenticatorHelper;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);
        mProfiles = new SparseArray<ProfileData>(2);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.account_settings, menu);
        final UserHandle currentProfile = UserHandle.getCallingUserHandle();
        if (mIsSingleProfileUi) {
            menu.findItem(R.id.account_settings_menu_auto_sync)
                    .setVisible(true)
                    .setOnMenuItemClickListener(new MasterSyncStateClickListener(currentProfile));
            menu.removeItem(R.id.account_settings_menu_auto_sync_personal);
            menu.removeItem(R.id.account_settings_menu_auto_sync_work);
        } else {
            final UserHandle managedProfile = Utils.getManagedProfile(mUm);

            menu.findItem(R.id.account_settings_menu_auto_sync_personal)
                    .setVisible(true)
                    .setOnMenuItemClickListener(new MasterSyncStateClickListener(currentProfile));
            menu.findItem(R.id.account_settings_menu_auto_sync_work)
                    .setVisible(true)
                    .setOnMenuItemClickListener(new MasterSyncStateClickListener(managedProfile));
            menu.removeItem(R.id.account_settings_menu_auto_sync);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final UserHandle currentProfile = UserHandle.getCallingUserHandle();
        if (mIsSingleProfileUi) {
            menu.findItem(R.id.account_settings_menu_auto_sync)
                    .setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                            currentProfile.getIdentifier()));
        } else {
            final UserHandle managedProfile = Utils.getManagedProfile(mUm);

            menu.findItem(R.id.account_settings_menu_auto_sync_personal)
                    .setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                            currentProfile.getIdentifier()));
            menu.findItem(R.id.account_settings_menu_auto_sync_work)
                    .setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                            managedProfile.getIdentifier()));
            }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
        mManagedProfileBroadcastReceiver.register(getActivity());
        listenToAccountUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListeningToAccountUpdates();
        mManagedProfileBroadcastReceiver.unregister(getActivity());
        cleanUpPreferences();
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        final ProfileData profileData = mProfiles.get(userHandle.getIdentifier());
        if (profileData != null) {
            updateAccountTypes(profileData);
        } else {
            Log.w(TAG, "Missing Settings screen for: " + userHandle.getIdentifier());
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // Check the preference
        final int count = mProfiles.size();
        for (int i = 0; i < count; i++) {
            ProfileData profileData = mProfiles.valueAt(i);
            if (preference == profileData.addAccountPreference) {
                Intent intent = new Intent(ADD_ACCOUNT_ACTION);
                intent.putExtra(EXTRA_USER, profileData.userHandle);
                startActivity(intent);
                return true;
            }
        }
        return false;
    }

    void updateUi() {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_settings);

        if(mUm.isLinkedUser()) {
            // Restricted user or similar
            updateSingleProfileUi();
        } else {
            if (Utils.isManagedProfile(mUm)) {
                // This should not happen
                Log.w(TAG, "We should not be showing settings for a managed profile");
                updateSingleProfileUi();
            }
            final UserHandle currentProfile = UserHandle.getCallingUserHandle();
            final UserHandle managedProfile = Utils.getManagedProfile(mUm);
            if (managedProfile == null) {
                updateSingleProfileUi();
            } else {
                mIsSingleProfileUi = false;
                updateProfileUi(currentProfile, KEY_CATEGORY_PERSONAL, KEY_ADD_ACCOUNT_PERSONAL,
                        EMPTY_LIST);
                final ArrayList<String> unusedPreferences = new ArrayList<String>(2);
                unusedPreferences.add(KEY_ADD_ACCOUNT);
                updateProfileUi(managedProfile, KEY_CATEGORY_WORK, KEY_ADD_ACCOUNT_WORK,
                        unusedPreferences);
            }
        }
        final int count = mProfiles.size();
        for (int i = 0; i < count; i++) {
            updateAccountTypes(mProfiles.valueAt(i));
        }
    }

    private void updateSingleProfileUi() {
        final ArrayList<String> unusedPreferences = new ArrayList<String>(2);
        unusedPreferences.add(KEY_CATEGORY_PERSONAL);
        unusedPreferences.add(KEY_CATEGORY_WORK);
        updateProfileUi(UserHandle.getCallingUserHandle(), KEY_ACCOUNT, KEY_ADD_ACCOUNT,
                unusedPreferences);
    }

    private void updateProfileUi(final UserHandle userHandle, String categoryKey,
            String addAccountKey, ArrayList<String> unusedPreferences) {
        final int count = unusedPreferences.size();
        for (int i = 0; i < count; i++) {
            removePreference(unusedPreferences.get(i));
        }
        final ProfileData profileData = new ProfileData();
        profileData.preferenceGroup = (PreferenceGroup) findPreference(categoryKey);
        if (mUm.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS, userHandle)) {
            removePreference(addAccountKey);
        } else {
            profileData.addAccountPreference = findPreference(addAccountKey);
            profileData.addAccountPreference.setOnPreferenceClickListener(this);
        }
        profileData.userHandle = userHandle;
        profileData.authenticatorHelper = new AuthenticatorHelper(
                getActivity(), userHandle, mUm, this);
        mProfiles.put(userHandle.getIdentifier(), profileData);
    }

    private void cleanUpPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }
    }

    private void listenToAccountUpdates() {
        final int count = mProfiles.size();
        for (int i = 0; i < count; i++) {
            mProfiles.valueAt(i).authenticatorHelper.listenToAccountUpdates();
        }
    }

    private void stopListeningToAccountUpdates() {
        final int count = mProfiles.size();
        for (int i = 0; i < count; i++) {
            mProfiles.valueAt(i).authenticatorHelper.stopListeningToAccountUpdates();
        }
    }

    private void updateAccountTypes(ProfileData profileData) {
        profileData.preferenceGroup.removeAll();
        final ArrayList<AccountPreference> preferences = getAccountTypePreferences(
                profileData.authenticatorHelper, profileData.userHandle);
        final int count = preferences.size();
        for (int i = 0; i < count; i++) {
            profileData.preferenceGroup.addPreference(preferences.get(i));
        }
        if (profileData.addAccountPreference != null) {
            profileData.preferenceGroup.addPreference(profileData.addAccountPreference);
        }
    }

    private ArrayList<AccountPreference> getAccountTypePreferences(AuthenticatorHelper helper,
            UserHandle userHandle) {
        final String[] accountTypes = helper.getEnabledAccountTypes();
        final ArrayList<AccountPreference> accountTypePreferences =
                new ArrayList<AccountPreference>(accountTypes.length);

        for (int i = 0; i < accountTypes.length; i++) {
            final String accountType = accountTypes[i];
            final CharSequence label = helper.getLabelForType(getActivity(), accountType);
            if (label == null) {
                continue;
            }

            final Account[] accounts = AccountManager.get(getActivity())
                    .getAccountsByTypeAsUser(accountType, userHandle);
            final boolean skipToAccount = accounts.length == 1
                    && !helper.hasAccountPreferences(accountType);

            if (skipToAccount) {
                final Bundle fragmentArguments = new Bundle();
                fragmentArguments.putParcelable(AccountSyncSettings.ACCOUNT_KEY,
                        accounts[0]);
                fragmentArguments.putParcelable(EXTRA_USER, userHandle);

                accountTypePreferences.add(new AccountPreference(getActivity(), label,
                        AccountSyncSettings.class.getName(), fragmentArguments,
                        helper.getDrawableForType(getActivity(), accountType)));
            } else {
                final Bundle fragmentArguments = new Bundle();
                fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_LABEL,
                        label.toString());
                fragmentArguments.putParcelable(EXTRA_USER, userHandle);

                accountTypePreferences.add(new AccountPreference(getActivity(), label,
                        ManageAccountsSettings.class.getName(), fragmentArguments,
                        helper.getDrawableForType(getActivity(), accountType)));
            }
            helper.preloadDrawableForType(getActivity(), accountType);
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

        public AccountPreference(Context context, CharSequence title, String fragment,
                Bundle fragmentArguments, Drawable icon) {
            super(context);
            mTitle = title;
            mFragment = fragment;
            mFragmentArguments = fragmentArguments;
            setWidgetLayoutResource(R.layout.account_type_preference);

            setTitle(title);
            setIcon(icon);

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

    private class ManagedProfileBroadcastReceiver extends BroadcastReceiver {
        private boolean listeningToManagedProfileEvents;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_MANAGED_PROFILE_REMOVED)
                    || intent.getAction().equals(Intent.ACTION_MANAGED_PROFILE_ADDED)) {
                Log.v(TAG, "Received broadcast: " + intent.getAction());
                // Clean old state
                stopListeningToAccountUpdates();
                cleanUpPreferences();
                // Build new state
                updateUi();
                listenToAccountUpdates();
                return;
            }
            Log.w(TAG, "Cannot handle received broadcast: " + intent.getAction());
        }

        public void register(Context context) {
            if (!listeningToManagedProfileEvents) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
                intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
                context.registerReceiver(this, intentFilter);
                listeningToManagedProfileEvents = true;
            }
        }

        public void unregister(Context context) {
            if (listeningToManagedProfileEvents) {
                context.unregisterReceiver(this);
                listeningToManagedProfileEvents = false;
            }
        }
    }

    private class MasterSyncStateClickListener implements MenuItem.OnMenuItemClickListener {
        private final UserHandle mUserHandle;

        public MasterSyncStateClickListener(UserHandle userHandle) {
            mUserHandle = userHandle;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            // TODO: Add confirmation dialogs. See: http://b/16076571
            if (ActivityManager.isUserAMonkey()) {
                Log.d(TAG, "ignoring monkey's attempt to flip sync state");
            } else {
                boolean newSyncState = !item.isChecked();
                item.setChecked(newSyncState);
                ContentResolver.setMasterSyncAutomaticallyAsUser(newSyncState,
                        mUserHandle.getIdentifier());
            }
            return true;
        }
    }
    // TODO Implement a {@link SearchIndexProvider} to allow Indexing and Search of account types
    // See http://b/15403806
}
