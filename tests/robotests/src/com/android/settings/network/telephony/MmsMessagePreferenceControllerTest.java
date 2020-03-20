/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;

import androidx.preference.SwitchPreference;

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
public class MmsMessagePreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    private MmsMessagePreferenceController mController;
    private SwitchPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID)).thenReturn(mTelephonyManager);

        mPreference = new SwitchPreference(mContext);
        mController = new MmsMessagePreferenceController(mContext, "mms_message");
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID);
        mController.init(SUB_ID);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_invalidSubscription_returnUnavailable() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_mobileDataOn_returnUnavailable() {
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus(SUB_ID)).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_meteredOff_returnUnavailable() {
        when(mTelephonyManager.isApnMetered(ApnSetting.TYPE_MMS)).thenReturn(false);

        assertThat(mController.getAvailabilityStatus(SUB_ID)).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_mobileDataOffWithValidSubId_returnAvailable() {
        mController.init(SUB_ID);
        when(mTelephonyManager.isDataEnabled()).thenReturn(false);
        when(mTelephonyManager.isApnMetered(ApnSetting.TYPE_MMS)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus(SUB_ID)).isEqualTo(AVAILABLE);
    }

    @Test
    public void isChecked_returnDataFromTelephonyManager() {
        when(mTelephonyManager.isDataEnabledForApn(ApnSetting.TYPE_MMS)).thenReturn(false);
        assertThat(mController.isChecked()).isFalse();

        when(mTelephonyManager.isDataEnabledForApn(ApnSetting.TYPE_MMS)).thenReturn(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_setDataIntoSubscriptionManager() {
        mController.setChecked(true);
        verify(mTelephonyManager).setAlwaysAllowMmsData(true);

        mController.setChecked(false);
        verify(mTelephonyManager).setAlwaysAllowMmsData(false);
    }
}
