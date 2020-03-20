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

package com.android.settings.security;

import static android.telephony.TelephonyManager.SIM_STATE_READY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SimLockPreferenceControllerTest {

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigManager mCarrierManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private PreferenceScreen mScreen;

    private SimLockPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE,
                mSubscriptionManager);
        shadowApplication.setSystemService(Context.CARRIER_CONFIG_SERVICE, mCarrierManager);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUserManager);
        shadowApplication.setSystemService(Context.TELEPHONY_SERVICE, mTelephonyManager);
        mController = new SimLockPreferenceController(RuntimeEnvironment.application);
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isAvailable_notAdmin_false() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void isAvailable_simIccNotReady_false() {
        when(mUserManager.isAdminUser()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void isAvailable_carrierConfigDisabled_false() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        setupMockIcc();
        final PersistableBundle pb = new PersistableBundle();
        pb.putBoolean(CarrierConfigManager.KEY_HIDE_SIM_LOCK_SETTINGS_BOOL, true);
        when(mCarrierManager.getConfigForSubId(anyInt())).thenReturn(pb);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void isAvailable_true() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        setupMockIcc();
        final PersistableBundle pb = new PersistableBundle();
        when(mCarrierManager.getConfigForSubId(anyInt())).thenReturn(pb);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void displayPreference_simReady_enablePreference() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void displayPreference_simNotReady_disablePreference() {
        setupMockSimReady();

        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    private void setupMockIcc() {
        final List<SubscriptionInfo> subscriptionInfoList = new ArrayList<>();
        SubscriptionInfo info = mock(SubscriptionInfo.class);
        subscriptionInfoList.add(info);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        when(mTelephonyManager.hasIccCard()).thenReturn(true);
        when(mSubscriptionManager.getActiveSubscriptionInfoList())
                .thenReturn(subscriptionInfoList);
    }

    private void setupMockSimReady() {
        final List<SubscriptionInfo> subscriptionInfoList = new ArrayList<>();
        SubscriptionInfo info = mock(SubscriptionInfo.class);
        subscriptionInfoList.add(info);
        when(mTelephonyManager.getSimState(anyInt())).thenReturn(SIM_STATE_READY);
        when(mSubscriptionManager.getActiveSubscriptionInfoList())
                .thenReturn(subscriptionInfoList);
    }
}
