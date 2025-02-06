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

import static android.provider.Settings.EXTRA_AUTHORITIES;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.credentials.CredentialManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.applications.autofill.PasswordsPreferenceController;
import com.android.settings.applications.credentials.CredentialManagerPreferenceController;
import com.android.settings.applications.credentials.DefaultCombinedPreferenceController;
import com.android.settings.applications.credentials.DefaultPrivateCombinedPreferenceController;
import com.android.settings.applications.credentials.DefaultWorkCombinedPreferenceController;
import com.android.settings.applications.defaultapps.DefaultAutofillPreferenceController;
import com.android.settings.applications.defaultapps.DefaultPrivateAutofillPreferenceController;
import com.android.settings.applications.defaultapps.DefaultWorkAutofillPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.users.AutoSyncDataPreferenceController;
import com.android.settings.users.AutoSyncPersonalDataPreferenceController;
import com.android.settings.users.AutoSyncPrivateDataPreferenceController;
import com.android.settings.users.AutoSyncWorkDataPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class AccountDashboardFragment extends DashboardFragment {
    private static final String TAG = "AccountDashboardFrag";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCOUNT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return getPreferenceLayoutResId(this.getContext());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_user_and_account_dashboard;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (CredentialManager.isServiceEnabled(context)) {
            CredentialManagerPreferenceController cmpp =
                    use(CredentialManagerPreferenceController.class);
            CredentialManagerPreferenceController.Delegate delegate =
                    new CredentialManagerPreferenceController.Delegate() {
                public void setActivityResult(int resultCode) {
                    getActivity().setResult(resultCode);
                }
                public void forceDelegateRefresh() {
                    forceUpdatePreferences();
                }
            };
            cmpp.init(this, getFragmentManager(), getIntent(), delegate,
                    /*isWorkProfile=*/false, /*isPrivateSpace=*/ false);
        } else {
            getSettingsLifecycle().addObserver(use(PasswordsPreferenceController.class));
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        buildAutofillPreferenceControllers(context, controllers,
                /*isWorkProfile=*/false, /*isPrivateSpace=*/ false);
        final String[] authorities = getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        buildAccountPreferenceControllers(context, this /* parent */, authorities, controllers);
        return controllers;
    }

    @Override
    protected boolean shouldSkipForInitialSUW() {
        return true;
    }

    static void buildAutofillPreferenceControllers(
            Context context,
            List<AbstractPreferenceController> controllers,
            boolean isWorkProfile,
            boolean isPrivateSpace) {
        if (CredentialManager.isServiceEnabled(context)) {
            controllers.add(new DefaultCombinedPreferenceController(
                    context, isWorkProfile, isPrivateSpace));
            controllers.add(new DefaultWorkCombinedPreferenceController(context));
            controllers.add(new DefaultPrivateCombinedPreferenceController(context));
        } else {
            controllers.add(new DefaultAutofillPreferenceController(context));
            controllers.add(new DefaultWorkAutofillPreferenceController(context));
            controllers.add(new DefaultPrivateAutofillPreferenceController(context));
        }
    }

    private static void buildAccountPreferenceControllers(
            Context context,
            DashboardFragment parent,
            String[] authorities,
            List<AbstractPreferenceController> controllers) {
        final AccountPreferenceController accountPrefController =
                new AccountPreferenceController(
                        context, parent, authorities, ProfileSelectFragment.ProfileType.ALL);
        if (parent != null) {
            parent.getSettingsLifecycle().addObserver(accountPrefController);
        }
        controllers.add(accountPrefController);
        controllers.add(new AutoSyncDataPreferenceController(context, parent));
        controllers.add(new AutoSyncPersonalDataPreferenceController(context, parent));
        controllers.add(new AutoSyncWorkDataPreferenceController(context, parent));
        controllers.add(new AutoSyncPrivateDataPreferenceController(context, parent));
    }

    private static int getPreferenceLayoutResId(Context context) {
        return (context != null && CredentialManager.isServiceEnabled(context))
                ? R.xml.accounts_dashboard_settings_credman
                : R.xml.accounts_dashboard_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = getPreferenceLayoutResId(context);
                    return List.of(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    final List<AbstractPreferenceController> controllers = new ArrayList<>();
                    buildAccountPreferenceControllers(
                            context, null /* parent */, null /* authorities*/, controllers);
                    buildAutofillPreferenceControllers(context, controllers, false, false);
                    return controllers;
                }

                @SuppressWarnings("MissingSuperCall") // TODO: Fix me
                @Override
                public List<SearchIndexableRaw> getDynamicRawDataToIndex(
                        Context context, boolean enabled) {
                    final List<SearchIndexableRaw> indexRaws = new ArrayList<>();
                    final UserManager userManager =
                            (UserManager) context.getSystemService(Context.USER_SERVICE);
                    final List<UserInfo> profiles = userManager.getProfiles(UserHandle.myUserId());
                    for (final UserInfo userInfo : profiles) {
                        if (userInfo.isManagedProfile()) {
                            return indexRaws;
                        }
                    }

                    final AccountManager accountManager = AccountManager.get(context);
                    final Account[] accounts = accountManager.getAccounts();
                    for (Account account : accounts) {
                        final SearchIndexableRaw raw = new SearchIndexableRaw(context);
                        raw.key = AccountTypePreference.buildKey(account);
                        raw.title = account.name;
                        indexRaws.add(raw);
                    }

                    return indexRaws;
                }
            };
}
