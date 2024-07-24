/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.android.settings.accounts.AccountDetailDashboardFragment.KEY_ACCOUNT;
import static com.android.settings.accounts.AccountDetailDashboardFragment.KEY_USER_HANDLE;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.LayoutPreference;

public class AccountHeaderPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume {

    private static final String KEY_ACCOUNT_HEADER = "account_header";

    private final Activity mActivity;
    private final PreferenceFragmentCompat mHost;
    private final Account mAccount;
    private final UserHandle mUserHandle;

    private LayoutPreference mHeaderPreference;

    public AccountHeaderPreferenceController(Context context, Lifecycle lifecycle,
            Activity activity, PreferenceFragmentCompat host, Bundle args) {
        super(context);
        mActivity = activity;
        mHost = host;
        if (args != null && args.containsKey(KEY_ACCOUNT)) {
            mAccount = args.getParcelable(KEY_ACCOUNT);
        } else {
            mAccount = null;
        }

        if (args != null && args.containsKey(KEY_USER_HANDLE)) {
            mUserHandle = args.getParcelable(KEY_USER_HANDLE);
        } else {
            mUserHandle = null;
        }
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return mAccount != null && mUserHandle != null;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ACCOUNT_HEADER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mHeaderPreference = screen.findPreference(KEY_ACCOUNT_HEADER);
    }

    @Override
    public void onResume() {
        final AuthenticatorHelper helper = new AuthenticatorHelper(mContext, mUserHandle, null);

        EntityHeaderController
                .newInstance(mActivity, mHost, mHeaderPreference.findViewById(R.id.entity_header))
                .setLabel(mAccount.name)
                .setIcon(helper.getDrawableForType(mContext, mAccount.type))
                .done(true /* rebindButtons */);
    }
}
