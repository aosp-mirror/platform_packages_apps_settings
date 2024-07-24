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

package com.android.settings.deviceinfo;

import android.accounts.Account;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accounts.AccountDashboardFragment;
import com.android.settings.accounts.AccountFeatureProvider;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;

public class BrandedAccountPreferenceController extends BasePreferenceController {
    private final AccountFeatureProvider mAccountFeatureProvider;
    private Account[] mAccounts;

    public BrandedAccountPreferenceController(Context context, String key) {
        super(context, key);
        mAccountFeatureProvider = FeatureFactory.getFeatureFactory().getAccountFeatureProvider();
        mAccounts = mAccountFeatureProvider.getAccounts(mContext);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(
                R.bool.config_show_branded_account_in_device_info)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mAccounts != null && mAccounts.length > 0) {
            return AVAILABLE;
        }
        return DISABLED_FOR_USER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference accountPreference = screen.findPreference(getPreferenceKey());
        if (accountPreference != null && (mAccounts == null || mAccounts.length == 0)) {
            screen.removePreference(accountPreference);
            return;
        }

        if (mAccounts.length == 1) {
            accountPreference.setSummary(mAccounts[0].name);
        } else {
            accountPreference.setSummary(getAccountSummary(mAccounts.length));
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        new SubSettingLauncher(mContext)
                .setDestination(AccountDashboardFragment.class.getName())
                .setTitleRes(R.string.account_dashboard_title)
                .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
                .launch();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mAccounts = mAccountFeatureProvider.getAccounts(mContext);
        if (mAccounts == null || mAccounts.length == 0) {
            preference.setVisible(false);
        }
    }

    private String getAccountSummary(int accountNo) {
        return mContext.getResources()
            .getString(R.string.my_device_info_account_preference_summary, accountNo);
    }
}
