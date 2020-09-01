/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Activity asking a user to select an account to be set up.
 */
public class ChooseAccountFragment extends DashboardFragment {

    private static final String TAG = "ChooseAccountFragment";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCOUNTS_CHOOSE_ACCOUNT_ACTIVITY;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        final String[] authorities = getIntent().getStringArrayExtra(
                AccountPreferenceBase.AUTHORITIES_FILTER_KEY);
        final String[] accountTypesFilter = getIntent().getStringArrayExtra(
                AccountPreferenceBase.ACCOUNT_TYPES_FILTER_KEY);
        final UserManager userManager = UserManager.get(getContext());
        final UserHandle userHandle = Utils.getSecureTargetUser(getActivity().getActivityToken(),
                userManager, null /* arguments */, getIntent().getExtras());

        use(ChooseAccountPreferenceController.class).initialize(authorities, accountTypesFilter,
                userHandle, getActivity());
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.add_account_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
