/*
 * Copyright (C) 2023 The Android Open Source Project
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
import com.android.settings.users.AutoSyncPrivateDataPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class AccountPrivateDashboardFragment extends DashboardFragment {
    private static final String TAG = "AccountPrivateFrag";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCOUNT_PRIVATE;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        if (this.getContext() != null && CredentialManager.isServiceEnabled(this.getContext())) {
            return R.xml.accounts_private_dashboard_settings_credman;
        }
        return R.xml.accounts_private_dashboard_settings;
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
            cmpp.init(this, getFragmentManager(), getIntent(), delegate, /*isWorkProfile=*/false);
        } else {
            getSettingsLifecycle().addObserver(use(PasswordsPreferenceController.class));
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        buildAutofillPreferenceControllers(context, controllers);
        final String[] authorities = getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        buildAccountPreferenceControllers(context, authorities, controllers);
        return controllers;
    }

    private void buildAccountPreferenceControllers(
            Context context,
            String[] authorities,
            List<AbstractPreferenceController> controllers) {
        final AccountPreferenceController accountPrefController =
                new AccountPreferenceController(
                        context,
                        this,
                        authorities,
                        ProfileSelectFragment.ProfileType.PRIVATE);
        controllers.add(accountPrefController);
        controllers.add(new AutoSyncDataPreferenceController(context, this));
        controllers.add(new AutoSyncPrivateDataPreferenceController(context, this));
    }
}
