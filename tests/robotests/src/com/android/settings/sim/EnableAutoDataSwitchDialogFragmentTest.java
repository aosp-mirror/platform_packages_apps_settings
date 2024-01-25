/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.sim;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class EnableAutoDataSwitchDialogFragmentTest
        extends SimDialogFragmentTestBase<EnableAutoDataSwitchDialogFragment> {
    private static final String SUMMARY = "fake summary";
    private static final String WARNING = "fake warning";

    // Mock
    @Mock
    private Context mContext;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        super.setUp();
        mFragment = spy(EnableAutoDataSwitchDialogFragment.newInstance());
        doReturn(mContext).when(mFragment).getContext();

        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mSubscriptionManager).when(mSubscriptionManager).createForAllUserProfiles();
        doReturn(mTelephonyManager).when(mContext).getSystemService(TelephonyManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(anyInt());
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);

        doReturn(SIM1_ID).when(mFragment).getDefaultDataSubId();
        doReturn(Arrays.asList(mSim1, mSim2)).when(mSubscriptionManager)
                .getActiveSubscriptionInfoList();
        doReturn(true).when(mUserManager)
                .isManagedProfile(UserHandle.MIN_SECONDARY_USER_ID);

        doReturn(SUMMARY).when(mContext).getString(
                eq(R.string.enable_auto_data_switch_dialog_message), any());
        doReturn(WARNING).when(mContext).getString(
                R.string.auto_data_switch_dialog_managed_profile_warning);
    }

    @After
    public void tearDown() {
        mFragment = null;
    }

    @Test
    public void updateDialog_getMessage_noDdsExists() {
        doReturn(SubscriptionManager.INVALID_SUBSCRIPTION_ID).when(mFragment).getDefaultDataSubId();
        String msg = mFragment.getMessage();
        assertThat(msg).isEqualTo(null);
    }

    @Test
    public void updateDialog_getMessage_noBackupSubExists() {
        doReturn(Collections.singletonList(mSim1)).when(mSubscriptionManager)
                .getActiveSubscriptionInfoList();
        String msg = mFragment.getMessage();
        assertThat(msg).isEqualTo(null);
    }

    @Test
    public void updateDialog_getMessage_autoSwitchAlreadyEnabled() {
        doReturn(true).when(mTelephonyManager).isMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH);
        String msg = mFragment.getMessage();
        assertThat(msg).isEqualTo(null);
    }

    @Test
    public void updateDialog_getMessage_noManagedProfile() {
        UserHandle userHandle = UserHandle.of(UserHandle.USER_NULL);
        UserHandle userHandle2 = UserHandle.of(UserHandle.USER_SYSTEM);
        doReturn(userHandle).when(mSubscriptionManager).getSubscriptionUserHandle(SIM1_ID);
        doReturn(userHandle2).when(mSubscriptionManager).getSubscriptionUserHandle(SIM2_ID);
        String msg = mFragment.getMessage();
        assertThat(msg).contains(SUMMARY);
        assertThat(msg).doesNotContain(WARNING);
    }

    @Test
    public void updateDialog_getMessage_hasManagedProfile() {
        UserHandle userHandle = UserHandle.of(UserHandle.USER_NULL);
        UserHandle userHandle2 = UserHandle.of(UserHandle.MIN_SECONDARY_USER_ID);
        doReturn(userHandle).when(mSubscriptionManager).getSubscriptionUserHandle(SIM1_ID);
        doReturn(userHandle2).when(mSubscriptionManager).getSubscriptionUserHandle(SIM2_ID);
        String msg = mFragment.getMessage();
        assertThat(msg).contains(SUMMARY);
        assertThat(msg).contains(WARNING);
    }

    @Test
    public void updateDialog_getMessage_BothManagedProfile() {
        UserHandle userHandle = UserHandle.of(UserHandle.MIN_SECONDARY_USER_ID);
        UserHandle userHandle2 = UserHandle.of(UserHandle.MIN_SECONDARY_USER_ID);
        doReturn(userHandle).when(mSubscriptionManager).getSubscriptionUserHandle(SIM1_ID);
        doReturn(userHandle2).when(mSubscriptionManager).getSubscriptionUserHandle(SIM2_ID);
        String msg = mFragment.getMessage();
        assertThat(msg).contains(SUMMARY);
        assertThat(msg).doesNotContain(WARNING);
    }
}
