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
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;

public class AccountSyncPreferenceController extends PreferenceController {

    private static final String TAG = "AccountSyncController";
    private static final String KEY_ACCOUNT_SYNC = "account_sync";

    private Account mAccount;
    private UserHandle mUserHandle;

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
        Utils.startWithFragment(mContext, AccountSyncSettings.class.getName(), args, null, 0,
                R.string.account_sync_title, null, MetricsProto.MetricsEvent.ACCOUNT);

        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ACCOUNT_SYNC;
    }

    public void init(Account account, UserHandle userHandle) {
        mAccount = account;
        mUserHandle = userHandle;
    }
}
