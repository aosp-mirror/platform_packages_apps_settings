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

package com.android.settings.network.telephony;

import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSubscriptionManager;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.SwitchPreference;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSubscriptionManager.class)
public class MobileDataPreferenceControllerTest {
    private static final int SUB_ID = 2;
    private static final int SUB_ID_OTHER = 3;

    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private FragmentTransaction mFragmentTransaction;

    private MobileDataPreferenceController mController;
    private SwitchPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mFragmentTransaction).when(mFragmentManager).beginTransaction();

        mPreference = new SwitchPreference(mContext);
        mController = new MobileDataPreferenceController(mContext, "mobile_data");
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID);
        mController.init(mFragmentManager, SUB_ID);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_invalidSubscription_returnUnavailable() {
        mController.init(mFragmentManager, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isDialogNeeded_disableSingleSim_returnTrue() {
        doReturn(true).when(mTelephonyManager).isDataEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        doReturn(1).when(mTelephonyManager).getSimCount();

        assertThat(mController.isDialogNeeded()).isTrue();
        assertThat(mController.mDialogType).isEqualTo(MobileDataDialogFragment.TYPE_DISABLE_DIALOG);
    }

    @Test
    public void isDialogNeeded_enableNonDefaultSimInMultiSimMode_returnTrue() {
        doReturn(false).when(mTelephonyManager).isDataEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        doReturn(true).when(mSubscriptionManager).isActiveSubscriptionId(SUB_ID_OTHER);
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_OTHER);
        doReturn(2).when(mTelephonyManager).getSimCount();

        assertThat(mController.isDialogNeeded()).isTrue();
        assertThat(mController.mDialogType).isEqualTo(
                MobileDataDialogFragment.TYPE_MULTI_SIM_DIALOG);
    }

    @Test
    public void handlePreferenceTreeClick_needDialog_showDialog() {
        mController.mNeedDialog = true;

        mController.handlePreferenceTreeClick(mPreference);

        verify(mFragmentManager).beginTransaction();
    }

    @Test
    public void onPreferenceChange_needDialog_doNothing() {
        doReturn(true).when(mTelephonyManager).isDataEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        doReturn(1).when(mTelephonyManager).getSimCount();

        mController.onPreferenceChange(mPreference, true);

        verify(mTelephonyManager, never()).setDataEnabled(true);
    }

    @Test
    public void onPreferenceChange_notNeedDialog_update() {
        doReturn(true).when(mTelephonyManager).isDataEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        doReturn(2).when(mTelephonyManager).getSimCount();

        mController.onPreferenceChange(mPreference, true);

        verify(mTelephonyManager).setDataEnabled(true);
    }
}
