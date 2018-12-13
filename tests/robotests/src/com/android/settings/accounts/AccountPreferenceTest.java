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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.accounts.Account;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class AccountPreferenceTest {

    private AccountPreference mPreference;

    @Before
    public void setUp() {
        final ArrayList<String> authorities = new ArrayList<>();
        authorities.add("authority");

        mPreference = spy(new AccountPreference(
            RuntimeEnvironment.application,
            new Account("name", "type"),
            null /* icon */,
            authorities,
            false /* showTypeIcon */)
        );
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
