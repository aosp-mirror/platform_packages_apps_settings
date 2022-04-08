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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
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

    private RoamingPreferenceController mController;
    private RestrictedSwitchPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mFragmentTransaction).when(mFragmentManager).beginTransaction();

        mPreference = spy(new RestrictedSwitchPreference(mContext));
        mController = spy(new RoamingPreferenceController(mContext, "roaming"));
        mController.init(mFragmentManager, SUB_ID);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_validSubId_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_invalidSubId_returnUnsearchable() {
        mController.init(mFragmentManager, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void isDialogNeeded_roamingDisabledWithoutFlag_returnTrue() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL, false);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);
        doReturn(false).when(mTelephonyManager).isDataRoamingEnabled();

        assertThat(mController.isDialogNeeded()).isTrue();
    }

    @Test
    public void isDialogNeeded_roamingEnabled_returnFalse() {
        doReturn(true).when(mTelephonyManager).isDataRoamingEnabled();

        assertThat(mController.isDialogNeeded()).isFalse();
    }

    @Test
    public void setChecked_needDialog_showDialog() {
        doReturn(true).when(mController).isDialogNeeded();

        mController.setChecked(true);

        verify(mFragmentManager).beginTransaction();
    }

    @Test
    public void updateState_invalidSubId_disabled() {
        mController.init(mFragmentManager, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_validSubId_enabled() {
        doReturn(true).when(mTelephonyManager).isDataRoamingEnabled();

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
}
