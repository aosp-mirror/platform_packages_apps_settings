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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.content.Context;
import android.os.UserHandle;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowContentResolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowContentResolver.class})
public class AccountSyncSettingsTest {
    private Context mContext;
    private AccountSyncSettings mAccountSyncSettings;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mAccountSyncSettings = spy(new TestAccountSyncSettings(mContext));
    }

    @After
    public void tearDown() {
        ShadowContentResolver.reset();
    }

    @Test
    public void onPreferenceTreeClick_nullAuthority_shouldNotCrash() {
        when(mAccountSyncSettings.getActivity()).thenReturn(mock(FragmentActivity.class));
        final SyncStateSwitchPreference preference = new SyncStateSwitchPreference(mContext,
                new Account("acct1", "type1"), "" /* authority */, "testPackage", 1 /* uid */);
        preference.setOneTimeSyncMode(false);
        ReflectionHelpers.setField(mAccountSyncSettings, "mUserHandle", UserHandle.CURRENT);

        mAccountSyncSettings.onPreferenceTreeClick(preference);
        // no crash
    }

    @Test
    public void enabledSyncNowMenu_noSyncStateSwitchPreference_returnFalse() {
        assertThat(mAccountSyncSettings.enabledSyncNowMenu()).isFalse();
    }

    @Test
    public void enabledSyncNowMenu_addSyncStateSwitchPreferenceAndSwitchOn_returnTrue() {
        final SyncStateSwitchPreference preference = new SyncStateSwitchPreference(mContext,
                new Account("acct1", "type1"), "" /* authority */, "testPackage", 1 /* uid */);
        preference.setChecked(true);
        mAccountSyncSettings.getPreferenceScreen().addPreference(preference);

        assertThat(mAccountSyncSettings.enabledSyncNowMenu()).isTrue();
    }

    @Test
    public void enabledSyncNowMenu_addSyncStateSwitchPreferenceAndSwitchOff_returnFalse() {
        final SyncStateSwitchPreference preference = new SyncStateSwitchPreference(mContext,
                new Account("acct1", "type1"), "" /* authority */, "testPackage", 1 /* uid */);
        preference.setChecked(false);
        mAccountSyncSettings.getPreferenceScreen().addPreference(preference);

        assertThat(mAccountSyncSettings.enabledSyncNowMenu()).isFalse();
    }

    public static class TestAccountSyncSettings extends AccountSyncSettings {
        private PreferenceScreen mScreen;

        public TestAccountSyncSettings(Context context) {
            final PreferenceManager preferenceManager = new PreferenceManager(context);
            mScreen = preferenceManager.createPreferenceScreen(context);
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mScreen;
        }
    }
}
