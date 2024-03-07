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

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.os.UserHandle;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.shadow.ShadowAccountManager;
import com.android.settings.testutils.shadow.ShadowContentResolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
public class AccountSyncPreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private AccountManager mAccountManager;

    private Activity mActivity;
    private AccountSyncPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(Activity.class);
        ShadowApplication.getInstance().setSystemService(Context.ACCOUNT_SERVICE, mAccountManager);

        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(
                new AuthenticatorDescription[0]);
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(new Account[0]);

        mPreference = new Preference(mActivity);
        mPreference.setKey("account_sync");

        mController = new AccountSyncPreferenceController(mActivity);
        mController.init(new Account("acct1", "type1"), new UserHandle(3));
    }

    @After
    public void tearDown() {
        ShadowContentResolver.reset();
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartFragment() {
        mController.handlePreferenceTreeClick(mPreference);

        final Intent nextActivity = ShadowApplication.getInstance().getNextStartedActivity();

        assertThat(nextActivity.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AccountSyncSettings.class.getName());
        assertThat(nextActivity.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.account_sync_title);
    }

    @Test
    public void updateSummary_adapterInvisible_shouldNotCount() {
        SyncAdapterType syncAdapterType = new SyncAdapterType("authority" /* authority */,
                "type1" /* accountType */, false /* userVisible */, true /* supportsUploading */);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mActivity.getString(R.string.account_sync_summary_all_off));
    }

    @Test
    public void updateSummary_notSameAccountType_shouldNotCount() {
        SyncAdapterType syncAdapterType = new SyncAdapterType("authority" /* authority */,
                "type5" /* accountType */, true /* userVisible */, true /* supportsUploading */);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mActivity.getString(R.string.account_sync_summary_all_off));
    }

    @Test
    public void updateSummary_notSyncable_shouldNotCount() {
        SyncAdapterType syncAdapterType = new SyncAdapterType("authority" /* authority */,
                "type1" /* accountType */, true /* userVisible */, true /* supportsUploading */);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);
        ShadowContentResolver.setSyncable("authority", 0);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mActivity.getString(R.string.account_sync_summary_all_off));
    }

    @Test
    public void updateSummary_syncDisabled_shouldNotCount() {
        SyncAdapterType syncAdapterType = new SyncAdapterType("authority" /* authority */,
                "type1" /* accountType */, true /* userVisible */, true /* supportsUploading */);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);
        ShadowContentResolver.setSyncAutomatically("authority", false);
        ShadowContentResolver.setMasterSyncAutomatically(3, true);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mActivity.getString(R.string.account_sync_summary_all_off));
    }

    @Test
    public void updateSummary_syncEnabled_shouldCount() {
        SyncAdapterType syncAdapterType = new SyncAdapterType("authority" /* authority */,
                "type1" /* accountType */, true /* userVisible */, true /* supportsUploading */);
        SyncAdapterType[] syncAdapters = {syncAdapterType};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mActivity.getString(R.string.account_sync_summary_all_on));
    }

    @Test
    public void updateSummary_multipleSyncAdapters_shouldSetSummary() {
        SyncAdapterType syncAdapterType1 = new SyncAdapterType("authority1" /* authority */,
                "type1" /* accountType */, true /* userVisible */, true /* supportsUploading */);
        SyncAdapterType syncAdapterType2 = new SyncAdapterType("authority2" /* authority */,
                "type1" /* accountType */, true /* userVisible */, true /* supportsUploading */);
        SyncAdapterType syncAdapterType3 = new SyncAdapterType("authority3" /* authority */,
                "type1" /* accountType */, true /* userVisible */, true /* supportsUploading */);
        SyncAdapterType syncAdapterType4 = new SyncAdapterType("authority4" /* authority */,
                "type1" /* accountType */, true /* userVisible */, true /* supportsUploading */);
        SyncAdapterType[] syncAdapters =
                {syncAdapterType1, syncAdapterType2, syncAdapterType3, syncAdapterType4};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        ShadowContentResolver.setSyncAutomatically("authority4", false);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mActivity.getString(R.string.account_sync_summary_some_on, 3, 4));
    }
}
