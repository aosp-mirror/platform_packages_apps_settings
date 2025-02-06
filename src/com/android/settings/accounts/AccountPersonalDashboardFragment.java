/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.accounts.AccountDashboardFragment.buildAutofillPreferenceControllers;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.credentials.CredentialManager;

import com.android.settings.R;
import com.android.settings.applications.autofill.PasswordsPreferenceController;
import com.android.settings.applications.credentials.CredentialManagerPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.users.AutoSyncDataPreferenceController;
import com.android.settings.users.AutoSyncPersonalDataPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/** Account Setting page for personal profile. */
public class AccountPersonalDashboardFragment extends DashboardFragment {
    private static final String TAG = "AccountPersonalFrag";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCOUNT_PERSONAL;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        if (this.getContext() != null && CredentialManager.isServiceEnabled(this.getContext())) {
            return R.xml.accounts_personal_dashboard_settings_credman;
        }
        return R.xml.accounts_personal_dashboard_settings;
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
            cmpp.init(this, getFragmentManager(),
                    getIntent(), delegate, /*isWorkProfile=*/false, /*isPrivateSpace=*/ false);
        } else {
            getSettingsLifecycle().addObserver(use(PasswordsPreferenceController.class));
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        buildAutofillPreferenceControllers(
                context, controllers, /*isWorkProfile=*/ false, /*isPrivateSpace=*/ false);
        final String[] authorities = getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        buildAccountPreferenceControllers(context, this /* parent */, authorities, controllers);
        return controllers;
    }

    private static void buildAccountPreferenceControllers(
            Context context,
            DashboardFragment parent,
            String[] authorities,
            List<AbstractPreferenceController> controllers) {
        final AccountPreferenceController accountPrefController =
                new AccountPreferenceController(
                        context, parent, authorities, ProfileSelectFragment.ProfileType.PERSONAL);
        if (parent != null) {
            parent.getSettingsLifecycle().addObserver(accountPrefController);
        }
        controllers.add(accountPrefController);
        controllers.add(new AutoSyncDataPreferenceController(context, parent));
        controllers.add(new AutoSyncPersonalDataPreferenceController(context, parent));
    }

    // TODO: b/141601408. After featureFlag settings_work_profile is launched, unmark this
    //    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
    //            new BaseSearchIndexProvider(R.xml.accounts_personal_dashboard_settings) {
    //
    //                @Override
    //                public List<AbstractPreferenceController> createPreferenceControllers(
    //                        Context context) {
    //                    ..Add autofill here too..
    //                    return buildPreferenceControllers(
    //                            context, null /* parent */, null /* authorities*/);
    //                }
    //            };
}
