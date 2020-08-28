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

import android.accounts.Account;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncAdapterType;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;

public class AccountSyncPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, AuthenticatorHelper.OnAccountsUpdateListener {

    private static final String TAG = "AccountSyncController";
    private static final String KEY_ACCOUNT_SYNC = "account_sync";

    private Account mAccount;
    private UserHandle mUserHandle;
    private Preference mPreference;

    public AccountSyncPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY_ACCOUNT_SYNC.equals(preference.getKey())) {
            return false;
        }
        final Bundle args = new Bundle();
        args.putParcelable(AccountSyncSettings.ACCOUNT_KEY, mAccount);
        args.putParcelable(EXTRA_USER, mUserHandle);
        new SubSettingLauncher(mContext)
                .setDestination(AccountSyncSettings.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(SettingsEnums.ACCOUNT)
                .setTitleRes(R.string.account_sync_title)
                .launch();

        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ACCOUNT_SYNC;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        updateSummary(preference);
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        updateSummary(mPreference);
    }

    public void init(Account account, UserHandle userHandle) {
        mAccount = account;
        mUserHandle = userHandle;
    }

    @VisibleForTesting
    void updateSummary(Preference preference) {
        if (mAccount == null) {
            return;
        }
        final int userId = mUserHandle.getIdentifier();
        final SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(userId);
        int total = 0;
        int enabled = 0;
        if (syncAdapters != null) {
            for (int i = 0, n = syncAdapters.length; i < n; i++) {
                final SyncAdapterType sa = syncAdapters[i];
                if (!sa.accountType.equals(mAccount.type) || !sa.isUserVisible()) {
                    continue;
                }
                final int syncState =
                        ContentResolver.getIsSyncableAsUser(mAccount, sa.authority, userId);
                if (syncState > 0) {
                    total++;
                    final boolean syncEnabled = ContentResolver.getSyncAutomaticallyAsUser(
                            mAccount, sa.authority, userId);
                    final boolean oneTimeSyncMode =
                            !ContentResolver.getMasterSyncAutomaticallyAsUser(userId);
                    if (oneTimeSyncMode || syncEnabled) {
                        enabled++;
                    }
                }
            }
        }
        if (enabled == 0) {
            preference.setSummary(R.string.account_sync_summary_all_off);
        } else if (enabled == total) {
            preference.setSummary(R.string.account_sync_summary_all_on);
        } else {
            preference.setSummary(
                    mContext.getString(R.string.account_sync_summary_some_on, enabled, total));
        }
    }
}
