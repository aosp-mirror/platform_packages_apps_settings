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


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.ims.MockWfcQueryImsState;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class NetworkProviderWifiCallingGroupTest {

    private static final int SUB_ID = 1;
    private static final String KEY_PREFERENCE_WFC_CATEGORY = "provider_model_calling_category";
    private static final String PACKAGE_NAME = "com.android.settings";

    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ResolveInfo mResolveInfo;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private TelecomManager mTelecomManager;
    @Mock
    private TelephonyManager mTelephonyManager;

    private ComponentName mComponentName;
    private Context mContext;
    private MockWfcQueryImsState mMockQueryWfcState;
    private NetworkProviderWifiCallingGroup mNetworkProviderWifiCallingGroup;
    private PersistableBundle mCarrierConfig;
    private PhoneAccountHandle mPhoneAccountHandle;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(CarrierConfigManager.class)).thenReturn(
                mCarrierConfigManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mSubscriptionManager.createForAllUserProfiles()).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(TelecomManager.class)).thenReturn(mTelecomManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID)).thenReturn(mTelephonyManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(SUB_ID);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));

        mCarrierConfig = new PersistableBundle();
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true);
        when(mTelecomManager.getSimCallManagerForSubscription(SUB_ID))
                .thenReturn(mPhoneAccountHandle);
        mMockQueryWfcState = new MockWfcQueryImsState(mContext, SUB_ID);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        when(mPreferenceCategory.getKey()).thenReturn(KEY_PREFERENCE_WFC_CATEGORY);
        when(mPreferenceCategory.getPreferenceCount()).thenReturn(1);
        mPreferenceScreen.addPreference(mPreferenceCategory);

        mNetworkProviderWifiCallingGroup = spy(new NetworkProviderWifiCallingGroup(
                mContext, mLifecycle, KEY_PREFERENCE_WFC_CATEGORY));
    }

    @Test
    public void shouldShowWifiCallingForSub_invalidSubId_returnFalse() {
        assertThat(mNetworkProviderWifiCallingGroup.shouldShowWifiCallingForSub(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID)).isEqualTo(false);
    }

    @Test
    public void shouldShowWifiCallingForSub_carrierConfigIsUnavailable_returnFalse() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, false);

        assertThat(mNetworkProviderWifiCallingGroup.shouldShowWifiCallingForSub(SUB_ID))
                .isEqualTo(false);
    }

    @Test
    public void
    shouldShowWifiCallingForSub_wifiCallingDisabledWithWifiCallingNotReady_returnFalse() {
        setWifiCallingEnabled(false);
        doReturn(mMockQueryWfcState).when(mNetworkProviderWifiCallingGroup).queryImsState(SUB_ID);

        assertThat(mNetworkProviderWifiCallingGroup.shouldShowWifiCallingForSub(SUB_ID))
                .isEqualTo(false);
    }

    @Test
    public void shouldShowWifiCallingForSub_wifiCallingEnabledWithWifiCallingIsReady_returnTrue() {
        setWifiCallingEnabled(true);
        doReturn(mMockQueryWfcState).when(mNetworkProviderWifiCallingGroup).queryImsState(SUB_ID);

        assertThat(mNetworkProviderWifiCallingGroup.shouldShowWifiCallingForSub(SUB_ID))
                .isEqualTo(true);
    }

    @Test
    public void
    shouldShowWifiCallingForSub_wifiCallingDisabledWithNoActivityHandleIntent_returnFalse() {
        buildPhoneAccountConfigureIntent(false);
        doReturn(mMockQueryWfcState).when(mNetworkProviderWifiCallingGroup).queryImsState(SUB_ID);
        doReturn(mPhoneAccountHandle).when(mNetworkProviderWifiCallingGroup)
                .getPhoneAccountHandleForSubscriptionId(SUB_ID);

        assertThat(mNetworkProviderWifiCallingGroup.shouldShowWifiCallingForSub(SUB_ID))
                .isEqualTo(false);
    }

    @Test
    public void
    shouldShowWifiCallingForSub_wifiCallingEnabledWithActivityHandleIntent_returnTrue() {
        buildPhoneAccountConfigureIntent(true);
        doReturn(mMockQueryWfcState).when(mNetworkProviderWifiCallingGroup).queryImsState(SUB_ID);
        doReturn(mPhoneAccountHandle).when(mNetworkProviderWifiCallingGroup)
                .getPhoneAccountHandleForSubscriptionId(SUB_ID);

        assertThat(mNetworkProviderWifiCallingGroup.shouldShowWifiCallingForSub(SUB_ID))
                .isEqualTo(true);
    }

    private void setWifiCallingEnabled(boolean enabled) {
        mMockQueryWfcState.setIsEnabledByUser(enabled);
        mMockQueryWfcState.setServiceStateReady(enabled);
        mMockQueryWfcState.setIsEnabledByPlatform(enabled);
        mMockQueryWfcState.setIsProvisionedOnDevice(enabled);
    }

    private void buildPhoneAccountConfigureIntent(boolean hasActivityHandleIntent) {
        mComponentName = new ComponentName(PACKAGE_NAME, "testClass");
        mPhoneAccountHandle = new PhoneAccountHandle(mComponentName, "");
        when(mPackageManager.queryIntentActivities(nullable(Intent.class), anyInt()))
                .thenReturn(
                        hasActivityHandleIntent ? Arrays.asList(mResolveInfo) : new ArrayList<>());
    }
}
