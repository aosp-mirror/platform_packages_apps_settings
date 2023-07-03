/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class RoamingPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private LifecycleOwner mLifecycleOwner;

    private LifecycleRegistry mLifecycleRegistry;
    private RoamingPreferenceController mController;
    private RestrictedSwitchPreference mPreference;
    private Context mContext;
    private MobileNetworkInfoEntity mMobileNetworkInfoEntity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        doReturn(mCarrierConfigManager).when(mContext).getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mFragmentTransaction).when(mFragmentManager).beginTransaction();

        mPreference = spy(new RestrictedSwitchPreference(mContext));
        mController = spy(
                new RoamingPreferenceController(mContext, "roaming", mLifecycle, mLifecycleOwner,
                        SUB_ID));
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
        mController.init(mFragmentManager, SUB_ID, mMobileNetworkInfoEntity);
        mPreference.setKey(mController.getPreferenceKey());
    }

    private MobileNetworkInfoEntity setupMobileNetworkInfoEntity(String subId,
            boolean isDataRoaming) {
        return new MobileNetworkInfoEntity(subId, false, false, true, false, false, false, false,
                false, false, false, isDataRoaming);
    }

    @Test
    public void getAvailabilityStatus_validSubId_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_invalidSubId_returnUnsearchable() {
        mController.init(mFragmentManager, SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                mMobileNetworkInfoEntity);

        assertThat(mController.getAvailabilityStatus(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID)).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void isDialogNeeded_roamingDisabledWithoutFlag_returnTrue() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL, false);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);
        mMobileNetworkInfoEntity = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), false);
        mController.setMobileNetworkInfoEntity(mMobileNetworkInfoEntity);

        assertThat(mController.isDialogNeeded()).isTrue();
    }

    @Test
    public void isDialogNeeded_roamingEnabled_returnFalse() {
        mMobileNetworkInfoEntity = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), true);
        mController.setMobileNetworkInfoEntity(mMobileNetworkInfoEntity);

        assertThat(mController.isDialogNeeded()).isFalse();
    }

    @Test
    @UiThreadTest
    public void setChecked_needDialog_showDialog() {
        mMobileNetworkInfoEntity = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), false);
        mController.setMobileNetworkInfoEntity(mMobileNetworkInfoEntity);
        doReturn(null).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        mController.setChecked(true);

        verify(mFragmentManager).beginTransaction();
    }

    @Test
    public void updateState_invalidSubId_disabled() {
        mMobileNetworkInfoEntity = setupMobileNetworkInfoEntity(
                String.valueOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID), false);
        mController.setMobileNetworkInfoEntity(mMobileNetworkInfoEntity);
        mController.init(mFragmentManager, SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                mMobileNetworkInfoEntity);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_validSubId_enabled() {
        mMobileNetworkInfoEntity = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), true);
        mController.setMobileNetworkInfoEntity(mMobileNetworkInfoEntity);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_isNotDisabledByAdmin_shouldInvokeSetEnabled() {
        when(mPreference.isDisabledByAdmin()).thenReturn(false);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(anyBoolean());
    }

    @Test
    public void updateState_isDisabledByAdmin_shouldNotInvokeSetEnabled() {
        when(mPreference.isDisabledByAdmin()).thenReturn(true);

        mController.updateState(mPreference);

        verify(mPreference, never()).setEnabled(anyBoolean());
    }

    @Test
    public void getAvailabilityStatus_carrierConfigIsNull_shouldReturnAvailable() {
        doReturn(null).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_forceHomeNetworkIsFalse_shouldReturnAvailable() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL, false);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_forceHomeNetworkIsTrue_shouldReturnConditionallyAvailable() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL, true);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}
