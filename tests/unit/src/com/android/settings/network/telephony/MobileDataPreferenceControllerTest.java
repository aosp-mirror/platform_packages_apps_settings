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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class MobileDataPreferenceControllerTest {
    private static final String SUB_ID_1 = "1";
    private static final String SUB_ID_2 = "2";
    private static final String DISPLAY_NAME_1 = "Sub 1";
    private static final String DISPLAY_NAME_2 = "Sub 2";
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
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private LifecycleOwner mLifecycleOwner;
    private SubscriptionInfoEntity mSubInfo1;
    private SubscriptionInfoEntity mSubInfo2;
    private MobileNetworkInfoEntity mNetworkInfo1;
    private MobileNetworkInfoEntity mNetworkInfo2;
    private LifecycleRegistry mLifecycleRegistry;
    private MobileDataPreferenceController mController;
    private SwitchPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);

        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mSubscriptionManager.createForAllUserProfiles()).thenReturn(mSubscriptionManager);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mFragmentTransaction).when(mFragmentManager).beginTransaction();

        mPreference = new SwitchPreference(mContext);
        mController = new MobileDataPreferenceController(mContext, "mobile_data", mLifecycle,
                mLifecycleOwner, SUB_ID);
        mController.init(mFragmentManager, SUB_ID, mSubInfo1, mNetworkInfo1);
        mPreference.setKey(mController.getPreferenceKey());
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
    }

    private SubscriptionInfoEntity setupSubscriptionInfoEntity(String subId, String displayName,
            boolean isOpportunistic, boolean isValid, boolean isActive, boolean isAvailable) {
        int id = Integer.parseInt(subId);
        return new SubscriptionInfoEntity(subId, id, id,
                displayName, displayName, 0, "mcc", "mnc", "countryIso", false, id,
                TelephonyManager.DEFAULT_PORT_INDEX, isOpportunistic, null,
                SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM, displayName, false,
                "1234567890", true, false, isValid, true, isActive, isAvailable, false);
    }

    private MobileNetworkInfoEntity setupMobileNetworkInfoEntity(String subId,
            boolean isDatEnabled) {
        return new MobileNetworkInfoEntity(subId, false, false, isDatEnabled, false, false, false,
                false, false, false, false, false);
    }

    @Test
    public void getAvailabilityStatus_invalidSubscription_returnAvailableUnsearchable() {
        mController.init(mFragmentManager, SubscriptionManager.INVALID_SUBSCRIPTION_ID, mSubInfo1,
                mNetworkInfo1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void isDialogNeeded_disableSingleSim_returnFalse() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, false, true, true, true);
        mNetworkInfo1 = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), true);
        doReturn(1).when(mTelephonyManager).getActiveModemCount();

        assertThat(mController.isDialogNeeded()).isFalse();
    }

    @Test
    public void isDialogNeeded_enableNonDefaultSimInMultiSimMode_returnTrue() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, false, true, true, true);
        mNetworkInfo1 = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), false);
        doReturn(1).when(mTelephonyManager).getActiveModemCount();
        // Ideally, it would be better if we could set the default data subscription to
        // SUB_ID_OTHER, and set that as an active subscription id.
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, DISPLAY_NAME_2, false, true, true, true);
        mNetworkInfo1 = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), true);
        doReturn(2).when(mTelephonyManager).getActiveModemCount();

        assertThat(mController.isDialogNeeded()).isTrue();
        assertThat(mController.mDialogType).isEqualTo(
                MobileDataDialogFragment.TYPE_MULTI_SIM_DIALOG);
    }

    @Test
    public void handlePreferenceTreeClick_needDialog_showDialog() {
        mController.mNeedDialog = true;
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        instrumentation.runOnMainSync(() -> {
            mController.handlePreferenceTreeClick(mPreference);
        });
        verify(mFragmentManager).beginTransaction();
    }

    @Test
    public void onPreferenceChange_singleSim_On_shouldEnableData() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, true, true, true, true);
        mNetworkInfo1 = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), true);
        mController.setSubscriptionInfoEntity(mSubInfo1);
        mController.setMobileNetworkInfoEntity(mNetworkInfo1);
        doReturn(1).when(mTelephonyManager).getActiveModemCount();

        mController.onPreferenceChange(mPreference, true);

        verify(mTelephonyManager).setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER
                ,true);
    }

    @Test
    public void onPreferenceChange_multiSim_On_shouldEnableData() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, true, true, true, true);
        mNetworkInfo1 = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), true);
        mController.setSubscriptionInfoEntity(mSubInfo1);
        mController.setMobileNetworkInfoEntity(mNetworkInfo1);
        doReturn(2).when(mTelephonyManager).getActiveModemCount();

        mController.onPreferenceChange(mPreference, true);

        verify(mTelephonyManager).setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER
                ,true);
    }

    @Test
    public void isChecked_returnUserDataEnabled() {
        mNetworkInfo1 = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), false);
        mController.init(mFragmentManager, SUB_ID, mSubInfo1, mNetworkInfo1);
        assertThat(mController.isChecked()).isFalse();

        mNetworkInfo1 = setupMobileNetworkInfoEntity(String.valueOf(SUB_ID), true);
        mController.setMobileNetworkInfoEntity(mNetworkInfo1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void updateState_opportunistic_disabled() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, true, true, true, true);
        mController.init(mFragmentManager, SUB_ID, mSubInfo1, mNetworkInfo1);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext,
                        "mobile_data_settings_summary_auto_switch"));
    }

    @Test
    public void updateState_notOpportunistic_enabled() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, false, true, true, true);
        mController.init(mFragmentManager, SUB_ID, mSubInfo1, mNetworkInfo1);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "mobile_data_settings_summary"));
    }
}
