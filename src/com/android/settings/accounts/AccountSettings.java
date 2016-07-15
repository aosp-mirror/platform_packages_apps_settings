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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.AccessiblePreferenceCategory;
import com.android.settings.DimmableIconPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.users.UserDialogs;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.accounts.AuthenticatorHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.Intent.EXTRA_USER;
import static android.os.UserManager.DISALLOW_MODIFY_ACCOUNTS;
import static android.provider.Settings.EXTRA_AUTHORITIES;

/**
 * Settings screen for the account types on the device.
 * This shows all account types available for personal and work profiles.
 *
 * An extra {@link UserHandle} can be specified in the intent as {@link EXTRA_USER}, if the user for
 * which the action needs to be performed is different to the one the Settings App will run in.
 */
public class AccountSettings extends SettingsPreferenceFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener,
        OnPreferenceClickListener, Indexable {
    public static final String TAG = "AccountSettings";

    private static final String KEY_ACCOUNT = "account";

    private static final String ADD_ACCOUNT_ACTION = "android.settings.ADD_ACCOUNT_SETTINGS";
    private static final String TAG_CONFIRM_AUTO_SYNC_CHANGE = "confirmAutoSyncChange";

    private static final int ORDER_LAST = 1002;
    private static final int ORDER_NEXT_TO_LAST = 1001;
    private static final int ORDER_NEXT_TO_NEXT_TO_LAST = 1000;

    private UserManager mUm;
    private SparseArray<ProfileData> mProfiles = new SparseArray<ProfileData>();
    private ManagedProfileBroadcastReceiver mManagedProfileBroadcastReceiver
                = new ManagedProfileBroadcastReceiver();
    private Preference mProfileNotAvailablePreference;
    private String[] mAuthorities;
    private int mAuthoritiesCount = 0;

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
        public DimmableIconPreference addAccountPreference;
        /**
         * The preference that displays the button to remove the managed profile
         */
        public Preference removeWorkProfilePreference;
        /**
         * The preference that displays managed profile settings.
         */
        public Preference managedProfilePreference;
        /**
         * The {@link AuthenticatorHelper} that holds accounts data for this profile.
         */
        public AuthenticatorHelper authenticatorHelper;
        /**
         * The {@link UserInfo} of the profile.
         */
        public UserInfo userInfo;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ACCOUNT;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);
        mProfileNotAvailablePreference = new Preference(getPrefContext());
        mAuthorities = getActivity().getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        if (mAuthorities != null) {
            mAuthoritiesCount = mAuthorities.length;
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.account_settings, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final UserHandle currentProfile = Process.myUserHandle();
        if (mProfiles.size() == 1) {
            menu.findItem(R.id.account_settings_menu_auto_sync)
                    .setVisible(true)
                    .setOnMenuItemClickListener(new MasterSyncStateClickListener(currentProfile))
                    .setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                            currentProfile.getIdentifier()));
            menu.findItem(R.id.account_settings_menu_auto_sync_personal).setVisible(false);
            menu.findItem(R.id.account_settings_menu_auto_sync_work).setVisible(false);
        } else if (mProfiles.size() > 1) {
            // We assume there's only one managed profile, otherwise UI needs to change
            final UserHandle managedProfile = mProfiles.valueAt(1).userInfo.getUserHandle();

            menu.findItem(R.id.account_settings_menu_auto_sync_personal)
                    .setVisible(true)
                    .setOnMenuItemClickListener(new MasterSyncStateClickListener(currentProfile))
                    .setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                            currentProfile.getIdentifier()));
            menu.findItem(R.id.account_settings_menu_auto_sync_work)
                    .setVisible(true)
                    .setOnMenuItemClickListener(new MasterSyncStateClickListener(managedProfile))
                    .setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                            managedProfile.getIdentifier()));
            menu.findItem(R.id.account_settings_menu_auto_sync).setVisible(false);
         } else {
             Log.w(TAG, "Method onPrepareOptionsMenu called before mProfiles was initialized");
         }
    }

    @Override
    public void onResume() {
        super.onResume();
        cleanUpPreferences();
        updateUi();
        mManagedProfileBroadcastReceiver.register(getActivity());
        listenToAccountUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListeningToAccountUpdates();
        mManagedProfileBroadcastReceiver.unregister(getActivity());
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
                intent.putExtra(EXTRA_USER, profileData.userInfo.getUserHandle());
                intent.putExtra(EXTRA_AUTHORITIES, mAuthorities);
                startActivity(intent);
                return true;
            }
            if (preference == profileData.removeWorkProfilePreference) {
                final int userId = profileData.userInfo.id;
                UserDialogs.createRemoveDialog(getActivity(), userId,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mUm.removeUser(userId);
                            }
                        }
                ).show();
                return true;
            }
            if (preference == profileData.managedProfilePreference) {
                Bundle arguments = new Bundle();
                arguments.putParcelable(Intent.EXTRA_USER, profileData.userInfo.getUserHandle());
                ((SettingsActivity) getActivity()).startPreferencePanel(
                        ManagedProfileSettings.class.getName(), arguments,
                        R.string.managed_profile_settings_title, null, null, 0);
                return true;
            }
        }
        return false;
    }

    void updateUi() {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_settings);

        if (Utils.isManagedProfile(mUm)) {
            // This should not happen
            Log.e(TAG, "We should not be showing settings for a managed profile");
            finish();
            return;
        }

        final PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference(KEY_ACCOUNT);
        if(mUm.isLinkedUser()) {
            // Restricted user or similar
            UserInfo userInfo = mUm.getUserInfo(UserHandle.myUserId());
            updateProfileUi(userInfo, false /* no category needed */, preferenceScreen);
        } else {
            List<UserInfo> profiles = mUm.getProfiles(UserHandle.myUserId());
            final int profilesCount = profiles.size();
            final boolean addCategory = profilesCount > 1;
            for (int i = 0; i < profilesCount; i++) {
                updateProfileUi(profiles.get(i), addCategory, preferenceScreen);
            }
        }

        // Add all preferences, starting with one for the primary profile.
        // Note that we're relying on the ordering given by the SparseArray keys, and on the
        // value of UserHandle.USER_OWNER being smaller than all the rest.
        final int profilesCount = mProfiles.size();
        for (int i = 0; i < profilesCount; i++) {
            ProfileData profileData = mProfiles.valueAt(i);
            if (!profileData.preferenceGroup.equals(preferenceScreen)) {
                preferenceScreen.addPreference(profileData.preferenceGroup);
            }
            updateAccountTypes(profileData);
        }
    }

    private void updateProfileUi(final UserInfo userInfo, boolean addCategory,
            PreferenceScreen parent) {
        final Context context = getActivity();
        final ProfileData profileData = new ProfileData();
        profileData.userInfo = userInfo;
        if (addCategory) {
            profileData.preferenceGroup = new AccessiblePreferenceCategory(getPrefContext());
            if (userInfo.isManagedProfile()) {
                profileData.preferenceGroup.setLayoutResource(R.layout.work_profile_category);
                profileData.preferenceGroup.setTitle(R.string.category_work);
                String workGroupSummary = getWorkGroupSummary(context, userInfo);
                profileData.preferenceGroup.setSummary(workGroupSummary);
                ((AccessiblePreferenceCategory) profileData.preferenceGroup).setContentDescription(
                        getString(R.string.accessibility_category_work, workGroupSummary));
                profileData.removeWorkProfilePreference = newRemoveWorkProfilePreference(context);
                profileData.managedProfilePreference = newManagedProfileSettings();
            } else {
                profileData.preferenceGroup.setTitle(R.string.category_personal);
                ((AccessiblePreferenceCategory) profileData.preferenceGroup).setContentDescription(
                        getString(R.string.accessibility_category_personal));
            }
            parent.addPreference(profileData.preferenceGroup);
        } else {
            profileData.preferenceGroup = parent;
        }
        if (userInfo.isEnabled()) {
            profileData.authenticatorHelper = new AuthenticatorHelper(context,
                    userInfo.getUserHandle(), this);
            profileData.addAccountPreference = newAddAccountPreference(context);
            if (RestrictedLockUtils.hasBaseUserRestriction(context,
                    UserManager.DISALLOW_MODIFY_ACCOUNTS, userInfo.id)) {
                profileData.addAccountPreference.setEnabled(false);
            } else {
                profileData.addAccountPreference.checkRestrictionAndSetDisabled(
                        DISALLOW_MODIFY_ACCOUNTS, userInfo.id);
            }
        }
        mProfiles.put(userInfo.id, profileData);
        Index.getInstance(getActivity()).updateFromClassNameResource(
                AccountSettings.class.getName(), true, true);
    }

    private DimmableIconPreference newAddAccountPreference(Context context) {
        DimmableIconPreference preference = new DimmableIconPreference(getPrefContext());
        preference.setTitle(R.string.add_account_label);
        preference.setIcon(R.drawable.ic_menu_add);
        preference.setOnPreferenceClickListener(this);
        preference.setOrder(ORDER_NEXT_TO_NEXT_TO_LAST);
        return preference;
    }

    private Preference newRemoveWorkProfilePreference(Context context) {
        Preference preference = new Preference(getPrefContext());
        preference.setTitle(R.string.remove_managed_profile_label);
        preference.setIcon(R.drawable.ic_menu_delete);
        preference.setOnPreferenceClickListener(this);
        preference.setOrder(ORDER_LAST);
        return preference;
    }


    private Preference newManagedProfileSettings() {
        Preference preference = new Preference(getPrefContext());
        preference.setTitle(R.string.managed_profile_settings_title);
        preference.setIcon(R.drawable.ic_settings);
        preference.setOnPreferenceClickListener(this);
        preference.setOrder(ORDER_NEXT_TO_LAST);
        return preference;
    }

    private String getWorkGroupSummary(Context context, UserInfo userInfo) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo adminApplicationInfo = Utils.getAdminApplicationInfo(context, userInfo.id);
        if (adminApplicationInfo == null) {
            return null;
        }
        CharSequence appLabel = packageManager.getApplicationLabel(adminApplicationInfo);
        return getString(R.string.managing_admin, appLabel);
    }

    private void cleanUpPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }
        mProfiles.clear();
    }

    private void listenToAccountUpdates() {
        final int count = mProfiles.size();
        for (int i = 0; i < count; i++) {
            AuthenticatorHelper authenticatorHelper = mProfiles.valueAt(i).authenticatorHelper;
            if (authenticatorHelper != null) {
                authenticatorHelper.listenToAccountUpdates();
            }
        }
    }

    private void stopListeningToAccountUpdates() {
        final int count = mProfiles.size();
        for (int i = 0; i < count; i++) {
            AuthenticatorHelper authenticatorHelper = mProfiles.valueAt(i).authenticatorHelper;
            if (authenticatorHelper != null) {
                authenticatorHelper.stopListeningToAccountUpdates();
            }
        }
    }

    private void updateAccountTypes(ProfileData profileData) {
        profileData.preferenceGroup.removeAll();
        if (profileData.userInfo.isEnabled()) {
            final ArrayList<AccountPreference> preferences = getAccountTypePreferences(
                    profileData.authenticatorHelper, profileData.userInfo.getUserHandle());
            final int count = preferences.size();
            for (int i = 0; i < count; i++) {
                profileData.preferenceGroup.addPreference(preferences.get(i));
            }
            if (profileData.addAccountPreference != null) {
                profileData.preferenceGroup.addPreference(profileData.addAccountPreference);
            }
        } else {
            // Put a label instead of the accounts list
            mProfileNotAvailablePreference.setEnabled(false);
            mProfileNotAvailablePreference.setIcon(R.drawable.empty_icon);
            mProfileNotAvailablePreference.setTitle(null);
            mProfileNotAvailablePreference.setSummary(
                    R.string.managed_profile_not_available_label);
            profileData.preferenceGroup.addPreference(mProfileNotAvailablePreference);
        }
        if (profileData.removeWorkProfilePreference != null) {
            profileData.preferenceGroup.addPreference(profileData.removeWorkProfilePreference);
        }
        if (profileData.managedProfilePreference != null) {
            profileData.preferenceGroup.addPreference(profileData.managedProfilePreference);
        }
    }

    private ArrayList<AccountPreference> getAccountTypePreferences(AuthenticatorHelper helper,
            UserHandle userHandle) {
        final String[] accountTypes = helper.getEnabledAccountTypes();
        final ArrayList<AccountPreference> accountTypePreferences =
                new ArrayList<AccountPreference>(accountTypes.length);

        for (int i = 0; i < accountTypes.length; i++) {
            final String accountType = accountTypes[i];
            // Skip showing any account that does not have any of the requested authorities
            if (!accountTypeHasAnyRequestedAuthorities(helper, accountType)) {
                continue;
            }
            final CharSequence label = helper.getLabelForType(getActivity(), accountType);
            if (label == null) {
                continue;
            }
            final String titleResPackageName = helper.getPackageForType(accountType);
            final int titleResId = helper.getLabelIdForType(accountType);

            final Account[] accounts = AccountManager.get(getActivity())
                    .getAccountsByTypeAsUser(accountType, userHandle);
            final boolean skipToAccount = accounts.length == 1
                    && !helper.hasAccountPreferences(accountType);

            if (skipToAccount) {
                final Bundle fragmentArguments = new Bundle();
                fragmentArguments.putParcelable(AccountSyncSettings.ACCOUNT_KEY,
                        accounts[0]);
                fragmentArguments.putParcelable(EXTRA_USER, userHandle);

                accountTypePreferences.add(new AccountPreference(getPrefContext(), label,
                        titleResPackageName, titleResId, AccountSyncSettings.class.getName(),
                        fragmentArguments,
                        helper.getDrawableForType(getActivity(), accountType)));
            } else {
                final Bundle fragmentArguments = new Bundle();
                fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_LABEL,
                        label.toString());
                fragmentArguments.putParcelable(EXTRA_USER, userHandle);

                accountTypePreferences.add(new AccountPreference(getPrefContext(), label,
                        titleResPackageName, titleResId, ManageAccountsSettings.class.getName(),
                        fragmentArguments,
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

    private boolean accountTypeHasAnyRequestedAuthorities(AuthenticatorHelper helper,
            String accountType) {
        if (mAuthoritiesCount == 0) {
            // No authorities required
            return true;
        }
        final ArrayList<String> authoritiesForType = helper.getAuthoritiesForAccountType(
                accountType);
        if (authoritiesForType == null) {
            Log.d(TAG, "No sync authorities for account type: " + accountType);
            return false;
        }
        for (int j = 0; j < mAuthoritiesCount; j++) {
            if (authoritiesForType.contains(mAuthorities[j])) {
                return true;
            }
        }
        return false;
    }

    private class AccountPreference extends Preference implements OnPreferenceClickListener {
        /**
         * Title of the tile that is shown to the user.
         * @attr ref android.R.styleable#PreferenceHeader_title
         */
        private final CharSequence mTitle;

        /**
         * Packange name used to resolve the resources of the title shown to the user in the new
         * fragment.
         */
        private final String mTitleResPackageName;

        /**
         * Resource id of the title shown to the user in the new fragment.
         */
        private final int mTitleResId;

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

        public AccountPreference(Context context, CharSequence title, String titleResPackageName,
                int titleResId, String fragment, Bundle fragmentArguments,
                Drawable icon) {
            super(context);
            mTitle = title;
            mTitleResPackageName = titleResPackageName;
            mTitleResId = titleResId;
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
                Utils.startWithFragment(getContext(), mFragment, mFragmentArguments,
                        null /* resultTo */, 0 /* resultRequestCode */, mTitleResPackageName,
                        mTitleResId, null /* title */);
                return true;
            }
            return false;
        }
    }

    private class ManagedProfileBroadcastReceiver extends BroadcastReceiver {
        private boolean listeningToManagedProfileEvents;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.v(TAG, "Received broadcast: " + action);
            if (action.equals(Intent.ACTION_MANAGED_PROFILE_REMOVED)
                    || action.equals(Intent.ACTION_MANAGED_PROFILE_ADDED)) {
                // Clean old state
                stopListeningToAccountUpdates();
                cleanUpPreferences();
                // Build new state
                updateUi();
                listenToAccountUpdates();
                // Force the menu to update. Note that #onPrepareOptionsMenu uses data built by
                // #updateUi so we must call this later
                getActivity().invalidateOptionsMenu();
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
            if (ActivityManager.isUserAMonkey()) {
                Log.d(TAG, "ignoring monkey's attempt to flip sync state");
            } else {
                ConfirmAutoSyncChangeFragment.show(AccountSettings.this, !item.isChecked(),
                        mUserHandle);
            }
            return true;
        }
    }

    /**
     * Dialog to inform user about changing auto-sync setting
     */
    public static class ConfirmAutoSyncChangeFragment extends DialogFragment {
        private static final String SAVE_ENABLING = "enabling";
        private static final String SAVE_USER_HANDLE = "userHandle";
        private boolean mEnabling;
        private UserHandle mUserHandle;

        public static void show(AccountSettings parent, boolean enabling, UserHandle userHandle) {
            if (!parent.isAdded()) return;

            final ConfirmAutoSyncChangeFragment dialog = new ConfirmAutoSyncChangeFragment();
            dialog.mEnabling = enabling;
            dialog.mUserHandle = userHandle;
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_AUTO_SYNC_CHANGE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            if (savedInstanceState != null) {
                mEnabling = savedInstanceState.getBoolean(SAVE_ENABLING);
                mUserHandle = (UserHandle) savedInstanceState.getParcelable(SAVE_USER_HANDLE);
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if (!mEnabling) {
                builder.setTitle(R.string.data_usage_auto_sync_off_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_off_dialog);
            } else {
                builder.setTitle(R.string.data_usage_auto_sync_on_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_on_dialog);
            }

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ContentResolver.setMasterSyncAutomaticallyAsUser(mEnabling,
                            mUserHandle.getIdentifier());
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_ENABLING, mEnabling);
            outState.putParcelable(SAVE_USER_HANDLE, mUserHandle);
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.account_settings;
            return Arrays.asList(sir);
        }

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();
            final String screenTitle = res.getString(R.string.account_settings_title);

            final UserManager um = UserManager.get(context);
            List<UserInfo> profiles = um.getProfiles(UserHandle.myUserId());
            final int profilesCount = profiles.size();
            for (int i = 0; i < profilesCount; i++) {
                UserInfo userInfo = profiles.get(i);
                if (userInfo.isEnabled()) {
                    if (!RestrictedLockUtils.hasBaseUserRestriction(context,
                            DISALLOW_MODIFY_ACCOUNTS, userInfo.id)) {
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.add_account_label);
                        data.screenTitle = screenTitle;
                        result.add(data);
                    }
                    if (userInfo.isManagedProfile()) {
                        {
                            SearchIndexableRaw data = new SearchIndexableRaw(context);
                            data.title = res.getString(R.string.remove_managed_profile_label);
                            data.screenTitle = screenTitle;
                            result.add(data);
                        }
                        {
                            SearchIndexableRaw data = new SearchIndexableRaw(context);
                            data.title = res.getString(R.string.managed_profile_settings_title);
                            data.screenTitle = screenTitle;
                            result.add(data);
                        }
                    }
                }
            }
            return result;
        }
    };
}
