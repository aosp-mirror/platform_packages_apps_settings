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

import static android.provider.ContactsContract.RawContacts.DefaultAccount;

import android.accounts.Account;
import android.content.Context;
import android.os.UserHandle;
import android.provider.ContactsContract.RawContacts.DefaultAccount.DefaultAccountAndState;
import android.provider.Flags;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.accounts.AuthenticatorHelper;

/**
 * A preference controller handling the logic for updating summary of contacts default account.
 */
public class ContactsStoragePreferenceController extends BasePreferenceController {
    private static final String TAG = "ContactsStorageController";

    private final AuthenticatorHelper mAuthenticatorHelper;

    private DefaultAccountAndState mCurrentDefaultAccountAndState;

    public ContactsStoragePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAuthenticatorHelper = new AuthenticatorHelper(mContext,
                new UserHandle(UserHandle.myUserId()), null);
        try {
            mCurrentDefaultAccountAndState =
                    DefaultAccount.getDefaultAccountForNewContacts(mContext.getContentResolver());
        } catch (IllegalStateException e) {
            Log.e(TAG, "The default account is in an invalid state: " + e);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to look up the default account: " + e);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return (Flags.newDefaultAccountApiEnabled()
                && mCurrentDefaultAccountAndState != null) ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        if (mCurrentDefaultAccountAndState != null) {
            int currentDefaultAccountState = mCurrentDefaultAccountAndState.getState();
            Account currentDefaultAccount = mCurrentDefaultAccountAndState.getAccount();
            if (currentDefaultAccountState
                    == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_NOT_SET) {
                return mContext.getResources().getString(
                        R.string.contacts_storage_no_account_set_summary);
            } else if (currentDefaultAccountState
                    == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_LOCAL) {
                return mContext.getResources().getString(
                        R.string.contacts_storage_local_account_summary);
            } else if (currentDefaultAccountState
                    == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_SIM) {
                return mContext.getResources().getString(
                        R.string.sim_card_label);
            } else if (currentDefaultAccountState
                    == DefaultAccountAndState.DEFAULT_ACCOUNT_STATE_CLOUD) {
                String accountTypeLabel = (String) mAuthenticatorHelper.getLabelForType(mContext,
                        currentDefaultAccount.type);
                // If there's no account type, or the account type is the same as the
                // current default account name, just return the account name.
                if (accountTypeLabel == null || accountTypeLabel.equals(
                        currentDefaultAccount.name)) {
                    return currentDefaultAccount.name;
                }
                return accountTypeLabel + " | " + currentDefaultAccount.name;
            }
        }
        return "";
    }
}
