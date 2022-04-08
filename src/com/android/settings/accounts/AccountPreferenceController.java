/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.content.Intent.EXTRA_USER;
import static android.os.UserManager.DISALLOW_MODIFY_ACCOUNTS;
import static android.os.UserManager.DISALLOW_REMOVE_MANAGED_PROFILE;
import static android.provider.Settings.ACTION_ADD_ACCOUNT;
import static android.provider.Settings.EXTRA_AUTHORITIES;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.BidiFormatter;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.AccessiblePreferenceCategory;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AccountPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, AuthenticatorHelper.OnAccountsUpdateListener,
        OnPreferenceClickListener, LifecycleObserver, OnPause, OnResume {

    private static final String TAG = "AccountPrefController";

    private static final int ORDER_ACCOUNT_PROFILES = 1;
    private static final int ORDER_LAST = 1002;
    private static final int ORDER_NEXT_TO_LAST = 1001;
    private static final int ORDER_NEXT_TO_NEXT_TO_LAST = 1000;

    private static final String PREF_KEY_ADD_ACCOUNT = "add_account";
    private static final String PREF_KEY_REMOVE_PROFILE = "remove_profile";
    private static final String PREF_KEY_WORK_PROFILE_SETTING = "work_profile_setting";

    private UserManager mUm;
    private SparseArray<ProfileData> mProfiles = new SparseArray<ProfileData>();
    private ManagedProfileBroadcastReceiver mManagedProfileBroadcastReceiver =
            new ManagedProfileBroadcastReceiver();
    private Preference mProfileNotAvailablePreference;
    private String[] mAuthorities;
    private int mAuthoritiesCount = 0;
    private SettingsPreferenceFragment mFragment;
    private int mAccountProfileOrder = ORDER_ACCOUNT_PROFILES;
    private AccountRestrictionHelper mHelper;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private @ProfileSelectFragment.ProfileType int mType;

    /**
     * Holds data related to the accounts belonging to one profile.
     */
    public static class ProfileData {
        /**
         * The preference that displays the accounts.
         */
        public PreferenceGroup preferenceGroup;
        /**
         * The preference that displays the add account button.
         */
        public RestrictedPreference addAccountPreference;
        /**
         * The preference that displays the button to remove the managed profile
         */
        public RestrictedPreference removeWorkProfilePreference;
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
        /**
         * The {@link UserInfo} of the profile.
         */
        public boolean pendingRemoval;
        /**
         * The map from account key to account preference
         */
        public ArrayMap<String, AccountTypePreference> accountPreferences = new ArrayMap<>();
    }

    public AccountPreferenceController(Context context, SettingsPreferenceFragment parent,
            String[] authorities, @ProfileSelectFragment.ProfileType int type) {
        this(context, parent, authorities, new AccountRestrictionHelper(context), type);
    }

    @VisibleForTesting
    AccountPreferenceController(Context context, SettingsPreferenceFragment parent,
            String[] authorities, AccountRestrictionHelper helper,
            @ProfileSelectFragment.ProfileType int type) {
        super(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mAuthorities = authorities;
        mFragment = parent;
        if (mAuthorities != null) {
            mAuthoritiesCount = mAuthorities.length;
        }
        final FeatureFactory featureFactory = FeatureFactory.getFactory(mContext);
        mMetricsFeatureProvider = featureFactory.getMetricsFeatureProvider();
        mHelper = helper;
        mType = type;
    }

    @Override
    public boolean isAvailable() {
        return !mUm.isManagedProfile();
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        updateUi();
    }

    @Override
    public void updateDynamicRawDataToIndex(List<SearchIndexableRaw> rawData) {
        if (!isAvailable()) {
            return;
        }
        final Resources res = mContext.getResources();
        final String screenTitle = res.getString(R.string.account_settings_title);

        List<UserInfo> profiles = mUm.getProfiles(UserHandle.myUserId());
        for (final UserInfo userInfo : profiles) {
            if (userInfo.isEnabled() && userInfo.isManagedProfile()) {
                if (!mHelper.hasBaseUserRestriction(DISALLOW_REMOVE_MANAGED_PROFILE,
                        UserHandle.myUserId())) {
                    final SearchIndexableRaw data = new SearchIndexableRaw(mContext);
                    data.key = PREF_KEY_REMOVE_PROFILE;
                    data.title = res.getString(R.string.remove_managed_profile_label);
                    data.screenTitle = screenTitle;
                    rawData.add(data);
                }
                final SearchIndexableRaw data = new SearchIndexableRaw(mContext);
                data.key = PREF_KEY_WORK_PROFILE_SETTING;
                data.title = res.getString(R.string.managed_profile_settings_title);
                data.screenTitle = screenTitle;
                rawData.add(data);
            }
        }
    }

    @Override
    public void onResume() {
        updateUi();
        mManagedProfileBroadcastReceiver.register(mContext);
        listenToAccountUpdates();
    }

    @Override
    public void onPause() {
        stopListeningToAccountUpdates();
        mManagedProfileBroadcastReceiver.unregister(mContext);
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
        final int metricsCategory = mFragment.getMetricsCategory();
        // Check the preference
        final int count = mProfiles.size();
        for (int i = 0; i < count; i++) {
            ProfileData profileData = mProfiles.valueAt(i);
            if (preference == profileData.addAccountPreference) {
                mMetricsFeatureProvider.logClickedPreference(preference, metricsCategory);
                Intent intent = new Intent(ACTION_ADD_ACCOUNT);
                intent.putExtra(EXTRA_USER, profileData.userInfo.getUserHandle());
                intent.putExtra(EXTRA_AUTHORITIES, mAuthorities);
                mContext.startActivity(intent);
                return true;
            }
            if (preference == profileData.removeWorkProfilePreference) {
                mMetricsFeatureProvider.logClickedPreference(preference, metricsCategory);
                final int userId = profileData.userInfo.id;
                RemoveUserFragment.newInstance(userId).show(mFragment.getFragmentManager(),
                        "removeUser");
                return true;
            }
            if (preference == profileData.managedProfilePreference) {
                mMetricsFeatureProvider.logClickedPreference(preference, metricsCategory);
                Bundle arguments = new Bundle();
                arguments.putParcelable(Intent.EXTRA_USER, profileData.userInfo.getUserHandle());
                new SubSettingLauncher(mContext)
                        .setSourceMetricsCategory(metricsCategory)
                        .setDestination(ManagedProfileSettings.class.getName())
                        .setTitleRes(R.string.managed_profile_settings_title)
                        .setArguments(arguments)
                        .launch();

                return true;
            }
        }
        return false;
    }

    private void updateUi() {
        if (!isAvailable()) {
            // This should not happen
            Log.e(TAG, "We should not be showing settings for a managed profile");
            return;
        }

        for (int i = 0, size = mProfiles.size(); i < size; i++) {
            mProfiles.valueAt(i).pendingRemoval = true;
        }
        if (mUm.isRestrictedProfile()) {
            // Restricted user or similar
            UserInfo userInfo = mUm.getUserInfo(UserHandle.myUserId());
            updateProfileUi(userInfo);
        } else {
            List<UserInfo> profiles = mUm.getProfiles(UserHandle.myUserId());
            final int profilesCount = profiles.size();
            for (int i = 0; i < profilesCount; i++) {
                if (profiles.get(i).isManagedProfile()
                        && (mType & ProfileSelectFragment.ProfileType.WORK) != 0) {
                    updateProfileUi(profiles.get(i));
                } else if (!profiles.get(i).isManagedProfile()
                        && (mType & ProfileSelectFragment.ProfileType.PERSONAL) != 0) {
                    updateProfileUi(profiles.get(i));
                }
            }
        }
        cleanUpPreferences();

        // Add all preferences, starting with one for the primary profile.
        // Note that we're relying on the ordering given by the SparseArray keys, and on the
        // value of UserHandle.USER_OWNER being smaller than all the rest.
        final int profilesCount = mProfiles.size();
        for (int i = 0; i < profilesCount; i++) {
            updateAccountTypes(mProfiles.valueAt(i));
        }
    }

    private void updateProfileUi(final UserInfo userInfo) {
        if (mFragment.getPreferenceManager() == null) {
            return;
        }
        final ProfileData data = mProfiles.get(userInfo.id);
        if (data != null) {
            data.pendingRemoval = false;
            data.userInfo = userInfo;
            if (userInfo.isEnabled()) {
                // recreate the authentication helper to refresh the list of enabled accounts
                data.authenticatorHelper =
                        new AuthenticatorHelper(mContext, userInfo.getUserHandle(), this);
            }
            return;
        }
        final Context context = mContext;
        final ProfileData profileData = new ProfileData();
        profileData.userInfo = userInfo;
        AccessiblePreferenceCategory preferenceGroup =
                mHelper.createAccessiblePreferenceCategory(
                        mFragment.getPreferenceManager().getContext());
        preferenceGroup.setOrder(mAccountProfileOrder++);
        if (isSingleProfile()) {
            preferenceGroup.setTitle(context.getString(R.string.account_for_section_header,
                    BidiFormatter.getInstance().unicodeWrap(userInfo.name)));
            preferenceGroup.setContentDescription(
                    mContext.getString(R.string.account_settings));
        } else if (userInfo.isManagedProfile()) {
            if (mType == ProfileSelectFragment.ProfileType.ALL) {
                preferenceGroup.setTitle(R.string.category_work);
                final String workGroupSummary = getWorkGroupSummary(context, userInfo);
                preferenceGroup.setSummary(workGroupSummary);
                preferenceGroup.setContentDescription(
                        mContext.getString(R.string.accessibility_category_work, workGroupSummary));
            }
            profileData.removeWorkProfilePreference = newRemoveWorkProfilePreference();
            mHelper.enforceRestrictionOnPreference(profileData.removeWorkProfilePreference,
                    DISALLOW_REMOVE_MANAGED_PROFILE, UserHandle.myUserId());
            profileData.managedProfilePreference = newManagedProfileSettings();
        } else {
            if (mType == ProfileSelectFragment.ProfileType.ALL) {
                preferenceGroup.setTitle(R.string.category_personal);
                preferenceGroup.setContentDescription(
                        mContext.getString(R.string.accessibility_category_personal));
            }
        }
        final PreferenceScreen screen = mFragment.getPreferenceScreen();
        if (screen != null) {
            screen.addPreference(preferenceGroup);
        }
        profileData.preferenceGroup = preferenceGroup;
        if (userInfo.isEnabled()) {
            profileData.authenticatorHelper = new AuthenticatorHelper(context,
                    userInfo.getUserHandle(), this);
            profileData.addAccountPreference = newAddAccountPreference();
            mHelper.enforceRestrictionOnPreference(profileData.addAccountPreference,
                    DISALLOW_MODIFY_ACCOUNTS, userInfo.id);
        }
        mProfiles.put(userInfo.id, profileData);
    }

    private RestrictedPreference newAddAccountPreference() {
        RestrictedPreference preference =
                new RestrictedPreference(mFragment.getPreferenceManager().getContext());
        preference.setKey(PREF_KEY_ADD_ACCOUNT);
        preference.setTitle(R.string.add_account_label);
        preference.setIcon(R.drawable.ic_add_24dp);
        preference.setOnPreferenceClickListener(this);
        preference.setOrder(ORDER_NEXT_TO_NEXT_TO_LAST);
        return preference;
    }

    private RestrictedPreference newRemoveWorkProfilePreference() {
        RestrictedPreference preference = new RestrictedPreference(
                mFragment.getPreferenceManager().getContext());
        preference.setKey(PREF_KEY_REMOVE_PROFILE);
        preference.setTitle(R.string.remove_managed_profile_label);
        preference.setIcon(R.drawable.ic_delete);
        preference.setOnPreferenceClickListener(this);
        preference.setOrder(ORDER_LAST);
        return preference;
    }


    private Preference newManagedProfileSettings() {
        Preference preference = new Preference(mFragment.getPreferenceManager().getContext());
        preference.setKey(PREF_KEY_WORK_PROFILE_SETTING);
        preference.setTitle(R.string.managed_profile_settings_title);
        preference.setIcon(R.drawable.ic_settings_24dp);
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
        return mContext.getString(R.string.managing_admin, appLabel);
    }

    void cleanUpPreferences() {
        PreferenceScreen screen = mFragment.getPreferenceScreen();
        if (screen == null) {
            return;
        }
        final int count = mProfiles.size();
        for (int i = count - 1; i >= 0; i--) {
            final ProfileData data = mProfiles.valueAt(i);
            if (data.pendingRemoval) {
                screen.removePreference(data.preferenceGroup);
                mProfiles.removeAt(i);
            }
        }
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
        if (mFragment.getPreferenceManager() == null
                || profileData.preferenceGroup.getPreferenceManager() == null) {
            // This could happen if activity is finishing
            return;
        }
        if (profileData.userInfo.isEnabled()) {
            final ArrayMap<String, AccountTypePreference> preferenceToRemove =
                    new ArrayMap<>(profileData.accountPreferences);
            final ArrayList<AccountTypePreference> preferences = getAccountTypePreferences(
                    profileData.authenticatorHelper, profileData.userInfo.getUserHandle(),
                    preferenceToRemove);
            final int count = preferences.size();
            for (int i = 0; i < count; i++) {
                final AccountTypePreference preference = preferences.get(i);
                preference.setOrder(i);
                final String key = preference.getKey();
                if (!profileData.accountPreferences.containsKey(key)) {
                    profileData.preferenceGroup.addPreference(preference);
                    profileData.accountPreferences.put(key, preference);
                }
            }
            if (profileData.addAccountPreference != null) {
                profileData.preferenceGroup.addPreference(profileData.addAccountPreference);
            }
            for (String key : preferenceToRemove.keySet()) {
                profileData.preferenceGroup.removePreference(
                        profileData.accountPreferences.get(key));
                profileData.accountPreferences.remove(key);
            }
        } else {
            profileData.preferenceGroup.removeAll();
            // Put a label instead of the accounts list
            if (mProfileNotAvailablePreference == null) {
                mProfileNotAvailablePreference =
                        new Preference(mFragment.getPreferenceManager().getContext());
            }
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

    private ArrayList<AccountTypePreference> getAccountTypePreferences(AuthenticatorHelper helper,
            UserHandle userHandle, ArrayMap<String, AccountTypePreference> preferenceToRemove) {
        final String[] accountTypes = helper.getEnabledAccountTypes();
        final ArrayList<AccountTypePreference> accountTypePreferences =
                new ArrayList<>(accountTypes.length);

        for (int i = 0; i < accountTypes.length; i++) {
            final String accountType = accountTypes[i];
            // Skip showing any account that does not have any of the requested authorities
            if (!accountTypeHasAnyRequestedAuthorities(helper, accountType)) {
                continue;
            }
            final CharSequence label = helper.getLabelForType(mContext, accountType);
            if (label == null) {
                continue;
            }
            final String titleResPackageName = helper.getPackageForType(accountType);
            final int titleResId = helper.getLabelIdForType(accountType);

            final Account[] accounts = AccountManager.get(mContext)
                    .getAccountsByTypeAsUser(accountType, userHandle);
            final Drawable icon = helper.getDrawableForType(mContext, accountType);
            final Context prefContext = mFragment.getPreferenceManager().getContext();

            // Add a preference row for each individual account
            for (Account account : accounts) {
                final AccountTypePreference preference =
                        preferenceToRemove.remove(AccountTypePreference.buildKey(account));
                if (preference != null) {
                    accountTypePreferences.add(preference);
                    continue;
                }
                final ArrayList<String> auths =
                        helper.getAuthoritiesForAccountType(account.type);
                if (!AccountRestrictionHelper.showAccount(mAuthorities, auths)) {
                    continue;
                }
                final Bundle fragmentArguments = new Bundle();
                fragmentArguments.putParcelable(AccountDetailDashboardFragment.KEY_ACCOUNT,
                        account);
                fragmentArguments.putParcelable(AccountDetailDashboardFragment.KEY_USER_HANDLE,
                        userHandle);
                fragmentArguments.putString(AccountDetailDashboardFragment.KEY_ACCOUNT_TYPE,
                        accountType);
                fragmentArguments.putString(AccountDetailDashboardFragment.KEY_ACCOUNT_LABEL,
                        label.toString());
                fragmentArguments.putInt(AccountDetailDashboardFragment.KEY_ACCOUNT_TITLE_RES,
                        titleResId);
                fragmentArguments.putParcelable(EXTRA_USER, userHandle);
                accountTypePreferences.add(new AccountTypePreference(
                        prefContext, mMetricsFeatureProvider.getMetricsCategory(mFragment),
                        account, titleResPackageName, titleResId, label,
                        AccountDetailDashboardFragment.class.getName(), fragmentArguments, icon));
            }
            helper.preloadDrawableForType(mContext, accountType);
        }
        // Sort by label
        Collections.sort(accountTypePreferences, new Comparator<AccountTypePreference>() {
            @Override
            public int compare(AccountTypePreference t1, AccountTypePreference t2) {
                int result = t1.getSummary().toString().compareTo(t2.getSummary().toString());
                return result != 0
                        ? result : t1.getTitle().toString().compareTo(t2.getTitle().toString());
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

    private boolean isSingleProfile() {
        return mUm.isLinkedUser() || mUm.getProfiles(UserHandle.myUserId()).size() == 1;
    }

    private class ManagedProfileBroadcastReceiver extends BroadcastReceiver {
        private boolean mListeningToManagedProfileEvents;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.v(TAG, "Received broadcast: " + action);
            if (action.equals(Intent.ACTION_MANAGED_PROFILE_REMOVED)
                    || action.equals(Intent.ACTION_MANAGED_PROFILE_ADDED)) {
                if (mFragment instanceof AccountWorkProfileDashboardFragment) {
                    mFragment.getActivity().finish();
                } else {
                    // Clean old state
                    stopListeningToAccountUpdates();
                    // Build new state
                    updateUi();
                    listenToAccountUpdates();
                }
                return;
            }
            Log.w(TAG, "Cannot handle received broadcast: " + intent.getAction());
        }

        public void register(Context context) {
            if (!mListeningToManagedProfileEvents) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
                intentFilter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
                context.registerReceiver(this, intentFilter);
                mListeningToManagedProfileEvents = true;
            }
        }

        public void unregister(Context context) {
            if (mListeningToManagedProfileEvents) {
                context.unregisterReceiver(this);
                mListeningToManagedProfileEvents = false;
            }
        }
    }
}
