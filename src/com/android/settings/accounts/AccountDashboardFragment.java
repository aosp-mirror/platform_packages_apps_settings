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
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.users.AutoSyncDataPreferenceController;
import com.android.settings.users.AutoSyncPersonalDataPreferenceController;
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
        return R.xml.accounts_dashboard_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_user_and_account_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final String[] authorities = getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        return buildPreferenceControllers(context, this /* parent */, authorities);
    }

    @Override
    protected boolean shouldSkipForInitialSUW() {
        return true;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            SettingsPreferenceFragment parent, String[] authorities) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final AccountPreferenceController accountPrefController =
                new AccountPreferenceController(context, parent, authorities,
                        ProfileSelectFragment.ProfileType.ALL);
        if (parent != null) {
            parent.getSettingsLifecycle().addObserver(accountPrefController);
        }
        controllers.add(accountPrefController);
        controllers.add(new AutoSyncDataPreferenceController(context, parent));
        controllers.add(new AutoSyncPersonalDataPreferenceController(context, parent));
        controllers.add(new AutoSyncWorkDataPreferenceController(context, parent));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accounts_dashboard_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(
                            context, null /* parent */, null /* authorities*/);
                }

                @Override
                public List<SearchIndexableRaw> getDynamicRawDataToIndex(Context context,
                        boolean enabled) {
                    final List<SearchIndexableRaw> indexRaws = new ArrayList<>();
                    final UserManager userManager = (UserManager) context.getSystemService(
                            Context.USER_SERVICE);
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
