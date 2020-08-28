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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.SwitchPreference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSubscriptionManager;

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
    public void getAvailabilityStatus_invalidSubscription_returnAvailableUnsearchable() {
        mController.init(mFragmentManager, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void isDialogNeeded_disableSingleSim_returnFalse() {
        doReturn(true).when(mTelephonyManager).isDataEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        doReturn(1).when(mTelephonyManager).getActiveModemCount();

        assertThat(mController.isDialogNeeded()).isFalse();
    }

    @Test
    public void isDialogNeeded_enableNonDefaultSimInMultiSimMode_returnTrue() {
        doReturn(false).when(mTelephonyManager).isDataEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        doReturn(true).when(mSubscriptionManager).isActiveSubscriptionId(SUB_ID_OTHER);
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_OTHER);
        doReturn(2).when(mTelephonyManager).getActiveModemCount();

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
    public void onPreferenceChange_singleSim_On_shouldEnableData() {
        doReturn(true).when(mTelephonyManager).isDataEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        doReturn(1).when(mTelephonyManager).getActiveModemCount();

        mController.onPreferenceChange(mPreference, true);

        verify(mTelephonyManager).setDataEnabled(true);
    }

    @Test
    public void onPreferenceChange_multiSim_On_shouldEnableData() {
        doReturn(true).when(mTelephonyManager).isDataEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        doReturn(2).when(mTelephonyManager).getActiveModemCount();

        mController.onPreferenceChange(mPreference, true);

        verify(mTelephonyManager).setDataEnabled(true);
    }

    @Test
    public void isChecked_returnUserDataEnabled() {
        mController.init(mFragmentManager, SUB_ID);
        assertThat(mController.isChecked()).isFalse();

        doReturn(true).when(mTelephonyManager).isDataEnabled();
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void updateState_opportunistic_disabled() {
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        mController.init(mFragmentManager, SUB_ID);
        doReturn(true).when(mSubscriptionInfo).isOpportunistic();
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.mobile_data_settings_summary_auto_switch));
    }

    @Test
    public void updateState_notOpportunistic_enabled() {
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        mController.init(mFragmentManager, SUB_ID);
        doReturn(false).when(mSubscriptionInfo).isOpportunistic();
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.mobile_data_settings_summary));
    }
}
