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

import static com.google.common.truth.Truth.assertThat;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AccountsSettingsTest {

    private static final String ACCOUNTS = "Accounts";
    private static final String ACCOUNT_TYPE = "com.settingstest.account-prefs";
    private static final String PREF_TITLE = "Test preference for external account";

    private UiDevice mDevice;
    private Context mContext;
    private String mTargetPackage;

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mContext = InstrumentationRegistry.getTargetContext();
        mTargetPackage = mContext.getPackageName();
    }

    @Test
    public void testExternalAccountInfoExists() throws UiObjectNotFoundException {
        // add a test account
        final String testAccountName = "Test Account";
        final Account account = new Account(testAccountName, ACCOUNT_TYPE);
        final AccountManager accountManager = AccountManager.get(mContext);
        final boolean accountAdded =
            accountManager.addAccountExplicitly(account, null /* password */, null /* userdata */);
        assertThat(accountAdded).isTrue();

        // launch Accounts Settings and select the test account
        launchAccountsSettings();
        mDevice.findObject(new UiSelector().text(testAccountName)).click();
        final UiObject testPreference = mDevice.findObject(new UiSelector().text(PREF_TITLE));
        // remove test account
        accountManager.removeAccountExplicitly(account);

        assertThat(testPreference.exists()).isTrue();
    }

    private void launchAccountsSettings() throws UiObjectNotFoundException  {
        // launch settings
        Intent settingsIntent = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(mTargetPackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(settingsIntent);
        // selects Accounts
        final UiScrollable settings = new UiScrollable(
                new UiSelector().packageName(mTargetPackage).scrollable(true));
        final String titleAccounts = ACCOUNTS;
        settings.scrollTextIntoView(titleAccounts);
        mDevice.findObject(new UiSelector().text(titleAccounts)).click();
    }
}
