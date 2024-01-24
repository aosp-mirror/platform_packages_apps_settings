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

package com.android.settings.testutils.shadow;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;

import androidx.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Implements(AccountManager.class)
public class ShadowAccountManager extends org.robolectric.shadows.ShadowAccountManager {

    private static final Map<String, AuthenticatorDescription> sAuthenticators = new HashMap<>();
    private static final Map<Integer, List<Account>> sAccountsByUserId = new HashMap<>();

    @Implementation
    protected AuthenticatorDescription[] getAuthenticatorTypesAsUser(int userId) {
        return sAuthenticators.values().toArray(new AuthenticatorDescription[sAuthenticators.size()]);
    }

    @Override
    public void addAuthenticator(AuthenticatorDescription authenticator) {
        sAuthenticators.put(authenticator.type, authenticator);
    }

    public static void reset() {
        sAuthenticators.clear();
        sAccountsByUserId.clear();
    }

    @Implementation @NonNull
    protected Account[] getAccountsAsUser(int userId) {
        if (sAccountsByUserId.containsKey(userId)) {
            return sAccountsByUserId.get(userId).toArray(new Account[0]);
        } else {
            return new Account[0];
        }
    }

    public static void addAccountForUser(int userId, Account account) {
        if (!sAccountsByUserId.containsKey(userId)) {
            sAccountsByUserId.put(userId, new ArrayList<>());
        }
        sAccountsByUserId.get(userId).add(account);
    }
}
