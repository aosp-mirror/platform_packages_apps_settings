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

import static com.android.settings.accounts.TopLevelAccountEntryPreferenceControllerTest
        .ShadowAuthenticationHelper.LABELS;
import static com.android.settings.accounts.TopLevelAccountEntryPreferenceControllerTest
        .ShadowAuthenticationHelper.TYPES;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settingslib.accounts.AuthenticatorHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {TopLevelAccountEntryPreferenceControllerTest.ShadowAuthenticationHelper.class})
public class TopLevelAccountEntryPreferenceControllerTest {

    private TopLevelAccountEntryPreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new TopLevelAccountEntryPreferenceController(mContext, "test_key");
    }

    @After
    public void tearDown() {
        ShadowAuthenticationHelper.reset();
    }

    @Test

    public void updateSummary_hasAccount_shouldDisplayUpTo3AccountTypes() {
        assertThat(mController.getSummary())
                .isEqualTo(LABELS[0] + ", " + LABELS[1] + ", and " + LABELS[2]);
    }

    @Test
    public void updateSummary_noAccount_shouldDisplayDefaultSummary() {
        ShadowAuthenticationHelper.setEnabledAccount(null);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.account_dashboard_default_summary));
    }

    @Test
    public void updateSummary_noAccountTypeLabel_shouldNotDisplayNullEntry() {
        final String[] enabledAccounts = {TYPES[0], "unlabeled_account_type", TYPES[1]};
        ShadowAuthenticationHelper.setEnabledAccount(enabledAccounts);


        // should only show the 2 accounts with labels
        assertThat(mController.getSummary()).isEqualTo(LABELS[0] + " and " + LABELS[1]);
    }

    @Implements(AuthenticatorHelper.class)
    public static class ShadowAuthenticationHelper {

        static final String[] TYPES = {"type1", "type2", "type3", "type4"};
        static final String[] LABELS = {"LABEL1", "LABEL2", "LABEL3", "LABEL4"};
        private static String[] sEnabledAccount = TYPES;

        protected void __constructor__(Context context, UserHandle userHandle,
                AuthenticatorHelper.OnAccountsUpdateListener listener) {
        }

        private static void setEnabledAccount(String[] enabledAccount) {
            sEnabledAccount = enabledAccount;
        }

        @Resetter
        public static void reset() {
            sEnabledAccount = TYPES;
        }

        @Implementation
        protected String[] getEnabledAccountTypes() {
            return sEnabledAccount;
        }

        @Implementation
        protected CharSequence getLabelForType(Context context, final String accountType) {
            if (TextUtils.equals(accountType, TYPES[0])) {
                return LABELS[0];
            } else if (TextUtils.equals(accountType, TYPES[1])) {
                return LABELS[1];
            } else if (TextUtils.equals(accountType, TYPES[2])) {
                return LABELS[2];
            } else if (TextUtils.equals(accountType, TYPES[3])) {
                return LABELS[3];
            }
            return null;
        }
    }
}
