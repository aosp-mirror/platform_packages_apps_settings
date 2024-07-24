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

package com.android.settings.privatespace.autolock;

import static android.provider.Settings.Secure.PRIVATE_SPACE_AUTO_LOCK;
import static android.provider.Settings.Secure.PRIVATE_SPACE_AUTO_LOCK_AFTER_INACTIVITY;
import static android.provider.Settings.Secure.PRIVATE_SPACE_AUTO_LOCK_NEVER;
import static android.provider.Settings.Secure.PRIVATE_SPACE_AUTO_LOCK_ON_DEVICE_LOCK;

import static com.android.settings.privatespace.PrivateSpaceMaintainer.PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Flags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.widget.CandidateInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AutoLockSettingsFragmentTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private AutoLockSettingsFragment mFragment;
    private Context mContext;
    private Resources mResources;
    private ContentResolver mContentResolver;
    @Settings.Secure.PrivateSpaceAutoLockOption private int mOriginalAutoLockValue;

    @Before
    @UiThreadTest
    public void setup() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mFragment = spy(new AutoLockSettingsFragment());
        mContentResolver = mContext.getContentResolver();
        mResources = spy(mContext.getResources());
        mOriginalAutoLockValue =
                Settings.Secure.getInt(
                        mContentResolver,
                        Settings.Secure.PRIVATE_SPACE_AUTO_LOCK,
                        PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL);
        when(mResources.getStringArray(
                        com.android.settings.R.array.private_space_auto_lock_options))
                .thenReturn(
                        new String[] {
                            "Every time device locks", "After 5 minutes of inactivity", "Never"
                        });
        when(mResources.getStringArray(
                        com.android.settings.R.array.private_space_auto_lock_options_values))
                .thenReturn(new String[] {"0", "1", "2"});
        doReturn(mResources).when(mContext).getResources();
    }

    @After
    public void tearDown() {
        Settings.Secure.putInt(
                mContentResolver, Settings.Secure.PRIVATE_SPACE_AUTO_LOCK, mOriginalAutoLockValue);
    }

    @Test
    public void verifyMetricsConstant() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);
        assertThat(mFragment.getMetricsCategory()).isEqualTo(SettingsEnums.PRIVATE_SPACE_SETTINGS);
    }

    @Test
    @UiThreadTest
    public void getCandidates_returnsCandidateInfoListWithAllKeys() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);
        mFragment.onAttach(mContext);

        final List<? extends CandidateInfo> candidates = mFragment.getCandidates();

        assertThat(candidates.size()).isEqualTo(3);

        assertThat(candidates.get(0).getKey()).isEqualTo("0");
        assertThat(candidates.get(1).getKey()).isEqualTo("1");
        assertThat(candidates.get(2).getKey()).isEqualTo("2");
    }

    @Test
    @UiThreadTest
    public void getDefaultKey_returnsStoredAutoLockOptionsValue() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);

        mFragment.onAttach(mContext);

        Settings.Secure.putInt(
                mContentResolver, PRIVATE_SPACE_AUTO_LOCK, PRIVATE_SPACE_AUTO_LOCK_NEVER);
        assertThat(mFragment.getDefaultKey()).isEqualTo("2");

        Settings.Secure.putInt(
                mContentResolver, PRIVATE_SPACE_AUTO_LOCK, PRIVATE_SPACE_AUTO_LOCK_ON_DEVICE_LOCK);
        assertThat(mFragment.getDefaultKey()).isEqualTo("0");

        Settings.Secure.putInt(
                mContentResolver,
                PRIVATE_SPACE_AUTO_LOCK,
                PRIVATE_SPACE_AUTO_LOCK_AFTER_INACTIVITY);
        assertThat(mFragment.getDefaultKey()).isEqualTo("1");
    }

    @Test
    @UiThreadTest
    public void setDefaultKey_storesCorrectAutoLockOptionValue() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);

        mFragment.onAttach(mContext);
        mFragment.setDefaultKey("2");
        assertThat(Settings.Secure.getInt(mContentResolver, PRIVATE_SPACE_AUTO_LOCK, -1))
                .isEqualTo(PRIVATE_SPACE_AUTO_LOCK_NEVER);

        mFragment.setDefaultKey("1");
        assertThat(Settings.Secure.getInt(mContentResolver, PRIVATE_SPACE_AUTO_LOCK, -1))
                .isEqualTo(PRIVATE_SPACE_AUTO_LOCK_AFTER_INACTIVITY);

        mFragment.setDefaultKey("0");
        assertThat(Settings.Secure.getInt(mContentResolver, PRIVATE_SPACE_AUTO_LOCK, -1))
                .isEqualTo(PRIVATE_SPACE_AUTO_LOCK_ON_DEVICE_LOCK);
    }
}
