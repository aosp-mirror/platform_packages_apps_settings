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
 * limitations under the License
 */

package com.android.settings.network;

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.network.telephony.DataDuringCallsPreferenceController;

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
public class DataDuringCallsPreferenceControllerTest {
    private static final String PREF_KEY = "pref_key";
    private static final int SUB_ID_1 = 111;
    private static final int SUB_ID_2 = 222;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private SwitchPreference mSwitchPreference;
    private DataDuringCallsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(eq(TelephonyManager.class));
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        mSwitchPreference = new SwitchPreference(mContext);
        when(mPreferenceScreen.findPreference(PREF_KEY)).thenReturn(mSwitchPreference);
        mController = new DataDuringCallsPreferenceController(mContext, PREF_KEY);
        mController.init(mLifecycle, SUB_ID_1);
    }

    @Test
    public void getAvailabilityStatus_noInit_notAvailable() {
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_1);
        DataDuringCallsPreferenceController controller =
                new DataDuringCallsPreferenceController(mContext, PREF_KEY);

        // note that we purposely don't call init first on the controller
        assertThat(controller.getAvailabilityStatus(INVALID_SUBSCRIPTION_ID)).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_defaultForData_notAvailable() {
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_1);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mController.isAvailable()).isFalse();
        assertThat(mSwitchPreference.isVisible()).isFalse();
    }

    @Test
    public void  displayPreference_notDefaultForData_available() {
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_2);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mSwitchPreference.isVisible()).isTrue();
    }

    @Test
    public void onSubscriptionsChanged_becomesDefaultForData_notAvailable() {
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_2);

        mController.displayPreference(mPreferenceScreen);
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_1);
        mController.onSubscriptionsChanged();

        assertThat(mController.isAvailable()).isFalse();
        assertThat(mSwitchPreference.isVisible()).isFalse();
    }

    @Test
    public void onSubscriptionsChanged_noLongerDefaultForData_available() {
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_1);

        mController.displayPreference(mPreferenceScreen);
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_2);
        mController.onSubscriptionsChanged();

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mSwitchPreference.isVisible()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_mobileDataChangWithDefaultDataSubId_returnUnavailable() {
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_1);

        mController.refreshPreference();

        assertThat(mController.getAvailabilityStatus(SUB_ID_1))
                .isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_mobileDataChangWithoutDefaultDataSubId_returnAvailable() {
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID_1);

        mController.displayPreference(mPreferenceScreen);
        mController.refreshPreference();

        assertThat(mController.getAvailabilityStatus(SUB_ID_2)).isEqualTo(AVAILABLE);
    }
}
