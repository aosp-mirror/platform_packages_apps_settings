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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.Settings.MobileNetworkActivity;
import com.android.settings.network.helper.SubscriptionAnnotation;
import com.android.settings.network.helper.SubscriptionGrouping;
import com.android.settings.widget.AddPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class MobileNetworkSummaryControllerTest {

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private EuiccManager mEuiccManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private MobileNetworkRepository mMobileNetworkRepository;
    @Mock
    private MobileNetworkRepository.MobileNetworkCallback mMobileNetworkCallback;

    private AddPreference mPreference;
    private Context mContext;
    private MobileNetworkSummaryController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(TelephonyManager.class);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mEuiccManager).when(mContext).getSystemService(EuiccManager.class);
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(mContext);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mMobileNetworkRepository.addRegister(mLifecycleOwner, mMobileNetworkCallback,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        when(mTelephonyManager.getNetworkCountryIso()).thenReturn("");
        when(mSubscriptionManager.isActiveSubscriptionId(anyInt())).thenReturn(true);
        when(mEuiccManager.isEnabled()).thenReturn(true);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.EUICC_PROVISIONED, 1);

        mController = new MobileNetworkSummaryController(mContext, mLifecycle, mLifecycleOwner);
        mPreference = spy(new AddPreference(mContext, null));
        mPreference.setKey(mController.getPreferenceKey());
        when(mPreferenceScreen.findPreference(eq(mController.getPreferenceKey()))).thenReturn(
                mPreference);
    }

    @After
    public void tearDown() {
        mMobileNetworkRepository.removeRegister(mMobileNetworkCallback);
        SubscriptionUtil.setActiveSubscriptionsForTesting(null);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(null);
    }

    @Test
    public void isAvailable_wifiOnlyMode_notAvailable() {
        when(mTelephonyManager.isDataCapable()).thenReturn(false);
        when(mUserManager.isAdminUser()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_secondaryUser_notAvailable() {
        when(mTelephonyManager.isDataCapable()).thenReturn(true);
        when(mUserManager.isAdminUser()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getSummary_noSubscriptions_returnSummaryCorrectly() {
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();

        assertThat(mController.getSummary()).isEqualTo("Add a network");
    }

    @Test
    public void getSummary_noSubscriptionsNoEuiccMgr_correctSummaryAndClickHandler() {
        when(mEuiccManager.isEnabled()).thenReturn(false);
        assertThat(TextUtils.isEmpty(mController.getSummary())).isTrue();
        assertThat(mPreference.getOnPreferenceClickListener()).isNull();
        assertThat(mPreference.getFragment()).isNull();
    }

    @Test
    @Ignore
    public void getSummary_oneSubscription_correctSummaryAndClickHandler() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        when(sub1.getSubscriptionId()).thenReturn(1);
        when(sub1.getDisplayName()).thenReturn("sub1");
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        SubscriptionUtil.setActiveSubscriptionsForTesting(Arrays.asList(sub1));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        assertThat(mController.getSummary()).isEqualTo("sub1");
        assertThat(mPreference.getFragment()).isNull();
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(intentCaptor.capture());
        mPreference.getOnPreferenceClickListener().onPreferenceClick(mPreference);
        Intent intent = intentCaptor.getValue();
        assertThat(intent.getComponent().getClassName()).isEqualTo(
                MobileNetworkActivity.class.getName());
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID)).isEqualTo(sub1.getSubscriptionId());
    }

    @Test
    @Ignore
    public void getSummary_oneInactivePSim_cannotDisablePsim_correctSummaryAndClickHandler() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        when(sub1.getSubscriptionId()).thenReturn(1);
        when(sub1.getDisplayName()).thenReturn("sub1");
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        when(mSubscriptionManager.isActiveSubscriptionId(eq(1))).thenReturn(false);

        mController.displayPreference(mPreferenceScreen);
        mController.onResume();

        assertThat(mController.getSummary()).isEqualTo("Tap to activate sub1");

        assertThat(mPreference.getFragment()).isNull();
        mPreference.getOnPreferenceClickListener().onPreferenceClick(mPreference);
        verify(mSubscriptionManager).setSubscriptionEnabled(eq(sub1.getSubscriptionId()), eq(true));
    }

    @Test
    @Ignore
    public void getSummary_oneInactivePSim_canDisablePsim_correctSummaryAndClickHandler() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        when(sub1.getSubscriptionId()).thenReturn(1);
        when(sub1.getDisplayName()).thenReturn("sub1");
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        SubscriptionUtil.setActiveSubscriptionsForTesting(Arrays.asList(sub1));
        when(mSubscriptionManager.isActiveSubscriptionId(eq(1))).thenReturn(false);
        when(mSubscriptionManager.canDisablePhysicalSubscription()).thenReturn(true);

        mController.displayPreference(mPreferenceScreen);
        mController.onResume();

        assertThat(mController.getSummary()).isEqualTo("sub1");

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(intentCaptor.capture());
        mPreference.getOnPreferenceClickListener().onPreferenceClick(mPreference);
        Intent intent = intentCaptor.getValue();
        assertThat(intent.getComponent().getClassName()).isEqualTo(
                MobileNetworkActivity.class.getName());
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID)).isEqualTo(sub1.getSubscriptionId());
    }

    @Test
    public void addButton_noSubscriptionsNoEuiccMgr_noAddClickListener() {
        when(mEuiccManager.isEnabled()).thenReturn(false);
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        verify(mPreference, never()).setOnAddClickListener(notNull());
    }

    @Test
    public void addButton_oneSubscriptionNoEuiccMgr_noAddClickListener() {
        when(mEuiccManager.isEnabled()).thenReturn(false);
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        verify(mPreference, never()).setOnAddClickListener(notNull());
    }

    @Test
    public void addButton_noSubscriptions_noAddClickListener() {
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        verify(mPreference, never()).setOnAddClickListener(notNull());
    }

    @Test
    @Ignore
    public void addButton_oneSubscription_hasAddClickListener() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        verify(mPreference).setOnAddClickListener(notNull());
    }

    @Test
    @Ignore
    public void addButton_twoSubscriptions_hasAddClickListener() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo sub2 = mock(SubscriptionInfo.class);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        verify(mPreference).setOnAddClickListener(notNull());
    }

    @Test
    @Ignore
    public void addButton_oneSubscriptionAirplaneModeTurnedOn_addButtonGetsDisabled() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        mController.onAirplaneModeChanged(true);

        final ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        verify(mPreference, atLeastOnce()).setAddWidgetEnabled(captor.capture());
        assertThat(captor.getValue()).isFalse();
    }

    @Test
    @Ignore
    public void onResume_oneSubscriptionAirplaneMode_isDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();

        assertThat(mPreference.isEnabled()).isFalse();

        final ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        verify(mPreference, atLeastOnce()).setAddWidgetEnabled(captor.capture());
        assertThat(captor.getValue()).isFalse();
    }

    @Test
    public void onAvailableSubInfoChanged_noSubscriptionEsimDisabled_isDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        when(mEuiccManager.isEnabled()).thenReturn(false);
        mController.displayPreference(mPreferenceScreen);

        mController.onAvailableSubInfoChanged(null);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onAirplaneModeChanged_oneSubscriptionAirplaneModeGetsTurnedOn_isDisabled() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();

        assertThat(mPreference.isEnabled()).isTrue();

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        mController.onAirplaneModeChanged(true);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    @Ignore
    public void onAirplaneModeChanged_oneSubscriptionAirplaneModeGetsTurnedOff_isEnabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();

        assertThat(mPreference.isEnabled()).isFalse();

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        mController.onAirplaneModeChanged(false);

        assertThat(mPreference.isEnabled()).isTrue();

        final ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        verify(mPreference, atLeastOnce()).setAddWidgetEnabled(eq(false));
        verify(mPreference, atLeastOnce()).setAddWidgetEnabled(captor.capture());
        assertThat(captor.getValue()).isTrue();
    }

    @Test
    public void onResume_disabledByAdmin_prefStaysDisabled() {
        mPreference.setDisabledByAdmin(new RestrictedLockUtils.EnforcedAdmin());
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        verify(mPreference, never()).setEnabled(eq(true));
    }
}
