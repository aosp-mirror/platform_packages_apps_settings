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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AccountSyncPreferenceControllerTest {

    @Test
    public void handlePreferenceTreeClick_shouldStartFragment() {
        final ShadowApplication application = ShadowApplication.getInstance();
        final Context context = application.getApplicationContext();
        final Preference preference = new Preference(context);
        preference.setKey("account_sync");

        final AccountSyncPreferenceController controller =
                new AccountSyncPreferenceController(context);
        controller.init(new Account("acct1", "type1"), mock(UserHandle.class));
        controller.handlePreferenceTreeClick(preference);

        final Intent nextActivity = application.getNextStartedActivity();

        assertThat(nextActivity.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AccountSyncSettings.class.getName());
        assertThat(nextActivity.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.account_sync_title);
    }

}
