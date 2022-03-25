/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.CarrierConfigCache;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class NetworkProviderBackupCallingGroupTest {

    private static final int SUB_ID_1 = 1;
    private static final int SUB_ID_2 = 2;

    private static final String KEY_PREFERENCE_CATEGORY_BACKUP_CALLING =
            "provider_model_backup_calling_category";
    private static final String DISPLAY_NAME_1 = "Test Display Name 1";
    private static final String DISPLAY_NAME_2 = "Test Display Name 2";

    @Mock
    private PreferenceGroup mPreferenceGroup;
    @Mock
    private CarrierConfigCache mCarrierConfigCache;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo1;
    @Mock
    private SubscriptionInfo mSubscriptionInfo2;
    @Mock
    private List<SubscriptionInfo> mSubscriptionInfoList;
    @Mock
    private TelephonyManager mTelephonyManager1;
    @Mock
    private TelephonyManager mTelephonyManager2;

    @Mock
    private NetworkProviderBackupCallingGroup mNetworkProviderBackupCallingGroup;
    private Context mContext;
    private PersistableBundle mCarrierConfig;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private SwitchPreference mSwitchPreference1;
    private SwitchPreference mSwitchPreference2;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager1);
        when(mTelephonyManager1.createForSubscriptionId(SUB_ID_1)).thenReturn(mTelephonyManager1);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager2);
        when(mTelephonyManager2.createForSubscriptionId(SUB_ID_2)).thenReturn(mTelephonyManager2);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mSubscriptionInfo1.getSubscriptionId()).thenReturn(SUB_ID_1);
        when(mSubscriptionInfo1.getDisplayName()).thenReturn(DISPLAY_NAME_1);
        doReturn(true).when(mNetworkProviderBackupCallingGroup).isCrossSimEnabledByPlatform(
                mContext, SUB_ID_1);
        mSubscriptionInfoList.add(mSubscriptionInfo1);
        when(mSubscriptionInfo2.getSubscriptionId()).thenReturn(SUB_ID_2);
        when(mSubscriptionInfo2.getDisplayName()).thenReturn(DISPLAY_NAME_2);
        doReturn(true).when(mNetworkProviderBackupCallingGroup).isCrossSimEnabledByPlatform(
                mContext, SUB_ID_2);
        mSubscriptionInfoList.add(mSubscriptionInfo2);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                mSubscriptionInfoList);

        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);
        mCarrierConfig = new PersistableBundle();
        doReturn(mCarrierConfig).when(mCarrierConfigCache).getConfigForSubId(SUB_ID_1);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                true);
        doReturn(mCarrierConfig).when(mCarrierConfigCache).getConfigForSubId(SUB_ID_2);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                true);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        when(mPreferenceGroup.getKey()).thenReturn(KEY_PREFERENCE_CATEGORY_BACKUP_CALLING);
        when(mPreferenceGroup.getPreferenceCount()).thenReturn(2);
        mPreferenceScreen.addPreference(mPreferenceGroup);

        mNetworkProviderBackupCallingGroup = spy(new NetworkProviderBackupCallingGroup(
                mContext, mLifecycle, mSubscriptionInfoList,
                KEY_PREFERENCE_CATEGORY_BACKUP_CALLING));
    }

    @Test
    public void shouldShowBackupCallingForSub_invalidSubId_returnFalse() {
        assertThat(mNetworkProviderBackupCallingGroup.hasBackupCallingFeature(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID)).isEqualTo(false);
    }

    @Test
    public void shouldShowBackupCallingForSub_carrierConfigIsUnavailable_returnFalse() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                false);

        assertThat(mNetworkProviderBackupCallingGroup.hasBackupCallingFeature(SUB_ID_1))
                .isEqualTo(false);
    }

    @Test
    public void
    shouldShowBackupCallingForSub_crossSimDisabled_returnFalse() {
        doReturn(false).when(mNetworkProviderBackupCallingGroup).isCrossSimEnabledByPlatform(
                mContext, SUB_ID_1);

        assertThat(mNetworkProviderBackupCallingGroup.hasBackupCallingFeature(SUB_ID_1))
                .isEqualTo(false);
    }

    @Test
    public void shouldBackupCallingForSub_crossSimEnabled_returnTrue() {
        doReturn(true).when(mNetworkProviderBackupCallingGroup).isCrossSimEnabledByPlatform(
                mContext, SUB_ID_1);

        assertThat(mNetworkProviderBackupCallingGroup.hasBackupCallingFeature(SUB_ID_1))
                .isEqualTo(true);
    }
}
