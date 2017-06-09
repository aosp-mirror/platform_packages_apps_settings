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

import android.accounts.Account;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AccountPreferenceTest {

    private Context mContext;
    private Account mAccount;
    private ArrayList<String> mAuthorities;
    private AccountPreference mPreference;

    @Before
    public void setUp() {
        mContext = ShadowApplication.getInstance().getApplicationContext();
        mAccount = new Account("name", "type");
        mAuthorities = new ArrayList<>();
        mAuthorities.add("authority");

        mPreference = spy(new AccountPreference(
                mContext, mAccount, null /* icon */, mAuthorities, false /* showTypeIcon */));
    }

    @Test
    public void setSyncStatus_differentStatus_shouldUpdate() {
        mPreference.setSyncStatus(AccountPreference.SYNC_ERROR, true);
        verify(mPreference).setSummary(R.string.sync_error);
    }

    @Test
    public void setSyncStatus_sameStatus_shouldNotUpdate() {
        // Set it once, should update summary
        mPreference.setSyncStatus(AccountPreference.SYNC_ERROR, true);
        verify(mPreference).setSummary(R.string.sync_error);

        // Set it again, should not update summary
        mPreference.setSyncStatus(AccountPreference.SYNC_ERROR, true);
        verify(mPreference).setSummary(R.string.sync_error);
    }
}
