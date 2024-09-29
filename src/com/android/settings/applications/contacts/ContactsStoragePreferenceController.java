
/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.applications.contacts;

import android.accounts.Account;
import android.content.Context;
import android.os.UserHandle;
import android.provider.ContactsContract;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settingslib.accounts.AuthenticatorHelper;

/**
 * A preference controller handling the logic for updating summary of contacts default account.
 */
public class ContactsStoragePreferenceController extends BasePreferenceController {
    private static final String TAG = "ContactsStorageController";

    private final AuthenticatorHelper mAuthenticatorHelper;

    public ContactsStoragePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAuthenticatorHelper = new AuthenticatorHelper(mContext,
                new UserHandle(UserHandle.myUserId()), null);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableContactsDefaultAccountInSettings()
            ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        Account currentDefaultAccount =
                ContactsContract.Settings.getDefaultAccount(mContext.getContentResolver());
        if (currentDefaultAccount == null) {
            return mContext.getResources().getString(
                    R.string.contacts_storage_no_account_set);
        }
        String accountTypeLabel = (String) mAuthenticatorHelper.getLabelForType(mContext,
                currentDefaultAccount.type);
        // If there's no account type, or the account type is the same as the
        // current default account name, just return the account name.
        if (accountTypeLabel == null || accountTypeLabel.equals(currentDefaultAccount.name)) {
            return currentDefaultAccount.name;
        }
        return accountTypeLabel + " | " + currentDefaultAccount.name;
    }
}
